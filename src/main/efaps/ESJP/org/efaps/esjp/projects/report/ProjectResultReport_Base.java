/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.projects.report;

import java.awt.Color;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.efaps.admin.common.MsgPhrase;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.projects.listener.IOnResultReport;
import org.efaps.esjp.projects.util.Projects;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;

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

/**
 * @author The eFaps Team
 */
@EFapsUUID("9b3c65df-97aa-4fe5-84f3-2931e4f66cc1")
@EFapsApplication("eFapsApp-Projects")
public abstract class ProjectResultReport_Base
    extends FilteredReport
{

    /**
     * Enum used for styling.
     */
    public enum Style
    {
        /** None. */
        NONE,
        /** HEADER. */
        HEADER,
        /** TOTAL. */
        TOTAL;
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

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return a DynamicReport
     * @throws EFapsException on error
     */
    protected DynProjectResultReport getDynReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynProjectResultReport(this);
    }

    /**
     * Dynamic Report.
     */
    public static class DynProjectResultReport
        extends AbstractDynamicReport
    {

        /**
         * Beans.
         */
        private List<ProjectBean> beans;

        /**
         * Report this DynamicReport belongs to.
         */
        private final FilteredReport filteredReport;

        /**
         * @param _filteredReport filtered report
         */
        public DynProjectResultReport(final FilteredReport _filteredReport)
        {
            filteredReport = _filteredReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final List<Map<String, Object>> source = new ArrayList<>();

            if (transpose(_parameter)) {
                final Map<Type, String> maping = new HashMap<>();
                maping.putAll(getTypeMap(_parameter, "Expense"));
                maping.putAll(getTypeMap(_parameter, "Collection"));
                maping.putAll(getTypeMap(_parameter, "Estimate"));

                final Map<String, Object> totalMap = new HashMap<>();
                totalMap.put("project", DBProperties.getProperty(ProjectResultReport.class.getName() + ".total.descr"));
                totalMap.put("style", Style.TOTAL.toString());
                final Properties properties = Projects.RESULTREPORT.get();
                for (final ProjectBean bean : getBeans(_parameter)) {
                    BigDecimal expenseNet = BigDecimal.ZERO;
                    BigDecimal estimateNet = BigDecimal.ZERO;
                    BigDecimal collectionNet = BigDecimal.ZERO;
                    BigDecimal expenseCross = BigDecimal.ZERO;
                    BigDecimal collectionCross = BigDecimal.ZERO;
                    BigDecimal estimateCross = BigDecimal.ZERO;

                    final Map<String, Object> map = new HashMap<>();
                    source.add(map);
                    map.put("project", bean.getName());
                    for (final Entry<Type, DocBean> entry : bean.getDocs().entrySet()) {
                        final boolean negate = "true".equalsIgnoreCase(properties.getProperty(entry.getKey().getName()
                                        + ".negate"));

                        BigDecimal cross = entry.getValue().getCross();
                        BigDecimal net = entry.getValue().getNet();
                        BigDecimal tax = cross.subtract(net);
                        if (negate) {
                            cross = cross.negate();
                            net = net.negate();
                            tax = tax.negate();
                        }
                        map.put(entry.getKey().getName() + ".net", net);
                        map.put(entry.getKey().getName() + ".cross", cross);
                        map.put(entry.getKey().getName() + ".tax", tax);

                        add2Map(totalMap, entry.getKey().getName() + ".net", net);
                        add2Map(totalMap, entry.getKey().getName() + ".cross", cross);
                        add2Map(totalMap, entry.getKey().getName() + ".tax", tax);

                        String caseStr;
                        if (maping.containsKey(entry.getKey())) {
                            caseStr = maping.get(entry.getKey());
                        } else {
                            caseStr = "unknown";
                        }
                        switch (caseStr) {
                            case "Expense":
                                expenseNet = expenseNet.add(net);
                                expenseCross = expenseCross.add(cross);
                                break;
                            case "Collection":
                                collectionNet = collectionNet.add(net);
                                collectionCross = collectionCross.add(cross);
                                break;
                            case "Estimate":
                                estimateNet = estimateNet.add(net);
                                estimateCross = estimateCross.add(cross);
                                break;
                            default:
                                break;
                        }
                    }

                    BigDecimal estimateGainPercentNet = BigDecimal.ZERO;
                    BigDecimal estimateGainPercentCross = BigDecimal.ZERO;
                    BigDecimal collectionGainPercentNet = BigDecimal.ZERO;
                    BigDecimal collectionGainPercentCross = BigDecimal.ZERO;

                    if (estimateNet.compareTo(BigDecimal.ZERO) != 0) {
                        estimateGainPercentNet = BigDecimal.ONE.subtract(
                                        expenseNet.divide(estimateNet, RoundingMode.HALF_UP))
                                        .multiply(new BigDecimal(100));
                    }
                    if (estimateCross.compareTo(BigDecimal.ZERO) != 0) {
                        estimateGainPercentCross = BigDecimal.ONE.subtract(expenseCross
                                        .divide(estimateCross, RoundingMode.HALF_UP))
                                        .multiply(new BigDecimal(100));
                    }
                    if (collectionNet.compareTo(BigDecimal.ZERO) != 0) {
                        collectionGainPercentNet = BigDecimal.ONE.subtract(
                                        expenseNet.divide(collectionNet, RoundingMode.HALF_UP))
                                        .multiply(new BigDecimal(100));
                    }
                    if (collectionCross.compareTo(BigDecimal.ZERO) != 0) {
                        collectionGainPercentCross = BigDecimal.ONE.subtract(expenseCross
                                        .divide(collectionCross, RoundingMode.HALF_UP))
                                        .multiply(new BigDecimal(100));
                    }

                    map.put("estimateGain.gain.net", estimateNet.subtract(expenseNet));
                    map.put("estimateGain.gainPercent.net", estimateGainPercentNet);
                    map.put("estimateGain.gain.cross", estimateCross.subtract(expenseCross));
                    map.put("estimateGain.gainPercent.cross", estimateGainPercentCross);
                    map.put("collectionGain.gain.net", collectionNet.subtract(expenseNet));
                    map.put("collectionGain.gainPercent.net", collectionGainPercentNet);
                    map.put("collectionGain.gain.cross", collectionCross.subtract(expenseCross));
                    map.put("collectionGain.gainPercent.cross", collectionGainPercentCross);

                    add2Map(totalMap, "expenseNet", expenseNet);
                    add2Map(totalMap, "estimateNet", estimateNet);
                    add2Map(totalMap, "collectionNet", collectionNet);
                    add2Map(totalMap, "expenseCross", expenseCross);
                    add2Map(totalMap, "collectionCross", collectionCross);
                    add2Map(totalMap, "estimateCross", estimateCross);
                }

                source.add(totalMap);

                // total percent calculation
                final BigDecimal expenseNet = (BigDecimal) totalMap.get("expenseNet");
                final BigDecimal estimateNet = (BigDecimal) totalMap.get("estimateNet");
                final BigDecimal collectionNet = (BigDecimal) totalMap.get("collectionNet");
                final BigDecimal expenseCross = (BigDecimal) totalMap.get("expenseCross");
                final BigDecimal collectionCross = (BigDecimal) totalMap.get("collectionCross");
                final BigDecimal estimateCross = (BigDecimal) totalMap.get("estimateCross");

                BigDecimal estimateGainPercentNet = BigDecimal.ZERO;
                BigDecimal estimateGainPercentCross = BigDecimal.ZERO;
                BigDecimal collectionGainPercentNet = BigDecimal.ZERO;
                BigDecimal collectionGainPercentCross = BigDecimal.ZERO;

                if (estimateNet.compareTo(BigDecimal.ZERO) != 0) {
                    estimateGainPercentNet = BigDecimal.ONE.subtract(
                                    expenseNet.divide(estimateNet, RoundingMode.HALF_UP))
                                    .multiply(new BigDecimal(100));
                }
                if (estimateCross.compareTo(BigDecimal.ZERO) != 0) {
                    estimateGainPercentCross = BigDecimal.ONE.subtract(expenseCross
                                    .divide(estimateCross, RoundingMode.HALF_UP))
                                    .multiply(new BigDecimal(100));
                }
                if (collectionNet.compareTo(BigDecimal.ZERO) != 0) {
                    collectionGainPercentNet = BigDecimal.ONE.subtract(
                                    expenseNet.divide(collectionNet, RoundingMode.HALF_UP))
                                    .multiply(new BigDecimal(100));
                }
                if (collectionCross.compareTo(BigDecimal.ZERO) != 0) {
                    collectionGainPercentCross = BigDecimal.ONE.subtract(expenseCross
                                    .divide(collectionCross, RoundingMode.HALF_UP))
                                    .multiply(new BigDecimal(100));
                }
                totalMap.put("estimateGain.gain.net", estimateNet.subtract(expenseNet));
                totalMap.put("estimateGain.gainPercent.net", estimateGainPercentNet);
                totalMap.put("estimateGain.gain.cross", estimateCross.subtract(expenseCross));
                totalMap.put("estimateGain.gainPercent.cross", estimateGainPercentCross);
                totalMap.put("collectionGain.gain.net", collectionNet.subtract(expenseNet));
                totalMap.put("collectionGain.gainPercent.net", collectionGainPercentNet);
                totalMap.put("collectionGain.gain.cross", collectionCross.subtract(expenseCross));
                totalMap.put("collectionGain.gainPercent.cross", collectionGainPercentCross);

            } else {
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
            }
            return new JRMapCollectionDataSource(new ArrayList<Map<String, ?>>(source));
        }

        /**
         * @param _map map to add to
         * @param _key ket to be used
         * @param _amount amount to be added
         */
        protected void add2Map(final Map<String, Object> _map,
                               final String _key,
                               final BigDecimal _amount)
        {
            if (_map.containsKey(_key)) {
                _map.put(_key, ((BigDecimal) _map.get(_key)).add(_amount));
            } else {
                _map.put(_key, _amount);
            }
        }

        protected Map<Type, String> getTypeMap(final Parameter _parameter,
                                               final String _key)
                                                   throws EFapsException
        {
            final Map<Type, String> ret = new HashMap<>();
            final Properties properties = Projects.RESULTREPORT.get();
            int i = 1;
            String keyTmp = _key + String.format("%02d", i);
            while (properties.containsKey(keyTmp)) {
                final String typeStr = properties.getProperty(keyTmp);
                ret.put(Type.get(typeStr), _key);
                i++;
                keyTmp = _key + String.format("%02d", i);
            }
            return ret;
        }

        protected void addHeader(final Parameter _parameter,
                                 final List<Map<String, Object>> _source,
                                 final String _key)
        {
            final Map<String, Object> map = new HashMap<>();
            map.put("descr", DBProperties.getProperty(ProjectResultReport.class.getName() + "." + _key + ".descr"));
            map.put("style", Style.HEADER.toString());
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
                    netPercent = BigDecimal.ONE.subtract(targetNetTotal.divide(baseNetTotal, RoundingMode.HALF_UP))
                                    .multiply(new BigDecimal(100));
                }
                if (baseCrossTotal.compareTo(BigDecimal.ZERO) != 0) {
                    crossPercent = BigDecimal.ONE.subtract(targetCrossTotal
                                    .divide(baseCrossTotal, RoundingMode.HALF_UP))
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
            totalMap.put("style", Style.TOTAL.toString());
            final Properties properties = Projects.RESULTREPORT.get();
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
                            final boolean negate = "true".equalsIgnoreCase(properties.getProperty(type.getName()
                                            + ".negate"));
                            final DocBean doc = bean.getDocs().get(type);
                            BigDecimal cross = doc.getCross();
                            BigDecimal net = doc.getNet();
                            BigDecimal tax = cross.subtract(net);
                            if (negate) {
                                cross = cross.negate();
                                net = net.negate();
                                tax = tax.negate();
                            }
                            map.put(bean.getNetKey(), net);
                            map.put(bean.getCrossKey(), cross);
                            map.put(bean.getTaxKey(), tax);
                            if (totalMap.containsKey(bean.getNetKey())) {
                                totalMap.put(bean.getNetKey(), ((BigDecimal) totalMap.get(bean.getNetKey())).add(net));
                                totalMap.put(bean.getCrossKey(),
                                                ((BigDecimal) totalMap.get(bean.getCrossKey())).add(cross));
                                totalMap.put(bean.getTaxKey(),
                                                ((BigDecimal) totalMap.get(bean.getTaxKey())).add(tax));
                            } else {
                                totalMap.put(bean.getNetKey(), net);
                                totalMap.put(bean.getCrossKey(), cross);
                                totalMap.put(bean.getTaxKey(), tax);
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
                final QueryBuilder projAttrQueryBldr = new QueryBuilder(CIProjects.ProjectAbstract);
                final Set<Instance> projInsts = new HashSet<>();
                if (filterMap.containsKey("project")) {
                    final InstanceSetFilterValue filter = (InstanceSetFilterValue) filterMap.get("project");
                    if (filter.getObject() != null) {
                        for (final Instance instance : filter.getObject()) {
                            if (instance.isValid()) {
                                projInsts.add(instance);
                            }
                        }
                    }
                }
                if (!projInsts.isEmpty()) {
                    projAttrQueryBldr.addWhereAttrEqValue(CIProjects.ProjectAbstract.ID, projInsts.toArray());
                }

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
                Boolean dateTarget = false;
                if (filterMap.containsKey("dateTarget")) {
                    dateTarget = (Boolean) filterMap.get("dateTarget");
                }
                if (dateTarget) {
                    projAttrQueryBldr.addWhereAttrLessValue(CIProjects.ProjectAbstract.Date,
                                    end.withTimeAtStartOfDay().plusDays(1));
                    projAttrQueryBldr.addWhereAttrGreaterValue(CIProjects.ProjectAbstract.Date,
                                    start.withTimeAtStartOfDay().minusMinutes(1));
                } else {
                    _queryBldr.addWhereAttrLessValue(CISales.DocumentAbstract.Date,
                                    end.withTimeAtStartOfDay().plusDays(1));
                    _queryBldr.addWhereAttrGreaterValue(CISales.DocumentAbstract.Date,
                                    start.withTimeAtStartOfDay().minusMinutes(1));
                }

                final QueryBuilder attrQueryBldr = new QueryBuilder(CIProjects.Project2DocumentAbstract);
                attrQueryBldr.addWhereAttrInQuery(CIProjects.Project2DocumentAbstract.FromAbstract,
                                projAttrQueryBldr.getAttributeQuery(CIProjects.ProjectAbstract.ID));
                _queryBldr.addWhereAttrInQuery(CISales.DocumentAbstract.ID,
                                attrQueryBldr.getAttributeQuery(CIProjects.Project2DocumentAbstract.ToAbstract));
            }
            final Properties properties = Projects.RESULTREPORT.get();

            final List<Status> statusList = getStatusListFromProperties(_parameter, properties);
            if (!statusList.isEmpty()) {
                _queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.StatusAbstract, statusList.toArray());
            }
        }

        protected ColumnTitleGroupBuilder getColumnGroup(final Parameter _parameter,
                                                         final JasperReportBuilder _builder,
                                                         final String _key,
                                                         final String _propKey,
                                                         final StyleBuilder _headerStyle)
                                                             throws EFapsException
        {
            final ColumnTitleGroupBuilder ret = DynamicReports.grid.titleGroup(
                            DBProperties.getProperty(ProjectResultReport.class.getName() + "." + _propKey + ".descr"));

            final Properties properties = Projects.RESULTREPORT.get();
            int i = 1;
            String keyTmp = _key + String.format("%02d", i);
            boolean result = true;
            while (properties.containsKey(keyTmp)) {
                result = false;
                final String typeStr = properties.getProperty(keyTmp);

                final TextColumnBuilder<BigDecimal> netColumn = DynamicReports.col.column(DBProperties
                                .getProperty(ProjectResultReport.class.getName() + ".Column.net"),
                                typeStr + ".net", DynamicReports.type.bigDecimalType()).setWidth(80)
                                .setStyle(_headerStyle);
                final TextColumnBuilder<BigDecimal> taxColumn = DynamicReports.col.column(DBProperties
                                .getProperty(ProjectResultReport.class.getName() + ".Column.tax"),
                                typeStr + ".tax", DynamicReports.type.bigDecimalType()).setWidth(80)
                                .setStyle(_headerStyle);
                final TextColumnBuilder<BigDecimal> crossColumn = DynamicReports.col.column(DBProperties
                                .getProperty(ProjectResultReport.class.getName() + ".Column.cross"),
                                typeStr + ".cross", DynamicReports.type.bigDecimalType()).setWidth(80)
                                .setStyle(_headerStyle);

                final ColumnTitleGroupBuilder group = DynamicReports.grid.titleGroup(Type.get(typeStr).getLabel(),
                                netColumn, taxColumn, crossColumn);
                ret.add(group);
                _builder.addColumn(netColumn, taxColumn, crossColumn);
                i++;
                keyTmp = _key + String.format("%02d", i);
            }
            if (result) {
                final TextColumnBuilder<BigDecimal> netColumn = DynamicReports.col.column(DBProperties
                                .getProperty(ProjectResultReport.class.getName() + ".Column.net"),
                                _propKey + ".gain.net", DynamicReports.type.bigDecimalType()).setStyle(_headerStyle);
                final TextColumnBuilder<BigDecimal> crossColumn = DynamicReports.col.column(DBProperties
                                .getProperty(ProjectResultReport.class.getName() + ".Column.cross"),
                                _propKey + ".gain.cross", DynamicReports.type.bigDecimalType()).setStyle(_headerStyle);
                final TextColumnBuilder<BigDecimal> netPercentColumn = DynamicReports.col.column(DBProperties
                                .getProperty(ProjectResultReport.class.getName() + ".Column.net"),
                                _propKey + ".gainPercent.net", DynamicReports.type.bigDecimalType()).setStyle(
                                                _headerStyle);
                final TextColumnBuilder<BigDecimal> crossPercentColumn = DynamicReports.col.column(DBProperties
                                .getProperty(ProjectResultReport.class.getName() + ".Column.cross"),
                                _propKey + ".gainPercent.cross", DynamicReports.type.bigDecimalType()).setStyle(
                                                _headerStyle);

                final ColumnTitleGroupBuilder gainGroup = DynamicReports.grid.titleGroup(DBProperties
                                .getProperty(ProjectResultReport.class.getName() + ".gain.descr"), netColumn,
                                crossColumn);

                final ColumnTitleGroupBuilder gainPercentGroup = DynamicReports.grid.titleGroup(DBProperties
                                .getProperty(ProjectResultReport.class.getName() + ".gainPercent.descr"),
                                netPercentColumn, crossPercentColumn);

                ret.add(gainGroup, gainPercentGroup);
                _builder.addColumn(netColumn, crossColumn, netPercentColumn, crossPercentColumn);
            }
            return ret;
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
                                              throws EFapsException
        {
            final ConditionalStyleBuilder totalCondition = DynamicReports.stl.conditionalStyle(
                            new TotalConditionExpression()).setBold(true).setItalic(true);
            if (transpose(_parameter)) {
                final StyleBuilder headerStyle = DynamicReports.stl.style()
                                .conditionalStyles(totalCondition);
                _builder.addField("style", String.class);
                final TextColumnBuilder<String> projectColumn = DynamicReports.col.column(DBProperties
                                .getProperty(ProjectResultReport.class.getName() + ".Column.project"),
                                "project", DynamicReports.type.stringType()).setWidth(250).setStyle(headerStyle);
                _builder.addColumn(projectColumn);
                final ColumnTitleGroupBuilder estimateGroup = getColumnGroup(_parameter, _builder, "Estimate",
                                "estimate", headerStyle);
                final ColumnTitleGroupBuilder expenseGroup = getColumnGroup(_parameter, _builder, "Expense", "expense",
                                headerStyle);
                final ColumnTitleGroupBuilder collectionGroup = getColumnGroup(_parameter, _builder, "Collection",
                                "collection", headerStyle);
                final ColumnTitleGroupBuilder estimateGainGroup = getColumnGroup(_parameter, _builder, null,
                                "estimateGain", headerStyle);
                final ColumnTitleGroupBuilder collectionGainGroup = getColumnGroup(_parameter, _builder, null,
                                "collectionGain", headerStyle);
                _builder.columnGrid(projectColumn, estimateGroup, expenseGroup, collectionGroup, estimateGainGroup,
                                collectionGainGroup);
            } else {
                final ConditionalStyleBuilder headerCondition = DynamicReports.stl.conditionalStyle(
                                new HeaderConditionExpression()).setBold(true).setBackgroundColor(Color.darkGray)
                                .setForegroundColor(Color.white);

                final StyleBuilder headerStyle = DynamicReports.stl.style()
                                .conditionalStyles(headerCondition, totalCondition);
                _builder.addField("style", String.class);

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
                    final ColumnTitleGroupBuilder projectGroup = DynamicReports.grid.titleGroup(bean.getName(),
                                    netColumn,
                                    taxColumn, crossColumn);
                    projectGroup.setTitleFixedWidth(250);
                    groupBuilders.add(projectGroup);
                    _builder.addColumn(netColumn, taxColumn, crossColumn);
                }
                _builder.columnGrid(groupBuilders.toArray(new ColumnGridComponentBuilder[groupBuilders.size()]));
            }
        }

        public boolean transpose(final Parameter _parameter)
            throws EFapsException
        {
            boolean ret = false;
            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
            if (filterMap.containsKey("transpose")) {
                ret = (Boolean) filterMap.get("transpose");
            }
            return ret;
        }

        protected List<ProjectBean> getBeans(final Parameter _parameter)
            throws EFapsException
        {
            if (beans == null) {
                beans = new ArrayList<>();
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
                    bean.add(_parameter,
                                    multi.getCurrentInstance(),
                                    multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.NetTotal),
                                    multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal));
                }
                beans.addAll(map.values());
            }
            return beans;
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        public FilteredReport getFilteredReport()
        {
            return filteredReport;
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
            return instance;
        }

        /**
         * @param _parameter Parameter as passed by the eFaps API
         * @param _instance instance the amounts belong to
         * @param _net net amount
         * @param _cross cross amount
         * @throws EFapsException on error
         */
        public void add(final Parameter _parameter,
                        final Instance _instance,
                        final BigDecimal _net,
                        final BigDecimal _cross)
                            throws EFapsException
        {
            final Type type = _instance.getType();
            DocBean docBean;
            if (docs.containsKey(type)) {
                docBean = docs.get(type);
            } else {
                docBean = new DocBean().setType(type);
                docs.put(type, docBean);
            }
            boolean add = true;
            for (final IOnResultReport listener : Listener.get().<IOnResultReport>invoke(IOnResultReport.class)) {
                add = listener.addAmounts2DocBean(_parameter, _instance, _net, _cross, docBean, add);
            }
            if (add) {
                docBean.addNet(_net);
                docBean.addCross(_cross);
            }
        }

        /**
         * Setter method for instance variable {@link #instance}.
         *
         * @param _instance value for instance variable {@link #instance}
         */
        public ProjectBean setInstance(final Instance _instance)
        {
            instance = _instance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docs}.
         *
         * @return value of instance variable {@link #docs}
         */
        public Map<Type, DocBean> getDocs()
        {
            return docs;
        }

        /**
         * Getter method for the instance variable {@link #name}.
         *
         * @return value of instance variable {@link #name}
         */
        public String getName()
        {
            return name;
        }

        /**
         * Setter method for instance variable {@link #name}.
         *
         * @param _name value for instance variable {@link #name}
         */
        public ProjectBean setName(final String _name)
        {
            name = _name;
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
            return cross;
        }

        /**
         * Setter method for instance variable {@link #cross}.
         *
         * @param _cross value for instance variable {@link #cross}
         */
        public DocBean addCross(final BigDecimal _cross)
        {
            if (_cross != null) {
                cross = cross.add(_cross);
            }
            return this;
        }

        /**
         * Getter method for the instance variable {@link #net}.
         *
         * @return value of instance variable {@link #net}
         */
        public BigDecimal getNet()
        {
            return net;
        }

        /**
         * Setter method for instance variable {@link #net}.
         *
         * @param _net value for instance variable {@link #net}
         */
        public DocBean addNet(final BigDecimal _net)
        {
            if (_net != null) {
                net = net.add(_net);
            }
            return this;
        }

        /**
         * Getter method for the instance variable {@link #type}.
         *
         * @return value of instance variable {@link #type}
         */
        public Type getType()
        {
            return type;
        }

        /**
         * Setter method for instance variable {@link #type}.
         *
         * @param _type value for instance variable {@link #type}
         */
        public DocBean setType(final Type _type)
        {
            type = _type;
            return this;
        }

        /**
         * Setter method for instance variable {@link #cross}.
         *
         * @param _cross value for instance variable {@link #cross}
         */
        public DocBean setCross(final BigDecimal _cross)
        {
            if (_cross != null) {
                cross = _cross;
            }
            return this;
        }

        /**
         * Setter method for instance variable {@link #net}.
         *
         * @param _net value for instance variable {@link #net}
         */
        public DocBean setNet(final BigDecimal _net)
        {
            net = _net;
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
            final String style = reportParameters.getValue("style");
            return "HEADER".equals(style);
        }
    }

    public static class TotalConditionExpression
        extends AbstractSimpleExpression<Boolean>
    {

        private static final long serialVersionUID = 1L;

        @Override
        public Boolean evaluate(final ReportParameters reportParameters)
        {
            final String style = reportParameters.getValue("style");
            return "TOTAL".equals(style);
        }
    }
}
