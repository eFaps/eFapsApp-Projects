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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.ui.field.Field.Display;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.projects.task.ProgressChart_Base.ProgressSeries;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.Days;


/**
 * Class for Gant representation and calculation.
 *
 * @author The eFaps Team
 * @version $Id$
 */
public abstract class Gant_Base
{
    /**
     * Key for the map to be stored in the request.
     */
    public static final String GANTREUQESTKEY = "org.efaps.esjp.projects.task.Gant.requestMap4Task";

    /**
     * Style class name for the left div.
     */
    public static final String CSSLEFT = "eFapsGantLeft";

    /**
     * Style class name for the left div.
     */
    public static final String CSSRIGHT = "eFapsGantRight";

    /**
     * Style class name for the left div.
     */
    public static final String CSSGANTBAR = "eFapsGantBar";

    /**
     * Style class name for the left div.
     */
    public static final String CSSPERCENT = "eFapsGantPercent";

    /**
     * Get the Html Snipplet that shows the gant bars in the table.
     * @param _parameter Parameter as passed form the eFaps API
     * @return html Snipplet
     * @throws EFapsException on error
     */
    public Return getGantFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        final StringBuilder html = new StringBuilder();
        // title
        if (Display.NONE.equals(fieldValue.getDisplay())) {
            html.append("");
        } else {
            @SuppressWarnings("unchecked")
            Map<Instance, String> values = (Map<Instance, String>) Context.getThreadContext()
                            .getRequestAttribute(Gant_Base.GANTREUQESTKEY);
            if (values == null || (values != null && !values.containsKey(_parameter.getInstance()))) {
                values = new HashMap<Instance, String>();
                Context.getThreadContext().setRequestAttribute(Gant_Base.GANTREUQESTKEY, values);
                @SuppressWarnings("unchecked")
                final List<Instance> instances = (List<Instance>) _parameter
                                .get(ParameterValues.REQUEST_INSTANCES);
                if (instances != null) {
                    final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
                    final boolean advance = "true".equalsIgnoreCase((String) props.get("ShowAdvance"));
                    final MultiPrintQuery multi = new MultiPrintQuery(instances);
                    final SelectBuilder selDate = new SelectBuilder()
                        .linkto(CIProjects.TaskAbstract.ProjectAbstractLink)
                        .attribute(CIProjects.ProjectAbstract.Date);
                    final SelectBuilder selDue = new SelectBuilder()
                        .linkto(CIProjects.TaskAbstract.ProjectAbstractLink)
                        .attribute(CIProjects.ProjectAbstract.DueDate);
                    multi.addSelect(selDate, selDue);
                    multi.addAttribute(CIProjects.TaskAbstract.DateFrom, CIProjects.TaskAbstract.DateUntil);
                    multi.execute();
                    boolean first = true;
                    while (multi.next()) {
                        final DateTime date = multi.<DateTime>getSelect(selDate);
                        final DateTime due = multi.<DateTime>getSelect(selDue);
                        final DateTime from = multi.<DateTime>getAttribute(CIProjects.TaskAbstract.DateFrom);
                        final DateTime until = multi.<DateTime>getAttribute(CIProjects.TaskAbstract.DateUntil);

                        final StringBuilder html4value = new StringBuilder();
                        if (first) {
                            first = false;
                            addStyle(_parameter, html4value);
                        }

                        // the number of days to the left
                        final BigDecimal left = addDayLeft(_parameter, html4value, date, due, from, until);
                        // the number of days to the right
                        final BigDecimal right = addDayRight(_parameter, html4value, date, due, from, until);
                        // the gant bar
                        addGantBar(_parameter, html4value, left, right);

                        if (advance) {
                            // the inner bar for precentage advance
                            addPercentBar(_parameter, html4value, multi.getCurrentInstance());
                        }
                        html4value.append("</div>");
                        values.put(multi.getCurrentInstance(), html4value.toString());
                    }
                }
            }
            html.append(values.get(_parameter.getInstance()));
        }
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    /**
     * @param _parameter    Parameter as passed form the eFaps API
     * @param _html         StringBuilder to append to
     * @throws EFapsException on error
     *
     */
    protected void addStyle(final Parameter _parameter,
                            final StringBuilder _html)
        throws EFapsException
    {
        _html.append("<style type=\"text/css\">")
            .append(".").append(Gant_Base.CSSLEFT).append("{")
            .append("float:left;text-align:right;")
            .append("}")
            .append(".").append(Gant_Base.CSSLEFT).append(" span{")
            .append("padding-right: 2px;")
            .append("}")
            .append(".").append(Gant_Base.CSSRIGHT).append("{")
            .append("float: right;")
            .append("}")
            .append(".").append(Gant_Base.CSSRIGHT).append(" span{")
            .append("padding-left:2px;")
            .append("}")
            .append(".").append(Gant_Base.CSSGANTBAR).append("{")
            .append("background-color: grey;height: 14px;border: 1px solid black; overflow:hidden;margin-top:1px;")
            .append("}")
            .append(".").append(Gant_Base.CSSPERCENT).append("{")
            .append("color:white;text-align:right;background-color:black;position:relative; top: 2px; height: 10px;")
            .append("}")
            .append(".").append(Gant_Base.CSSPERCENT).append(" span{")
            .append("position:relative;top:-4px;")
            .append("}")
            .append("</style>");
    }

