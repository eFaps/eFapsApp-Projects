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
package org.efaps.esjp.projects.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ui.html.HtmlTable;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * 
 */
@EFapsUUID("5829d1d9-bfb4-430e-93c4-a6e9467ea663")
@EFapsApplication("eFapsApp-Projects")
public abstract class GantHtmlTable_Base
{
    /**
     * Create a html table that represents the tasks.
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return getGantHtmlTableFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<Instance> instances = new ArrayList<>();
        if (_parameter.getInstance().getType().isKindOf(CIProjects.ProjectAbstract.getType())) {
            final QueryBuilder queryBldr = new QueryBuilder(CIProjects.TaskAbstract);
            queryBldr.addWhereAttrEqValue(CIProjects.TaskAbstract.ProjectAbstractLink,
                            _parameter.getInstance().getId());
            final InstanceQuery query = queryBldr.getQuery();
            instances.addAll(query.execute());
        } else {
            instances.add(_parameter.getInstance());
        }

        final Map<Instance, ATask> tasks = new HashMap<>();
        DateTime projectDate = null;
        DateTime projectDueDate = null;
        DateTime maxDate = null;
        DateTime minDate = null;
        final MultiPrintQuery multi = new MultiPrintQuery(instances);
        final SelectBuilder selDate = new SelectBuilder()
            .linkto(CIProjects.TaskAbstract.ProjectAbstractLink)
            .attribute(CIProjects.ProjectAbstract.Date);
        final SelectBuilder selDue = new SelectBuilder()
            .linkto(CIProjects.TaskAbstract.ProjectAbstractLink)
            .attribute(CIProjects.ProjectAbstract.DueDate);
        final SelectBuilder selPar = new SelectBuilder().linkto(CIProjects.TaskAbstract.ParentTaskAbstractLink).oid();
        multi.addSelect(selDate, selDue, selPar);
        multi.addAttribute(CIProjects.TaskAbstract.DateFrom, CIProjects.TaskAbstract.DateUntil,
                        CIProjects.TaskAbstract.Name, CIProjects.TaskAbstract.Description,
                        CIProjects.TaskAbstract.Order);
        add2MultiPrint(_parameter, multi);
        multi.execute();
        while (multi.next()) {
            final ATask task = getTask(_parameter, multi.getCurrentInstance(),
                            Instance.get(multi.<String>getSelect(selPar)));
            tasks.put(multi.getCurrentInstance(), task);
            final DateTime dateFrom = multi.<DateTime>getAttribute(CIProjects.TaskAbstract.DateFrom);
            final DateTime dateUntil = multi.<DateTime>getAttribute(CIProjects.TaskAbstract.DateUntil);
            task.addAttribute(CIProjects.TaskAbstract.DateFrom.name, dateFrom);
            task.addAttribute(CIProjects.TaskAbstract.DateUntil.name, dateUntil);
            task.addAttribute(CIProjects.TaskAbstract.Name.name,
                            multi.getAttribute(CIProjects.TaskAbstract.Name));
            task.addAttribute(CIProjects.TaskAbstract.Order.name,
                            multi.getAttribute(CIProjects.TaskAbstract.Order));
            task.addAttribute(CIProjects.TaskAbstract.Description.name,
                            multi.getAttribute(CIProjects.TaskAbstract.Description));
            add2Attributes(_parameter, task, multi);
            if (projectDate == null) {
                projectDate = multi.<DateTime>getSelect(selDate);
                projectDueDate = multi.<DateTime>getSelect(selDue);
                minDate = projectDate;
                maxDate = projectDueDate;
            }

            if (dateFrom.isBefore(minDate)) {
                minDate = dateFrom;
            }
            if (dateUntil.isAfter(maxDate)) {
                maxDate = dateUntil;
            }
        }
        final List<ATask> roots = new ArrayList<>();
        for (final Entry<Instance, ATask> entry : tasks.entrySet()) {
            if (entry.getValue().isChild()) {
                tasks.get(entry.getValue().getParentInstance()).add2Children(entry.getValue());
            } else {
                roots.add(entry.getValue());
            }
        }

        final DateTime startDate = minDate.isBefore(projectDate) ? minDate : projectDate;
        final DateTime endDate = maxDate.isAfter(projectDueDate) ? maxDate : projectDueDate;
        DateTime current = startDate;
        final List<DateTime> dates = new ArrayList<>();
        while (current.isBefore(endDate)) {
            dates.add(current);
            current = current.plusDays(1);
        }

        final HtmlTable html = new HtmlTable();
        html.table().tr().th(DBProperties.getProperty("org.efaps.esjp.projects.task.GantHtmlTable.Task"),
                        "border: 1px solid grey;", 0, 2);
        int i = 0;
        int j = 0;
        for (final DateTime date : dates) {
            i++;
            if (date.getDayOfWeek() == 7) {
                j++;
                html.th(DBProperties.getFormatedDBProperty("org.efaps.esjp.projects.task.GantHtmlTable.Week", j),
                                "border: 1px solid grey;", i, 0);
                i = 0;
            }
        }
        if (i > 0) {
            html.th(DBProperties.getFormatedDBProperty("org.efaps.esjp.projects.task.GantHtmlTable.Week", j + 1),
                            "border: 1px solid grey;", i, 0);
        }
        html.trC().tr();
        for (final DateTime date : dates) {
            html.td(date.toString("dd-MMM"));
        }
        html.trC();
        addRows(_parameter, html, roots, dates, 0);
        add2Table(_parameter, html, roots, dates);
        html.tableC();
        ret.put(ReturnValues.SNIPLETT, getStyleSheet(_parameter) + html.toString());
        return ret;
    }

    /**
     * @param _parameter    Parameter as passed by the eFasp API
     * * @return  String
     * @throws EFapsException on error
     */
    protected String getStyleSheet(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder css = new StringBuilder();
        css.append("<style type=\"text/css\">")
            .append(" .taskName {")
            .append("display:block;")
            .append("overflow:hidden;")
            .append("text-overflow:ellipsis;")
            .append("width:200px;")
            .append("}")
            .append(" .level0 {")
            .append("font-weight:bold;")
            .append("}")
            .append(" .level1 {")
            .append("padding-left: 5px;")
            .append("}")
            .append(" .level2 {")
            .append("padding-left: 10px;")
            .append("}")
            .append(" .level3 {")
            .append("padding-left: 15px;")
            .append("}")
            .append(" .contained {")
            .append("background-color:grey;")
            .append("display:block;")
            .append("height:10px")
            .append("}")
            .append(" .contained div {")
            .append("display:none;")
            .append("background-color: #D8D8D8;")
            .append("border: 1px solid;")
            .append("display: none;")
            .append("padding: 2px;")
            .append("position: absolute;")
            .append("}")
            .append(".contained:hover div {")
            .append("display:block;")
            .append("}")
            .append("</style>");
        return css.toString();
    }

