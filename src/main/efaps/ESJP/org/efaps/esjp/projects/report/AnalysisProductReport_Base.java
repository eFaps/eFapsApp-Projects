/*
 * Copyright 2003 - 2014 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.projects.report;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("5f849268-6522-4782-9932-2d6e57512e98")
@EFapsRevision("$Rev$")
public abstract class AnalysisProductReport_Base
    extends FilteredReport
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return return containing snipplet
     * @throws EFapsException on error
     */
    public Return getReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getDynReport(_parameter);
        dyRp.setFileName(DBProperties.getProperty(AnalysisProductReport.class.getName() + ".FileName"));
        final String html = dyRp.getHtml(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return return containing file
     * @throws EFapsException on error
     */
    public Return exportReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getDynReport(_parameter);
        dyRp.setFileName(DBProperties.getProperty(AnalysisProductReport.class.getName() + ".FileName"));
        File file = null;
        if ("xls".equalsIgnoreCase(getProperty(_parameter, "Mime"))) {
            file = dyRp.getExcel(_parameter);
        } else if ("pdf".equalsIgnoreCase(getProperty(_parameter, "Mime"))) {
            file = dyRp.getPDF(_parameter);
        }
        ret.put(ReturnValues.VALUES, file);
        ret.put(ReturnValues.TRUE, true);

        return ret;
    }

    protected DynAnalysisProductReport getDynReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynAnalysisProductReport(this);
    }

    public static class DynAnalysisProductReport
        extends AbstractDynamicReport
    {

        public DynAnalysisProductReport(final AnalysisProductReport_Base _salesProductReport_Base)
        {
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final Map<Instance, DataBean> map = new HashMap<>();

            final QueryBuilder attrQueryBldr = getQueryBldrFromProperties(_parameter);
            add2QueryBuilder(_parameter, attrQueryBldr);
            addMap4AnalysisProductReport(attrQueryBldr, map, true);

            final QueryBuilder attrQueryBldr2 = getQueryBldrFromProperties(_parameter, 100);
            add2QueryBuilder(_parameter, attrQueryBldr2);
            addMap4AnalysisProductReport(attrQueryBldr2, map, false);

            final List<DataBean> datasource = new ArrayList<>();
            datasource.addAll(map.values());
            final ComparatorChain<DataBean> chain = new ComparatorChain<>();
            chain.addComparator(new Comparator<DataBean>()
            {

                @Override
                public int compare(final DataBean _arg0,
                                   final DataBean _arg1)
                {
                    return _arg0.getProductName().compareTo(_arg1.getProductName());
                }
            });
            Collections.sort(datasource, chain);
            return new JRBeanCollectionDataSource(datasource);
        }

        protected void addMap4AnalysisProductReport(final QueryBuilder _attrQueryBldr,
                                                    final Map<Instance, DataBean> _map,
                                                    final boolean _inverse)
            throws EFapsException
        {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
            queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.DocumentAbstractLink,
                            _attrQueryBldr.getAttributeQuery(CIERP.DocumentAbstract.ID));
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selProd = SelectBuilder.get().linkto(CISales.PositionAbstract.Product);
            final SelectBuilder selProdInst = new SelectBuilder(selProd).instance();
            final SelectBuilder selProdName = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Name);
            final SelectBuilder selProdDesc = new SelectBuilder(selProd)
                            .attribute(CIProducts.ProductAbstract.Description);
            multi.addSelect(selProdInst, selProdName, selProdDesc);
            multi.addAttribute(CISales.PositionAbstract.UoM, CISales.PositionAbstract.Quantity);
            multi.execute();
            while (multi.next()) {
                final Instance prodInst = multi.getSelect(selProdInst);
                DataBean bean;
                final BigDecimal quantity = multi.<BigDecimal>getAttribute(CISales.PositionAbstract.Quantity);

                if (_map.containsKey(prodInst)) {
                    bean = _map.get(prodInst);
                } else {
                    bean = new DataBean();
                    _map.put(prodInst, bean);
                    bean.setProductName(multi.<String>getSelect(selProdName));
                    bean.setProductDesc(multi.<String>getSelect(selProdDesc));
                }
                if (_inverse) {
                    bean.addQuantity(multi.<Long>getAttribute(CISales.PositionAbstract.UoM), quantity);
                } else {
                    bean.addQuantityOut(multi.<Long>getAttribute(CISales.PositionAbstract.UoM), quantity);
                }
            }
        }

        /**
         * @param _parameter Parameter as passed from the eFaps API
         * @param _queryBldr QueryBuilder the criteria will be added to
         * @throws EFapsException on error
         */
        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Instance inst = _parameter.getInstance();
            if (inst != null && inst.isValid() && inst.getType().isKindOf(CIProjects.ProjectAbstract)) {
                final QueryBuilder projAttrQueryBldr = new QueryBuilder(CIProjects.Project2DocumentAbstract);
                projAttrQueryBldr.addWhereAttrEqValue(CIProjects.Project2DocumentAbstract.FromAbstract, inst);
                _queryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.ID,
                                projAttrQueryBldr.getAttributeQuery(CIProjects.Project2DocumentAbstract.ToAbstract));
            }
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<String> prodNameColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AnalysisProductReport.class.getName() + ".Column.productName"),
                            "productName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> prodDescColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AnalysisProductReport.class.getName() + ".Column.productDesc"),
                            "productDesc", DynamicReports.type.stringType()).setWidth(450);
            final TextColumnBuilder<String> uoMColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AnalysisProductReport.class.getName() + ".Column.uoMName"),
                            "uoMName", DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AnalysisProductReport.class.getName() + ".Column.quantity"),
                            "quantity", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> quantityOutColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AnalysisProductReport.class.getName() + ".Column.quantityOut"),
                            "quantityOut", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> differenceColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AnalysisProductReport.class.getName() + ".Column.difference"),
                            "difference", DynamicReports.type.bigDecimalType());

            _builder.addColumn(prodNameColumn, prodDescColumn, uoMColumn, quantityColumn, quantityOutColumn, differenceColumn);
        }
    }

    public static class DataBean
    {

        private Instance prodInst;

        private String productName;

        private String productDesc;

        private UoM uom;

        private BigDecimal quantity = BigDecimal.ZERO;

        private BigDecimal quantityOut = BigDecimal.ZERO;

        public String getUoMName()
        {
            return this.uom == null ? "" : this.uom.getName();
        }

        /**
         * @param _attribute
         * @param _attribute2
         */
        public void addQuantity(final Long _uomId,
                                final BigDecimal _quantity)
        {
            if (this.uom == null) {
                this.uom = Dimension.getUoM(_uomId);
                this.quantity = this.quantity.add(_quantity);
            } else if (this.uom.getId() == _uomId) {
                this.quantity = this.quantity.add(_quantity);
            } else {
                this.quantity = convertToBase(this.uom, this.quantity);
                final UoM uomTmp = Dimension.getUoM(_uomId);
                this.quantity = this.quantity.add(convertToBase(uomTmp, _quantity));
            }
        }

        /**
         * @param _attribute
         * @param _attribute2
         */
        public void addQuantityOut(final Long _uomId,
                                   final BigDecimal _quantityOut)
        {
            if (this.uom == null) {
                this.uom = Dimension.getUoM(_uomId);
                this.quantityOut = this.quantityOut.add(_quantityOut);
            } else if (this.uom.getId() == _uomId) {
                this.quantityOut = this.quantityOut.add(_quantityOut);
            } else {
                this.quantityOut = convertToBase(this.uom, this.quantityOut);
                final UoM uomTmp = Dimension.getUoM(_uomId);
                this.quantityOut = this.quantityOut.add(convertToBase(uomTmp, _quantityOut));
            }
        }

        /**
         * @param _attribute
         * @param _attribute2
         */
        public void addQuantity(final BigDecimal _amount)
        {
            this.quantity = this.quantity.add(_amount);
        }

        protected BigDecimal convertToBase(final UoM _uoM,
                                           final BigDecimal _quantity)
        {
            BigDecimal ret = _quantity;
            if (!_uoM.equals(_uoM.getDimension().getBaseUoM())) {
                ret = new BigDecimal(_uoM.getNumerator()).setScale(12, BigDecimal.ROUND_HALF_UP)
                                .divide(new BigDecimal(_uoM.getDenominator()), BigDecimal.ROUND_HALF_UP)
                                .multiply(_quantity);
                if (!this.uom.equals(this.uom.getDimension().getBaseUoM())) {
                    this.uom = this.uom.getDimension().getBaseUoM();
                }
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #oid}.
         *
         * @return value of instance variable {@link #oid}
         */
        public String getOid()
        {
            return this.prodInst.getOid();
        }

        /**
         * Getter method for the instance variable {@link #productName}.
         *
         * @return value of instance variable {@link #productName}
         */
        public String getProductName()
        {
            return this.productName;
        }

        /**
         * Setter method for instance variable {@link #productName}.
         *
         * @param _productName value for instance variable {@link #productName}
         */
        public void setProductName(final String _productName)
        {
            this.productName = _productName;
        }

        /**
         * Getter method for the instance variable {@link #productDesc}.
         *
         * @return value of instance variable {@link #productDesc}
         */
        public String getProductDesc()
        {
            return this.productDesc;
        }

        /**
         * Setter method for instance variable {@link #productDesc}.
         *
         * @param _productDesc value for instance variable {@link #productDesc}
         */
        public void setProductDesc(final String _productDesc)
        {
            this.productDesc = _productDesc;
        }

        /**
         * Getter method for the instance variable {@link #uom}.
         *
         * @return value of instance variable {@link #uom}
         */
        public UoM getUom()
        {
            return this.uom;
        }

        /**
         * Setter method for instance variable {@link #uom}.
         *
         * @param _uom value for instance variable {@link #uom}
         */
        public void setUom(final UoM _uom)
        {
            this.uom = _uom;
        }

        /**
         * Getter method for the instance variable {@link #prodInst}.
         *
         * @return value of instance variable {@link #prodInst}
         */
        public Instance getProdInst()
        {
            return this.prodInst;
        }

        /**
         * Setter method for instance variable {@link #prodInst}.
         *
         * @param _prodInst value for instance variable {@link #prodInst}
         */
        public void setProdInst(final Instance _prodInst)
        {
            this.prodInst = _prodInst;
        }

        /**
         * Setter method for instance variable {@link #estQuantity}.
         *
         * @param _estQuantity value for instance variable {@link #estQuantity}
         */
        public void setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
        }

        /**
         * Getter method for the instance variable {@link #aplQuantity}.
         *
         * @return value of instance variable {@link #aplQuantity}
         */
        public BigDecimal getQuantity()
        {
            return this.quantity;
        }

        /**
         * Setter method for instance variable {@link #quantityOut}.
         *
         * @param _estQuantity value for instance variable {@link #quantityOut}
         */
        public void setQuantityOut(final BigDecimal _quantityOut)
        {
            this.quantityOut = _quantityOut;
        }

        /**
         * Getter method for the instance variable {@link #quantityOut}.
         *
         * @return value of instance variable {@link #quantityOut}
         */
        public BigDecimal getQuantityOut()
        {
            return this.quantityOut;
        }

        public BigDecimal getDifference()
        {
            return this.quantity.subtract(this.quantityOut);
        }
    }
}
