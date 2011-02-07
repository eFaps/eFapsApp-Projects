/*
 * Copyright 2003 - 2011 The eFaps Team
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

package org.efaps.esjp.projects.task;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.common.chart.LineChart;
import org.efaps.util.EFapsException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.CustomXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.time.Day;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("ec8afa02-acd5-468b-bb72-af1f5685b777")
@EFapsRevision("$Rev$")
public abstract class Progress_Base
{

    public Return getChartFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final ProgressChart linechart = new ProgressChart();
        final Return ret = linechart.execute(_parameter);
        return ret;
    }

    public class ProgressChart
        extends LineChart
    {

        @Override
        protected JFreeChart createChart(final Parameter _parameter,
                                         final XYDataset _dataset,
                                         final XYToolTipGenerator _toolTipGen)
            throws EFapsException
        {

            final JFreeChart chart = ChartFactory.createTimeSeriesChart(
                            getTitel(_parameter),
                            getXAxisLabel(_parameter),
                            getYAxisLabel(_parameter),
                            _dataset,
                            true,
                            true,
                            false
            );
            return chart;
        }

        @Override
        protected String getYAxisLabel(final Parameter _parameter)
            throws EFapsException
        {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            print.addAttribute(CIProjects.TaskScheduled.UoM);
            print.execute();
            final Long uoMId = print.<Long>getAttribute(CIProjects.TaskScheduled.UoM);
            String ret = "";
            if (uoMId != null) {
                ret = Dimension.getUoM(uoMId).getName();
            }
            return ret;
        }

        @Override
        protected XYDataset getDataSet(final Parameter _parameter)
        {
            return new TimeSeriesCollection();
        }

        @Override
        protected void fillData(final Parameter _parameter,
                                final XYDataset _dataset,
                                final CustomXYToolTipGenerator _ttg)
            throws EFapsException
        {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            print.addAttribute(CIProjects.TaskScheduled.Quantity, CIProjects.TaskScheduled.DateFrom,
                            CIProjects.TaskScheduled.DateUntil);
            print.execute();
            BigDecimal quantity = print.<BigDecimal>getAttribute(CIProjects.TaskScheduled.Quantity);
            if (quantity == null) {
                quantity = BigDecimal.ZERO;
            }
            final DateTime until = print.<DateTime>getAttribute(CIProjects.TaskScheduled.DateUntil);
            final DateTime from = print.<DateTime>getAttribute(CIProjects.TaskScheduled.DateFrom);

            final TimeSeries series = new TimeSeries(DBProperties.getProperty(
                            "org.efaps.esjp.projects.task.Progress.targetSeries"));
            final List<String> toolTips = new ArrayList<String>();
            final RegularTimePeriod t = new Day(from.toDate());
            series.add(t, 0);
            final DateTimeFormatter formatter = DateTimeFormat.mediumDate();
            final String fromDate = from.withChronology(Context.getThreadContext().getChronology()).toString(
                            formatter.withLocale(Context.getThreadContext().getLocale()));
            final String untilDate = until.withChronology(Context.getThreadContext().getChronology()).toString(
                            formatter.withLocale(Context.getThreadContext().getLocale()));
            toolTips.add(fromDate + " - 0");
            series.addOrUpdate(new Day(until.toDate()), quantity);
            toolTips.add(untilDate + " - " + quantity.toPlainString());
            ((TimeSeriesCollection) _dataset).addSeries(series);
            _ttg.addToolTipSeries(toolTips);
            //get the progress for this task
            final QueryBuilder queryBldr = new QueryBuilder(CIProjects.ProgressTaskAbstract);
            queryBldr.addWhereAttrEqValue(CIProjects.ProgressTaskAbstract.TaskAbstractLink,
                            _parameter.getInstance().getId());
            queryBldr.addOrderByAttributeAsc(CIProjects.ProgressTaskAbstract.Date);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.setEnforceSorted(true);
            multi.addAttribute(CIProjects.ProgressTaskAbstract.Date,
                            CIProjects.ProgressTaskAbstract.UoM,
                            CIProjects.ProgressTaskAbstract.Progress);
            if (multi.execute()) {
                final TimeSeries series2 = new TimeSeries(DBProperties.getProperty(
                                "org.efaps.esjp.projects.task.Progress.progressSeries"));
                ((TimeSeriesCollection) _dataset).addSeries(series2);
                final List<String> toolTips2 = new ArrayList<String>();
                _ttg.addToolTipSeries(toolTips2);
                while (multi.next()) {
                    final DateTime date = multi.<DateTime>getAttribute(CIProjects.ProgressTaskAbstract.Date);
                    final BigDecimal value = multi.<BigDecimal>getAttribute(CIProjects.ProgressTaskAbstract.Progress);
                    series2.addOrUpdate(new Day(date.toDate()), value);
                    final String dateStr = date.withChronology(Context.getThreadContext().getChronology()).toString(
                                    formatter.withLocale(Context.getThreadContext().getLocale()));
                    toolTips2.add(dateStr + " - " + value.toPlainString());
                }

            }
            final ProgressSeries progSeries = getSubTaskProgressSeries(_parameter, _parameter.getInstance(), from,
                            until);
            if (!progSeries.isEmpty()) {
                final TimeSeries series3 = new TimeSeries(DBProperties.getProperty(
                                "org.efaps.esjp.projects.task.Progress.subSeries"));
                ((TimeSeriesCollection) _dataset).addSeries(series3);
                final List<String> toolTips3 = new ArrayList<String>();
                _ttg.addToolTipSeries(toolTips3);
                for (final Entry<DateTime, BigDecimal> entry : progSeries.entrySet()) {

                    series3.addOrUpdate(new Day(entry.getKey().toDate()), entry.getValue());
                    final String dateStr = entry.getKey().withChronology(Context.getThreadContext().getChronology())
                                    .toString(formatter.withLocale(Context.getThreadContext().getLocale()));
                    toolTips3.add(dateStr + " - " + entry.getValue().toPlainString());
                }
            }
        }
    }

    protected ProgressSeries getSubTaskProgressSeries(final Parameter _parameter,
                                                      final Instance _taskInstance,
                                                      final DateTime _from,
                                                      final DateTime _until)
        throws EFapsException
    {
        final List<ProgressSeries> seriesCollection = new ArrayList<ProgressSeries>();
        final QueryBuilder queryBldr = new QueryBuilder(CIProjects.TaskAbstract);
        queryBldr.addWhereAttrEqValue(CIProjects.TaskAbstract.ParentTaskAbstractLink, _taskInstance.getId());
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        while (query.next()) {
            ProgressSeries series;
            final QueryBuilder prQueryBldr = new QueryBuilder(CIProjects.ProgressTaskAbstract);
            prQueryBldr.addWhereAttrEqValue(CIProjects.ProgressTaskAbstract.TaskAbstractLink,
                            query.getCurrentValue().getId());
            final MultiPrintQuery multi = prQueryBldr.getPrint();
            multi.setEnforceSorted(true);
            final SelectBuilder selQuan = new SelectBuilder().linkto(CIProjects.ProgressTaskAbstract.TaskAbstractLink)
                .attribute(CIProjects.TaskAbstract.Quantity);
            final SelectBuilder selWeight = new SelectBuilder().linkto(CIProjects.ProgressTaskAbstract.TaskAbstractLink)
                .attribute(CIProjects.TaskAbstract.Weight);
            multi.addSelect(selQuan, selWeight);
            multi.addAttribute(CIProjects.ProgressTaskAbstract.Date,
                            CIProjects.ProgressTaskAbstract.UoM,
                            CIProjects.ProgressTaskAbstract.Progress);
            if (multi.execute()) {
                series = new ProgressSeries();
                while (multi.next()) {
                    final DateTime date = multi.<DateTime>getAttribute(CIProjects.ProgressTaskAbstract.Date);
                    final BigDecimal value = multi.<BigDecimal>getAttribute(CIProjects.ProgressTaskAbstract.Progress);
                    final BigDecimal quantity = multi.<BigDecimal>getSelect(selQuan);
                    final BigDecimal weight = multi.<BigDecimal>getSelect(selWeight);
                    series.setWeight(weight);
                    final BigDecimal current;
                    if (quantity != null) {
                        current = value.multiply(new BigDecimal(100).setScale(8).divide(quantity, BigDecimal.ROUND_HALF_UP));
                    } else {
                        current = BigDecimal.ZERO;
                    }
                    series.put(date, current);
                }
            } else {
                series = getSubTaskProgressSeries(_parameter, query.getCurrentValue(), _from, _until);
            }
            seriesCollection.add(series);
        }
        return combineProgressSeries(_parameter, seriesCollection, _from, _until);
    }


    public ProgressSeries combineProgressSeries(final Parameter _parameter,
                                                final List<ProgressSeries> _seriesCollection,
                                                final DateTime _from,
                                                final DateTime _until)
    {
        final ProgressSeries ret = new ProgressSeries();
        DateTime current = _from;
        if (!_seriesCollection.isEmpty()) {
            while (current.isBefore(_until)) {
                boolean hasValues = false;
                BigDecimal value = BigDecimal.ZERO;
                BigDecimal weights = BigDecimal.ZERO;
                for (final ProgressSeries series : _seriesCollection) {
                    if (!hasValues) {
                        hasValues = series.containsKey(current);
                    }
                    weights = weights.add(series.getWeight());
                    value = value.add(series.getWeight().multiply(series.get(current)));
                }
                if (value.compareTo(BigDecimal.ZERO) != 0 && hasValues) {
                    ret.put(current, value.divide(weights, BigDecimal.ROUND_HALF_UP));
                } else if (!hasValues && current.equals(_from)) {
                    ret.put(current, BigDecimal.ZERO);
                }
                current = current.plusDays(1);
            }
        }
        return ret;
    }

    public class ProgressSeries
        extends TreeMap<DateTime, BigDecimal>
    {
        private BigDecimal weight = BigDecimal.ONE;

        /**
         * Getter method for the instance variable {@link #weight}.
         *
         * @return value of instance variable {@link #weight}
         */
        protected BigDecimal getWeight()
        {
            return this.weight;
        }


        /**
         * Setter method for instance variable {@link #weight}.
         *
         * @param _weight value for instance variable {@link #weight}
         */

        protected void setWeight(final BigDecimal _weight)
        {
            if (_weight != null) {
                this.weight = _weight;
            }
        }

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        private BigDecimal last = BigDecimal.ZERO;

        /* (non-Javadoc)
         * @see java.util.TreeMap#get(java.lang.Object)
         */
        @Override
        public BigDecimal get(final Object _key)
        {
            BigDecimal ret;
            if (containsKey(_key)) {
                ret = super.get(_key);
                this.last = ret;
            } else {
                ret = this.last;
            }
            return ret;
        }

    }
}