    /**
     * @param _parameter    Parameter as passed by the eFasp API
     * @param _html         html to append to
     * @param _tasks        list of tasks
     * @param _dates        list of dates
     * @throws EFapsException on error
     */
    protected void add2Table(final Parameter _parameter,
                             final HtmlTable _html,
                             final List<ATask> _tasks,
                             final List<DateTime> _dates)
        throws EFapsException
    {
        // to be implemented
    }

    /**
     * @param _parameter    Parameter as passed by the eFasp API
     * @param _task         current task
     * @param _multi        query to append to
     * @throws EFapsException on error
     */
    protected void add2Attributes(final Parameter _parameter,
                                final ATask _task,
                                final MultiPrintQuery _multi)
        throws EFapsException
    {
        // to be implemented
    }

    /**
     * @param _parameter    Parameter as passed by the eFasp API
     * @param _multi        query to append to
     * @throws EFapsException on error
     */
    protected void add2MultiPrint(final Parameter _parameter,
                                final MultiPrintQuery _multi)
        throws EFapsException
    {
        // to be implemented
    }

    /**
     * @param _parameter    Parameter as passed by the eFasp API
     * @param _html         html to append to
     * @param _tasks        lis of tasks
     * @param _dates        list of dates
     * @param _count        count
     * @return new count
     * @throws EFapsException on error
     */
    protected int addRows(final Parameter _parameter,
                          final HtmlTable _html,
                          final List<ATask> _tasks,
                          final List<DateTime> _dates,
                          final int _count)
        throws EFapsException
    {
        Collections.sort(_tasks, getComparator());
        int i = _count;
        for (final ATask task : _tasks) {
            final String color = i % 2 == 0 ? "lightGray" : "lightBlue";
            _html.tr();
            final StringBuilder tag = new StringBuilder();
            tag.append("<span class=\"taskName level")
                .append(task.getLevel())
                .append("\"")
                .append("title=\"")
                .append(StringEscapeUtils.escapeHtml4(task
                                .<String>getAttributeValue(CIProjects.TaskAbstract.Name.name)))
                .append(" - ")
                .append(StringEscapeUtils.escapeHtml4(task
                                .<String>getAttributeValue(CIProjects.TaskAbstract.Description.name)))
                .append("\"")
                .append(">")
                .append(StringEscapeUtils.escapeHtml4(task
                                .<String>getAttributeValue(CIProjects.TaskAbstract.Name.name)))
                .append(" - ")
                .append(StringEscapeUtils.escapeHtml4(task
                                .<String>getAttributeValue(CIProjects.TaskAbstract.Description.name)))
                .append("</span>");
            _html.td(tag.toString(), "background-color:" + color);
            final DateTime dateFrom = task.getAttributeValue(CIProjects.TaskAbstract.DateFrom.name);
            final DateTime dateUntil = task.getAttributeValue(CIProjects.TaskAbstract.DateUntil.name);
            final Interval intervale = new Interval(dateFrom, dateUntil.isAfter(dateFrom) ? dateUntil.plusSeconds(1)
                            : dateFrom.plusSeconds(1));
            for (final DateTime date : _dates) {
                _html.td(getCellValue(_parameter, task, intervale.contains(date)), intervale.contains(date)
                                ? "padding-right:0;padding-left:0;background-color:" + color
                                                : "background-color:" + color);
            }
            _html.trC();
            i = addRows(_parameter, _html, task.getChildren(), _dates, i + 1);
        }
        return i;
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _task         current task
     * @param _contained    is it contained
     * @return  STring
     * @throws EFapsException on error
     */
    protected String getCellValue(final Parameter _parameter,
                                  final ATask _task,
                                  final boolean _contained)
        throws EFapsException
    {
        final StringBuilder html = new StringBuilder();
        if (_contained) {
            html.append("<span class=\"contained\">").append(getToolTip(_parameter, _task)).append("</span)");
        }
        return html.toString();
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _task         current task
     * @return  String
     * @throws EFapsException on error
     */
    protected String getToolTip(final Parameter _parameter,
                                final ATask _task)
        throws EFapsException
    {
        final StringBuilder html = new StringBuilder();
        html.append("<div>")
            .append(_task.<String>getAttributeValue(CIProjects.TaskAbstract.Name.name))
            .append(" - ")
            .append(_task.<String>getAttributeValue(CIProjects.TaskAbstract.Description.name))
            .append("</div>");
        return html.toString();
    }

    /**
     * @return new Comparator
     * @throws EFapsException on error
     */
    protected Comparator<ATask> getComparator()
        throws EFapsException
    {
        final Comparator<ATask> ret = new Comparator<ATask>() {

            @Override
            public int compare(final ATask _o1,
                               final ATask _o2)
            {
                return _o1.<Integer>getAttributeValue(CIProjects.TaskAbstract.Order.name)
                    .compareTo(_o2.<Integer>getAttributeValue(CIProjects.TaskAbstract.Order.name));
            }
        };
        return ret;
    }


    /**
     * @param _parameter    Paraamter as passed by the eFaps APIPA
     * @param _instance     instanc of the task
     * @param _parentTaskInstance parent instance ot this task
     * @return new ATask
     */
    protected ATask getTask(final Parameter _parameter,
                            final Instance _instance,
                            final Instance _parentTaskInstance)
    {
        return new ATask(_instance, _parentTaskInstance);
    }

    /**
     * A Task Instance.
     */
    public class ATask
    {

        /**
         *
         */
        private final Instance instance;

        /**
         *
         */
        private Instance parentInstance;
        /**
         *
         */
        private final List<GantHtmlTable_Base.ATask> children = new ArrayList<>();
        /**
         *
         */
        private ATask parent;
        /**
         *
         */
        private final Map<String, Object> attributes = new HashMap<>();
        /**
         * @param _instance         instance of this task
         * @param _parentTaskInstance parent instance
         */
        public ATask(final Instance _instance,
                     final Instance _parentTaskInstance)
        {
            this.instance = _instance;
            if (_parentTaskInstance.isValid()) {
                this.parentInstance = _parentTaskInstance;
            }
        }

        /**
         * @param _attrName name of the attribute
         * @param _value    value
         */
        public void addAttribute(final String _attrName,
                                 final Object _value)
        {
            this.attributes.put(_attrName, _value);
        }

        /**
         * @param <T> class
         * @param _attrName name of the attribute
         * @return value of the attribute
         */
        @SuppressWarnings("unchecked")
        public <T> T getAttributeValue(final String _attrName)
        {
            return (T) this.attributes.get(_attrName);
        }

        /**
         * Getter method for the instance variable {@link #children}.
         *
         * @return value of instance variable {@link #children}
         */
        public List<GantHtmlTable_Base.ATask> getChildren()
        {
            return this.children;
        }

        /**
         * @param _task task
         */
        public void add2Children(final ATask _task)
        {
            this.children.add(_task);
            _task.setParent(this);
        }

        /**
         * @param _aTask task
         */
        public void setParent(final ATask _aTask)
        {
            this.parent = _aTask;
        }

        /**
         * @return true if child
         */
        public boolean isChild()
        {
            return this.parentInstance != null;
        }

        /**
         * Getter method for the instance variable {@link #parentInstance}.
         *
         * @return value of instance variable {@link #parentInstance}
         */
        public Instance getParentInstance()
        {
            return this.parentInstance;
        }

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
         * Getter method for the instance variable {@link #parent}.
         *
         * @return value of instance variable {@link #parent}
         */
        public ATask getParent()
        {
            return this.parent;
        }


        /**
         * @return the level of this task
         */
        public int getLevel()
        {
            int ret = 0;
            ATask parentTmp = this.parent;
            while (parentTmp != null) {
                ret++;
                parentTmp = parentTmp.getParent();
            }

            return ret;
        }
    }
}
