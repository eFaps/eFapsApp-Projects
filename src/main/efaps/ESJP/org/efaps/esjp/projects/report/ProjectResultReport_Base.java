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

import java.awt.Color;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnGridComponentBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.dynamicreports.report.builder.style.ConditionalStyleBuilder;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import org.efaps.admin.common.MsgPhrase;
import org.efaps.admin.datamodel.Type;
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
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.projects.util.Projects;
import org.efaps.esjp.projects.util.ProjectsSettings;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: AnalysisProductReport_Base.java 14136 2014-09-29 21:42:24Z
 *          m.aranya@moxter.net $
 */
@EFapsUUID("9b3c65df-97aa-4fe5-84f3-2931e4f66cc1")
@EFapsRevision("$Rev$")
public abstract class ProjectResultReport_Base
    extends FilteredReport
{

    public enum Style
    {
        NONE, HEADER, TOTAL;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return return containing snipplet
     * @throws EFapsException on error
     */
    public Return generateReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getDynReport(_parameter);
        dyRp.setFileName(DBProperties.getProperty(ProjectResultReport.class.getName() + ".FileName"));
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
        dyRp.setFileName(DBProperties.getProperty(ProjectResultReport.class.getName() + ".FileName"));
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

    protected DynProjectResultReport getDynReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynProjectResultReport(this);
    }

    public static class DynProjectResultReport
        extends AbstractDynamicReport
    {

        private List<ProjectBean> beans;
        private final FilteredReport filteredReport;

        public DynProjectResultReport(final FilteredReport _filteredReport)
        {
            this.filteredReport = _filteredReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final List<Map<String, Object>> source = new ArrayList<>();

            final List<Map<String, Object>> expenseSource = new ArrayList<>();
            final Map<String, Object> expensetotalMap = addBlock(_parameter, expenseSource, "Expense");

            final List<Map<String, Object>> collectionSource = new ArrayList<>();
            final Map<String, Object> collectionTotalMap = addBlock(_parameter, collectionSource, "Collection");

            final List<Map<String, Object>> estimateSource = new ArrayList<>();
            final Map<String, Object> estimateTotalMap = addBlock(_parameter, estimateSource, "Estimate");

            addHeader(_parameter, source, "estimate");
            source.addAll(estimateSource);
            source.add(estimateTotalMap);
            source.add(Collections.<String, Object>emptyMap());
            addHeader(_parameter, source, "expense");
            source.addAll(expenseSource);
            source.add(expensetotalMap);
            source.add(Collections.<String, Object>emptyMap());
            addHeader(_parameter, source, "collection");
            source.addAll(collectionSource);
            source.add(collectionTotalMap);
            source.add(Collections.<String, Object>emptyMap());

            addHeader(_parameter, source, "estimateGain");
            addResult(_parameter, source, estimateTotalMap, expensetotalMap);
            source.add(Collections.<String, Object>emptyMap());
            addHeader(_parameter, source, "collectionGain");
            addResult(_parameter, source, collectionTotalMap, expensetotalMap);

            return new JRMapCollectionDataSource(new ArrayList<Map<String, ?>>(source));
        }

        protected void addHeader(final Parameter _parameter,
                                 final List<Map<String, Object>> _source,
                                 final String _key)
        {
            final Map<String, Object> map = new HashMap<>();
            map.put("descr", DBProperties.getProperty(ProjectResultReport.class.getName() + "." + _key + ".descr"));
            map.put("style", Style.HEADER);
            _source.add(map);
        }

        protected void addResult(final Parameter _parameter,
                                 final List<Map<String, Object>> _source,
                                 final Map<String, Object> _base,
                                 final Map<String, Object> _target)
            throws EFapsException
        {
            final Map<String, Object> estimateGainMap = new HashMap<>();
            estimateGainMap.put("descr", DBProperties.getProperty(ProjectResultReport.class.getName() + ".gain.descr"));
            final Map<String, Object> estimateGainPercentMap = new HashMap<>();
            estimateGainPercentMap.put("descr",
                            DBProperties.getProperty(ProjectResultReport.class.getName() + ".gainPercent.descr"));
            _source.add(estimateGainMap);
            _source.add(estimateGainPercentMap);
            for (final ProjectBean bean : getBeans(_parameter)) {
                final BigDecimal baseNetTotal = _base.containsKey(bean.getNetKey()) ? (BigDecimal) _base.get(bean
                                .getNetKey()) : BigDecimal.ZERO;
                final BigDecimal targetNetTotal = _target.containsKey(bean.getNetKey()) ? (BigDecimal) _target.get(bean
                                .getNetKey()) : BigDecimal.ZERO;
                final BigDecimal baseCrossTotal = _base.containsKey(bean.getCrossKey()) ? (BigDecimal) _base.get(bean
                                .getCrossKey()) : BigDecimal.ZERO;
                final BigDecimal targetCrossTotal = _target.containsKey(bean.getCrossKey()) ? (BigDecimal) _target
                                .get(bean.getCrossKey()) : BigDecimal.ZERO;

                estimateGainMap.put(bean.getNetKey(), baseNetTotal.subtract(targetNetTotal));
                estimateGainMap.put(bean.getCrossKey(), baseCrossTotal.subtract(targetCrossTotal));
                BigDecimal netPercent = BigDecimal.ZERO;
                BigDecimal crossPercent = BigDecimal.ZERO;
                if (baseNetTotal.compareTo(BigDecimal.ZERO) != 0) {
                    netPercent = BigDecimal.ONE.subtract(targetNetTotal.divide(baseNetTotal, BigDecimal.ROUND_HALF_UP))
                                    .multiply(new BigDecimal(100));
                }
                if (baseCrossTotal.compareTo(BigDecimal.ZERO) != 0) {
                    crossPercent = BigDecimal.ONE.subtract(targetCrossTotal
                                    .divide(baseCrossTotal, BigDecimal.ROUND_HALF_UP))
                                    .multiply(new BigDecimal(100));
                }
                estimateGainPercentMap.put(bean.getNetKey(), netPercent);
                estimateGainPercentMap.put(bean.getCrossKey(), crossPercent);
            }
        }

        protected Map<String, Object> addBlock(final Parameter _parameter,
                                               final List<Map<String, Object>> _source,
                                               final String _key)
            throws CacheReloadException, EFapsException
        {
            final Map<String, Object> totalMap = new HashMap<>();
            totalMap.put("style", Style.TOTAL);
            final Properties properties = Projects.getSysConfig().getAttributeValueAsProperties(
                            ProjectsSettings.RESULTREPORT, true);
            int i = 1;
            String keyTmp = _key + String.format("%02d", i);
            while (properties.containsKey(keyTmp)) {
                final String typeStr = properties.getProperty(keyTmp);
                final Map<String, Object> map = new HashMap<>();
                map.put("descr", Type.get(typeStr).getLabel());
                map.put("type", Type.get(typeStr));
                _source.add(map);
                i++;
                keyTmp = _key + String.format("%02d", i);
            }

            for (final Map<String, Object> map : _source) {
                if (map.containsKey("type")) {
                    final Type type = (Type) map.get("type");
                    for (final ProjectBean bean : getBeans(_parameter)) {
                        if (bean.getDocs().containsKey(type)) {
                            final DocBean doc = bean.getDocs().get(type);
                            final BigDecimal cross = doc.getCross();
                            final BigDecimal net = doc.getNet();
                            map.put(bean.getNetKey(), net);
                            map.put(bean.getCrossKey(), cross);
                            if (totalMap.containsKey(bean.getNetKey())) {
                                totalMap.put(bean.getNetKey(), ((BigDecimal) totalMap.get(bean.getNetKey())).add(net));
                                totalMap.put(bean.getCrossKey(),
                                                ((BigDecimal) totalMap.get(bean.getCrossKey())).add(cross));
                            } else {
                                totalMap.put(bean.getNetKey(), net);
                                totalMap.put(bean.getCrossKey(), cross);
                            }
                        }
                    }
                }
            }
            return totalMap;
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
            } else {
                final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
                final DateTime start;
                final DateTime end;
                if (filterMap.containsKey("dateFrom")) {
                    start = (DateTime) filterMap.get("dateFrom");
                } else {
                    start = new DateTime();
                }
                if (filterMap.containsKey("dateTo")) {
                    end = (DateTime) filterMap.get("dateTo");
                } else {
                    end = new DateTime();
                }
                final QueryBuilder projAttrQueryBldr = new QueryBuilder(CIProjects.ProjectAbstract);
                projAttrQueryBldr.addWhereAttrLessValue(CIProjects.ProjectAbstract.Date, end);
                projAttrQueryBldr.addWhereAttrGreaterValue(CIProjects.ProjectAbstract.Date, start.minusMinutes(1));

                final QueryBuilder attrQueryBldr = new QueryBuilder(CIProjects.Project2DocumentAbstract);
                attrQueryBldr.addWhereAttrInQuery(CIProjects.Project2DocumentAbstract.FromAbstract,
                                projAttrQueryBldr.getAttributeQuery(CIProjects.ProjectAbstract.ID));
                _queryBldr.addWhereAttrInQuery(CISales.DocumentAbstract.ID,
                                attrQueryBldr.getAttributeQuery(CIProjects.Project2DocumentAbstract.ToAbstract));
            }
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final ConditionalStyleBuilder headerCondition = DynamicReports.stl.conditionalStyle(
                            new HeaderConditionExpression()).setBold(true).setBackgroundColor(Color.darkGray)
                            .setForegroundColor(Color.white);

            final ConditionalStyleBuilder totalCondition = DynamicReports.stl.conditionalStyle(
                            new TotalConditionExpression()).setBold(true).setItalic(true);

            final StyleBuilder headerStyle = DynamicReports.stl.style()
                            .conditionalStyles(headerCondition, totalCondition);
            _builder.addField("style", Style.class);

            final List<ColumnGridComponentBuilder> groupBuilders = new ArrayList<>();
            final TextColumnBuilder<String> descrColumn = DynamicReports.col.column(DBProperties
                            .getProperty(ProjectResultReport.class.getName() + ".Column.descr"),
                            "descr", DynamicReports.type.stringType()).setStyle(headerStyle);
            _builder.addColumn(descrColumn);
            groupBuilders.add(descrColumn);
            descrColumn.setWidth(200);

            for (final ProjectBean bean : getBeans(_parameter)) {
                final TextColumnBuilder<BigDecimal> netColumn = DynamicReports.col.column(DBProperties
                                .getProperty(ProjectResultReport.class.getName() + ".Column.net"),
                                bean.getNetKey(), DynamicReports.type.bigDecimalType()).setStyle(headerStyle);
                final TextColumnBuilder<BigDecimal> taxColumn = DynamicReports.col.column(DBProperties
                                .getProperty(ProjectResultReport.class.getName() + ".Column.tax"),
                                bean.getTaxKey(), DynamicReports.type.bigDecimalType()).setStyle(headerStyle);
                final TextColumnBuilder<BigDecimal> crossColumn = DynamicReports.col.column(DBProperties
                                .getProperty(ProjectResultReport.class.getName() + ".Column.cross"),
                                bean.getCrossKey(), DynamicReports.type.bigDecimalType()).setStyle(headerStyle);
                final ColumnTitleGroupBuilder projectGroup = DynamicReports.grid.titleGroup(bean.getName(), netColumn,
                                taxColumn, crossColumn);
                projectGroup.setTitleFixedWidth(250);
                groupBuilders.add(projectGroup);
                _builder.addColumn(netColumn, taxColumn, crossColumn);
            }
            _builder.columnGrid(groupBuilders.toArray(new ColumnGridComponentBuilder[groupBuilders.size()]));
        }

        protected List<ProjectBean> getBeans(final Parameter _parameter)
            throws EFapsException
        {
            if (this.beans == null) {
                this.beans = new ArrayList<>();
                final Map<Instance, ProjectBean> map = new HashMap<>();
                final QueryBuilder queryBldr = new QueryBuilder(CISales.DocumentSumAbstract);
                add2QueryBuilder(_parameter, queryBldr);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selProject = SelectBuilder.get()
                                .linkfrom(CIProjects.Project2DocumentAbstract.ToAbstract)
                                .linkto(CIProjects.Project2DocumentAbstract.FromAbstract);
                final SelectBuilder selProjectInst = new SelectBuilder(selProject).instance();
                multi.addSelect(selProjectInst);
                // Project_ProjectMsgPhrase
                final MsgPhrase msgPhrase = MsgPhrase.get(UUID.fromString("64c30826-cb22-4579-a3d5-bd10090f155e"));
                multi.addMsgPhrase(selProject, msgPhrase);
                multi.addAttribute(CISales.DocumentSumAbstract.CrossTotal, CISales.DocumentSumAbstract.NetTotal);
                multi.execute();
                while (multi.next()) {
                    final Instance projectInst = multi.getSelect(selProjectInst);
                    ProjectBean bean;
                    if (map.containsKey(projectInst)) {
                        bean = map.get(projectInst);
                    } else {
                        bean = new ProjectBean().setInstance(projectInst);
                        bean.setName(multi.getMsgPhrase(selProject, msgPhrase));
                        map.put(projectInst, bean);
                    }
                    bean.add(multi.getCurrentInstance().getType(),
                                    multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.NetTotal),
                                    multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal));
                }
                this.beans.addAll(map.values());
            }
            return this.beans;
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        public FilteredReport getFilteredReport()
        {
            return this.filteredReport;
        }
    }

    public static class ProjectBean
    {

        private Instance instance;

        private final Map<Type, DocBean> docs = new HashMap<>();

        private String name;

        /**
         * Getter method for the instance variable {@link #instance}.
         *
         * @return value of instance variable {@link #instance}
         */
        public Instance getInstance()
        {
            return this.instance;
        }

        /**
         * @param _type
         * @param _attribute
         * @param _attribute2
         */
        public void add(final Type _type,
                        final BigDecimal _net,
                        final BigDecimal _cross)
        {
            DocBean docBean;
            if (this.docs.containsKey(_type)) {
                docBean = this.docs.get(_type);
            } else {
                docBean = new DocBean().setType(_type);
                this.docs.put(_type, docBean);
            }
            docBean.addNet(_net);
            docBean.addCross(_cross);
        }

        /**
         * Setter method for instance variable {@link #instance}.
         *
         * @param _instance value for instance variable {@link #instance}
         */
        public ProjectBean setInstance(final Instance _instance)
        {
            this.instance = _instance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docs}.
         *
         * @return value of instance variable {@link #docs}
         */
        public Map<Type, DocBean> getDocs()
        {
            return this.docs;
        }

        /**
         * Getter method for the instance variable {@link #name}.
         *
         * @return value of instance variable {@link #name}
         */
        public String getName()
        {
            return this.name;
        }

        /**
         * Setter method for instance variable {@link #name}.
         *
         * @param _name value for instance variable {@link #name}
         */
        public ProjectBean setName(final String _name)
        {
            this.name = _name;
            return this;
        }

        public String getNetKey()
        {
            return getInstance().getOid() + "_net";
        }

        public String getTaxKey()
        {
            return getInstance().getOid() + "_tax";
        }

        public String getCrossKey()
        {
            return getInstance().getOid() + "_cross";
        }
    }

    public static class DocBean
    {

        private BigDecimal cross = BigDecimal.ZERO;
        private BigDecimal net = BigDecimal.ZERO;
        private Type type;

        /**
         * Getter method for the instance variable {@link #cross}.
         *
         * @return value of instance variable {@link #cross}
         */
        public BigDecimal getCross()
        {
            return this.cross;
        }

        /**
         * Setter method for instance variable {@link #cross}.
         *
         * @param _cross value for instance variable {@link #cross}
         */
        public DocBean addCross(final BigDecimal _cross)
        {
            this.cross = this.cross.add(_cross);
            return this;
        }

        /**
         * Getter method for the instance variable {@link #net}.
         *
         * @return value of instance variable {@link #net}
         */
        public BigDecimal getNet()
        {
            return this.net;
        }

        /**
         * Setter method for instance variable {@link #net}.
         *
         * @param _net value for instance variable {@link #net}
         */
        public DocBean addNet(final BigDecimal _net)
        {
            this.net = this.net.add(_net);
            return this;
        }

        /**
         * Getter method for the instance variable {@link #type}.
         *
         * @return value of instance variable {@link #type}
         */
        public Type getType()
        {
            return this.type;
        }

        /**
         * Setter method for instance variable {@link #type}.
         *
         * @param _type value for instance variable {@link #type}
         */
        public DocBean setType(final Type _type)
        {
            this.type = _type;
            return this;
        }

        /**
         * Setter method for instance variable {@link #cross}.
         *
         * @param _cross value for instance variable {@link #cross}
         */
        public DocBean setCross(final BigDecimal _cross)
        {
            this.cross = _cross;
            return this;
        }

        /**
         * Setter method for instance variable {@link #net}.
         *
         * @param _net value for instance variable {@link #net}
         */
        public DocBean setNet(final BigDecimal _net)
        {
            this.net = _net;
            return this;
        }
    }

    public static class HeaderConditionExpression
        extends AbstractSimpleExpression<Boolean>
    {

        private static final long serialVersionUID = 1L;

        @Override
        public Boolean evaluate(final ReportParameters reportParameters)
        {
            final Style style = reportParameters.getValue("style");
            return Style.HEADER.equals(style);
        }
    }

    public static class TotalConditionExpression
        extends AbstractSimpleExpression<Boolean>
    {

        private static final long serialVersionUID = 1L;

        @Override
        public Boolean evaluate(final ReportParameters reportParameters)
        {
            final Style style = reportParameters.getValue("style");
            return Style.TOTAL.equals(style);
        }
    }
}