    /**
     * @param _parameter    Parameter as passed form the eFaps API
     * @param _html         StringBuilder to append to
     * @param _projectDate  start Date of the project
     * @param _projectDue   due date of the project
     * @param _taskFrom     start date of the task
     * @param _taskUntil    stop date of the task
     * @return left
     * @throws EFapsException on error
     */
    protected BigDecimal addDayLeft(final Parameter _parameter,
                                    final StringBuilder _html,
                                    final DateTime _projectDate,
                                    final DateTime _projectDue,
                                    final DateTime _taskFrom,
                                    final DateTime _taskUntil)
        throws EFapsException
    {
        final Days projectPeriod = Days.daysBetween(_projectDate, _projectDue);
        final Days startPeriod = Days.daysBetween(_projectDate, _taskFrom);
        BigDecimal left;
        if (projectPeriod.getDays() == 0) {
            left = BigDecimal.ZERO;
        } else {
            left = new BigDecimal(100).setScale(8)
                .divide(new BigDecimal(projectPeriod.getDays()), BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal(startPeriod.getDays()))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        }

        _html.append("<span style=\"width:").append(left.abs().setScale(1, BigDecimal.ROUND_HALF_UP))
            .append("%;\" class=\"").append(Gant_Base.CSSLEFT).append("\">")
            .append("<span>").append(startPeriod.getDays()).append("</span>")
            .append("</span>");
        return left;
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _html         StringBuilder to append to
     * @param _projectDate  start Date of the project
     * @param _projectDue   due date of the project
     * @param _taskFrom     start date of the task
     * @param _taskUntil    stop date of the task
     * @return html Snipplet
     * @throws EFapsException on error
     */
    protected BigDecimal addDayRight(final Parameter _parameter,
                                     final StringBuilder _html,
                                     final DateTime _projectDate,
                                     final DateTime _projectDue,
                                     final DateTime _taskFrom,
                                     final DateTime _taskUntil)
        throws EFapsException
    {
        final Days projectPeriod = Days.daysBetween(_projectDate, _projectDue);
        final Days d3 = Days.daysBetween(_taskUntil, _projectDue);
        final Days d4 = Days.daysBetween(_projectDate, _taskUntil);
        BigDecimal right;
        if (projectPeriod.getDays() == 0) {
            right = BigDecimal.ZERO;
        } else {
            right = new BigDecimal(100).setScale(8)
                .divide(new BigDecimal(projectPeriod.getDays()), BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal(d3.getDays()))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        }

        _html.append("<span style=\"width:").append(right.abs().setScale(1, BigDecimal.ROUND_HALF_UP))
            .append("%;\" class=\"").append(Gant_Base.CSSRIGHT).append("\">")
            .append("<span>").append(d4.getDays()).append("</span>")
            .append("</span>");
        return right;
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _html         StringBuilder to append to
     * @param _left         left border
     * @param _right        right border
     * @throws EFapsException on error
     */
    protected void addGantBar(final Parameter _parameter,
                              final StringBuilder _html,
                              final BigDecimal _left,
                              final BigDecimal _right)
        throws EFapsException
    {
        _html.append("<div class=\"").append(Gant_Base.CSSGANTBAR).append("\">");
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _html         StringBuilder to append to
     * @param _taskInstance Instance of the current task
     * @throws EFapsException on error
     */
    protected void addPercentBar(final Parameter _parameter,
                                 final StringBuilder _html,
                                 final Instance _taskInstance)
        throws EFapsException
    {
        final DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        format.applyPattern("#,##0.#");
        final DecimalFormat titleFormat = (DecimalFormat) NumberFormat.getInstance(
                        Context.getThreadContext().getLocale());
        titleFormat.applyPattern("#,##0.####");

        final BigDecimal percentage = getAdvance(_parameter, _taskInstance);
        _html.append("<div style=\"width:").append(percentage.abs().setScale(1, BigDecimal.ROUND_HALF_UP))
            .append("%;\" title=\"").append(titleFormat.format(percentage))
            .append("%\" class=\"")
            .append(Gant_Base.CSSPERCENT).append("\">")
            .append("<span>")
            .append(format.format(percentage)).append("</span>")
            .append("</div>");
    }


    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _taskInstance Instance of the current task
     * @return Value of the advance in percentage
     * @throws EFapsException on error
     */
    protected BigDecimal getAdvance(final Parameter _parameter,
                                    final Instance _taskInstance)
        throws EFapsException
    {
        @SuppressWarnings("unchecked")
        final Map<Object, Object> props = (Map<Object, Object>) _parameter.get(ParameterValues.PROPERTIES);
        props.put("EvaluateMaxDate", "true");
        props.put("EvaluateMinDate", "true");

        BigDecimal ret = BigDecimal.ZERO;

        final QueryBuilder queryBldr = new QueryBuilder(CIProjects.ProgressTaskAbstract);
        queryBldr.addWhereAttrEqValue(CIProjects.ProgressTaskAbstract.TaskAbstractLink,
                        _taskInstance.getId());
        queryBldr.addOrderByAttributeDesc(CIProjects.ProgressTaskAbstract.Date);
        final InstanceQuery query = queryBldr.getQuery();
        query.setLimit(1);

        final SelectBuilder sel = new SelectBuilder()
            .linkto(CIProjects.ProgressTaskAbstract.TaskAbstractLink).attribute(CIProjects.TaskAbstract.Quantity);
        final MultiPrintQuery multi = new MultiPrintQuery(query.execute());
        multi.addSelect(sel);
        multi.addAttribute(CIProjects.ProgressTaskAbstract.UoM,
                           CIProjects.ProgressTaskAbstract.Progress);
        if (multi.execute()) {
            while (multi.next()) {
                final BigDecimal value = multi.<BigDecimal>getAttribute(CIProjects.ProgressTaskAbstract.Progress);
                final BigDecimal quantity = multi.<BigDecimal>getSelect(sel);
                ret = value.multiply(new BigDecimal(100).setScale(8).divide(quantity,
                                BigDecimal.ROUND_HALF_UP));
            }
        }

        final ProgressSeries pgS = new ProgressChart().getSubTaskProgressSeries(_parameter, _taskInstance,
                        new DateTime().minusDays(1), new DateTime().plusDays(1));
        if (pgS != null && !pgS.isEmpty()) {
            ret = pgS.lastEntry().getValue();
        }
        return ret;
    }

}
