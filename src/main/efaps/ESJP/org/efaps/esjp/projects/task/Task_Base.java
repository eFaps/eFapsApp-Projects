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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field.Display;
import org.efaps.db.Context;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.Days;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("17c3bf25-b929-4825-a261-2f738a77abaf")
@EFapsRevision("$Rev$")
public abstract class Task_Base
{
    /**
     * Key for the map to be stored in the request.
     */
    public static final String TASKGANTREUQESTKEY = "org.efaps.esjp.projects.task.Task.requestMap4Task";

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing a snipplet
     * @throws EFapsException on error
     */
    public Return getValidateFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder html = new StringBuilder();
        final List<TaskPOs> tasks = getTaskTree(_parameter);
        validate4ProjectDates(_parameter, tasks, html);
        validate4TaskHierarchy(_parameter, tasks, html);
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    protected void validate4ProjectDates(final Parameter _parameter,
                                         final List<TaskPOs> _tasks,
                                         final StringBuilder _html)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        print.addAttribute(CIProjects.ProjectAbstract.Date, CIProjects.ProjectAbstract.DueDate);
        print.execute();

        final DateTime date = print.<DateTime>getAttribute(CIProjects.ProjectAbstract.Date);
        final DateTime dueDate = print.<DateTime>getAttribute(CIProjects.ProjectAbstract.DueDate);
        for (final TaskPOs task : _tasks) {
            _html.append(validate4ProjectDates(_parameter, task, date, dueDate));
        }
    }

    protected StringBuilder validate4ProjectDates(final Parameter _parameter,
                                                  final TaskPOs _task,
                                                  final DateTime _date,
                                                  final DateTime _dueDate)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        final DateTime dateFrom = _task.<DateTime>getAttrValue(CIProjects.TaskAbstract.DateFrom.name);
        final DateTime dateUntil = _task.<DateTime>getAttrValue(CIProjects.TaskAbstract.DateUntil.name);
        if (dateFrom.isBefore(_date)) {
            ret.append(DBProperties.getFormatedDBProperty(
                            "org.efaps.esjp.projects.task.Task.validate4ProjectDates.before",
                            _task.<Object>getAttrValue(CIProjects.TaskAbstract.Name.name),
                            _task.<String>getAttrValue(CIProjects.TaskAbstract.Description.name)))
                .append("<br/>");
        }
        if (dateUntil.isAfter(_dueDate)) {
            ret.append(DBProperties.getFormatedDBProperty(
                            "org.efaps.esjp.projects.task.Task.validate4ProjectDates.after",
                            _task.<Object>getAttrValue(CIProjects.TaskAbstract.Name.name),
                            _task.<String>getAttrValue(CIProjects.TaskAbstract.Description.name)))
                .append("<br/>");
        }
        for (final TaskPOs task : _task.getChildren()) {
            ret.append(validate4ProjectDates(_parameter, task, _date, _dueDate));
        }
        return ret;
    }


    protected void validate4TaskHierarchy(final Parameter _parameter,
                                          final List<TaskPOs> _tasks,
                                          final StringBuilder _html)
        throws EFapsException
    {
        for (final TaskPOs task : _tasks) {
            for (final TaskPOs child : task.getChildren()) {
                _html.append(validate4TaskHierarchy(_parameter, task, child));
            }
        }
    }

    protected StringBuilder validate4TaskHierarchy(final Parameter _parameter,
                                                   final TaskPOs _parentTask,
                                                   final TaskPOs _task)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        final DateTime dateFrom = _task.<DateTime>getAttrValue(CIProjects.TaskAbstract.DateFrom.name);
        final DateTime dateUntil = _task.<DateTime>getAttrValue(CIProjects.TaskAbstract.DateUntil.name);
        final DateTime parentDateFrom = _parentTask.<DateTime>getAttrValue(CIProjects.TaskAbstract.DateFrom.name);
        final DateTime parentDateUntil = _parentTask.<DateTime>getAttrValue(CIProjects.TaskAbstract.DateUntil.name);
        if (dateFrom.isBefore(parentDateFrom)) {
            ret.append(DBProperties.getFormatedDBProperty(
                            "org.efaps.esjp.projects.task.Task.validate4TaskHierarchy.datebefore",
                            _task.<Object>getAttrValue(CIProjects.TaskAbstract.Name.name),
                            _task.<String>getAttrValue(CIProjects.TaskAbstract.Description.name),
                            _parentTask.<String>getAttrValue(CIProjects.TaskAbstract.Name.name),
                            _parentTask.<String>getAttrValue(CIProjects.TaskAbstract.Description.name)))
                .append("<br/>");
        }
        if (dateUntil.isAfter(parentDateUntil)) {
            ret.append(DBProperties.getFormatedDBProperty(
                            "org.efaps.esjp.projects.task.Task.validate4TaskHierarchy.dateafter",
                            _task.<Object>getAttrValue(CIProjects.TaskAbstract.Name.name),
                            _task.<String>getAttrValue(CIProjects.TaskAbstract.Description.name),
                            _parentTask.<String>getAttrValue(CIProjects.TaskAbstract.Name.name),
                            _parentTask.<String>getAttrValue(CIProjects.TaskAbstract.Description.name)))
                .append("<br/>");
        }
        for (final TaskPOs task : _task.getChildren()) {
            ret.append(validate4TaskHierarchy(_parameter, _task, task));
        }
        return ret;
    }


    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return list of root task
     * @throws EFapsException on error
     */
    protected List<TaskPOs> getTaskTree(final Parameter _parameter)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CIProjects.TaskAbstract);
        queryBldr.addWhereAttrEqValue(CIProjects.TaskAbstract.ProjectAbstractLink, _parameter.getInstance().getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder sel = new SelectBuilder().linkto(CIProjects.TaskAbstract.ParentTaskAbstractLink).oid();
        multi.addSelect(sel);
        multi.addAttribute(CIProjects.TaskAbstract.DateFrom, CIProjects.TaskAbstract.DateUntil,
                        CIProjects.TaskAbstract.Description, CIProjects.TaskAbstract.Name,
                        CIProjects.TaskAbstract.Note, CIProjects.TaskAbstract.Quantity,
                        CIProjects.TaskAbstract.Weight, CIProjects.TaskAbstract.UoM);
        multi.execute();
        final Map<Instance, TaskPOs> tmp = new HashMap<Instance, TaskPOs>();

        while (multi.next()) {
            final TaskPOs task = new TaskPOs(multi.getCurrentInstance(), 0);
            tmp.put(task.getInstance(), task);
            task.addAttribute(CIProjects.TaskAbstract.DateFrom.name,
                              multi.getAttribute(CIProjects.TaskAbstract.DateFrom));
            task.addAttribute(CIProjects.TaskAbstract.DateUntil.name,
                            multi.getAttribute(CIProjects.TaskAbstract.DateUntil));
            task.addAttribute(CIProjects.TaskAbstract.Description.name,
                            multi.getAttribute(CIProjects.TaskAbstract.Description));
            task.addAttribute(CIProjects.TaskAbstract.Name.name,
                            multi.getAttribute(CIProjects.TaskAbstract.Name));
            task.addAttribute(CIProjects.TaskAbstract.Note.name,
                            multi.getAttribute(CIProjects.TaskAbstract.Note));
            task.addAttribute(CIProjects.TaskAbstract.Quantity.name,
                            multi.getAttribute(CIProjects.TaskAbstract.Quantity));
            task.addAttribute(CIProjects.TaskAbstract.Weight.name,
                            multi.getAttribute(CIProjects.TaskAbstract.Weight));
            task.addAttribute(CIProjects.TaskAbstract.Weight.name,
                            multi.getAttribute(CIProjects.TaskAbstract.Weight));
            task.setParentInstance(Instance.get(multi.<String>getSelect(sel)));
        }

        final List<TaskPOs> roots = new ArrayList<TaskPOs>();
        for (final Entry<Instance, TaskPOs> entry : tmp.entrySet()) {
            if (entry.getValue().isChild()) {
                entry.getValue().setParent(tmp.get(entry.getValue().getParentInstance()));
            } else {
                roots.add(entry.getValue());
            }
        }
        return roots;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing true if access is granted
     * @throws EFapsException on error
     */
    public Return accessCheck4TaskEditCreate(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        final AbstractCommand cmd = (AbstractCommand) _parameter.get(ParameterValues.UIOBJECT);
        final TargetMode mode = cmd.getTargetMode();

        final QueryBuilder queryBldr = new QueryBuilder(CIProjects.TaskAbstract);
        queryBldr.addWhereAttrEqValue(CIProjects.TaskAbstract.ProjectAbstractLink, _parameter.getInstance().getId());
        final InstanceQuery query = queryBldr.getQuery();
        query.setLimit(1);
        if ((query.execute().isEmpty() && TargetMode.CREATE.equals(mode))
                        || (!query.execute().isEmpty() && TargetMode.EDIT.equals(mode))) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * Create from an StructurBrowser.
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String[] names = _parameter.getParameterValues("name");
        final String[] descriptions = _parameter.getParameterValues("description");
        final Instance projectInst = _parameter.getInstance();
        final String[] allowChilds = _parameter.getParameterValues(EFapsKey.STRUCBRWSR_ALLOWSCHILDS.getKey());
        final String[] levels = _parameter.getParameterValues(EFapsKey.STRUCBRWSR_LEVEL.getKey());
        final String[] dateFroms = _parameter.getParameterValues("dateFrom_eFapsDate");
        final String[] dateUntils =  _parameter.getParameterValues("dateUntil_eFapsDate");

        final Stack<TaskPOs> parents = new Stack<TaskPOs>();

        for (int i = 0; i < allowChilds.length; i++) {
            final int level = Integer.parseInt(levels[i]);
            // folders
            final Insert insert = new Insert(CIProjects.TaskScheduled);
            boolean parent;
            if ("true".equalsIgnoreCase(allowChilds[i])) {
                parent = true;
                if (level != 1) {
                    insert.add(CIProjects.TaskAbstract.ParentTaskAbstractLink, parents.peek().instance.getId());
                }
            } else {
                parent = false;
                insert.add(CIProjects.TaskAbstract.ParentTaskAbstractLink, parents.peek().instance.getId());
            }
            insert.add(CIProjects.TaskAbstract.ProjectAbstractLink, projectInst.getId());
            insert.add(CIProjects.TaskAbstract.Name, names[i]);
            insert.add(CIProjects.TaskAbstract.Description, descriptions[i]);
            insert.add(CIProjects.TaskAbstract.DateFrom, DateUtil.getDateFromParameter(dateFroms[i]));
            insert.add(CIProjects.TaskAbstract.DateUntil, DateUtil.getDateFromParameter(dateUntils[i]));
            insert.add(CIProjects.TaskAbstract.StatusAbstract,
                            Status.find(CIProjects.TaskScheduledStatus.uuid, "Open").getId());
            insert.execute();

            if (parent) {
                final TaskPOs posGrp = new TaskPOs(insert.getInstance(), level);
                if (parents.isEmpty()) {
                    parents.push(posGrp);
                } else {
                    if (parents.peek().level < posGrp.level) {
                        parents.push(posGrp);
                    } else {
                        while (!parents.empty() && parents.peek().level >= posGrp.level) {
                            parents.pop();
                        }
                        parents.push(posGrp);
                    }
                }
            }
        }
        return ret;
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @return  Return
     * @throws EFapsException on error
     */
    public Return createSubTask(final Parameter _parameter)
        throws EFapsException
    {
        final Create create = new Create() {

            @Override
            protected void add2basicInsert(final Parameter _parameter,
                                           final Insert _insert)
                throws EFapsException
            {
                final PrintQuery print = new PrintQuery(_parameter.getInstance());
                print.addAttribute(CIProjects.TaskAbstract.ProjectAbstractLink);
                print.execute();
                final Long projId = print.<Long>getAttribute(CIProjects.TaskAbstract.ProjectAbstractLink);

                _insert.add(CIProjects.TaskAbstract.ProjectAbstractLink, projId);
            }
        };
        return create.execute(_parameter);
    }

    /**
     * Edit from an StructurBrowser.
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        @SuppressWarnings("unchecked")
        final Map<String, String> oidMap = (Map<String, String>) _parameter.get(ParameterValues.OIDMAP4UI);
        final String[] rowKeys = _parameter.getParameterValues(EFapsKey.TABLEROW_NAME.getKey());
        final String[] names = _parameter.getParameterValues("name");
        final String[] descriptions = _parameter.getParameterValues("description");
        final String[] dateFroms = _parameter.getParameterValues("dateFrom_eFapsDate");
        final String[] dateUntils =  _parameter.getParameterValues("dateUntil_eFapsDate");
        final Instance projectInst = _parameter.getInstance();
        final String[] allowChilds = _parameter.getParameterValues(EFapsKey.STRUCBRWSR_ALLOWSCHILDS.getKey());
        final String[] levels = _parameter.getParameterValues(EFapsKey.STRUCBRWSR_LEVEL.getKey());

        final Stack<TaskPOs> parents = new Stack<TaskPOs>();
        if (rowKeys != null) {
            for (int i = 0; i < rowKeys.length; i++) {
                final QueryBuilder queryBldr = new QueryBuilder(CIProjects.TaskAbstract);
                queryBldr.addWhereAttrEqValue(CIProjects.TaskAbstract.ProjectAbstractLink, projectInst.getId());
                final InstanceQuery query = queryBldr.getQuery();
                query.execute();
                while (query.next()) {
                    if (oidMap.containsKey(query.getCurrentValue().getOid())) {
                        final Delete del = new Delete(query.getCurrentValue());
                        del.execute();
                    }
                }

                //Update o. insert the GroupPosition
                boolean parent = true;
                Update update;
                final int level = Integer.parseInt(levels[i]);
                final TaskPOs posGrp = new TaskPOs(null, level);

                if (oidMap.get(rowKeys[i]) != null) {
                    update = new Update(oidMap.get(rowKeys[i]));
                    parent = "true".equalsIgnoreCase(allowChilds[i]);
                } else {
                    update = new Insert(CIProjects.TaskScheduled);
                    if (level == 1) {
                        parent = false;
                    } else {
                        while (!parents.empty() && parents.peek().getLevel() >= posGrp.getLevel()) {
                            parents.pop();
                        }
                        update.add(CIProjects.TaskAbstract.ParentTaskAbstractLink, parents.peek().instance.getId());
                    }
                    update.add(CIProjects.TaskAbstract.ProjectAbstractLink, projectInst.getId());
                    update.add(CIProjects.TaskAbstract.StatusAbstract,
                                    Status.find(CIProjects.TaskScheduledStatus.uuid, "Open").getId());
                }
                update.add(CIProjects.TaskAbstract.Name, names[i]);
                update.add(CIProjects.TaskAbstract.Description, descriptions[i]);
                update.add(CIProjects.TaskAbstract.DateFrom, DateUtil.getDateFromParameter(dateFroms[i]));
                update.add(CIProjects.TaskAbstract.DateUntil, DateUtil.getDateFromParameter(dateUntils[i]));
                update.execute();

                if (parent) {
                    posGrp.setInstance(update.getInstance());
                    parents.push(posGrp);
                }
            }
        }
        return ret;
    }

    /**
     * Before deletion the subtask will be deleted.
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return deletePreTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Instance delInst = _parameter.getInstance();
        final QueryBuilder queryBldr = new QueryBuilder(CIProjects.TaskAbstract);
        queryBldr.addWhereAttrEqValue(CIProjects.TaskAbstract.ParentTaskAbstractLink, delInst.getId());
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        while (query.next()) {
            final Delete del = new Delete(query.getCurrentValue());
            del.execute();
        }
        return new Return();
    }


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
                            .getRequestAttribute(Task_Base.TASKGANTREUQESTKEY);
            if (values == null || (values != null && !values.containsKey(_parameter.getInstance()))) {
                values = new HashMap<Instance, String>();
                Context.getThreadContext().setRequestAttribute(Task_Base.TASKGANTREUQESTKEY, values);
                @SuppressWarnings("unchecked")
                final List<Instance> instances = (List<Instance>) _parameter
                                .get(ParameterValues.REQUEST_INSTANCES);
                if (instances != null) {
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
                    while (multi.next()) {
                        final DateTime date = multi.<DateTime>getSelect(selDate);
                        final DateTime due = multi.<DateTime>getSelect(selDue);
                        final DateTime from = multi.<DateTime>getAttribute(CIProjects.TaskAbstract.DateFrom);
                        final DateTime until = multi.<DateTime>getAttribute(CIProjects.TaskAbstract.DateUntil);
                        final Days d = Days.daysBetween(date, due);
                        final int days = d.getDays();
                        final Days d2 = Days.daysBetween(date, from);
                        final Days d3 = Days.daysBetween(until, due);
                        final Days d4 = Days.daysBetween(date, until);
                        final BigDecimal left;
                        final BigDecimal right;
                        if (days == 0) {
                            left = BigDecimal.ZERO;
                            right = BigDecimal.ZERO;
                        } else {
                            left = new BigDecimal(100).setScale(8)
                                .divide(new BigDecimal(days), BigDecimal.ROUND_HALF_UP)
                                .multiply(new BigDecimal(d2.getDays()))
                                .setScale(2, BigDecimal.ROUND_HALF_UP);
                            right = new BigDecimal(100).setScale(8)
                                .divide(new BigDecimal(days), BigDecimal.ROUND_HALF_UP)
                                .multiply(new BigDecimal(d3.getDays()))
                                .setScale(2, BigDecimal.ROUND_HALF_UP);
                        }
                        final StringBuilder html4value = new StringBuilder();
                        // the number of days to the left
                        html4value.append("<span style=\"padding-right:2px; float: left; width:").append(left)
                            .append("%; text-align: right;\">")
                            .append("<span style=\"padding-right: 2px;\">").append(d2.getDays()).append("</span>")
                            .append("</span>");
                        // the number of days to the right
                        html4value.append("<span style=\"float: right; width:").append(right).append("%;\">")
                            .append("<span style=\"padding-left: 2px;\">").append(d4.getDays()).append("</span>")
                            .append("</span>");
                        // the gant bar
                        html4value.append("<div style=\"background-color: grey; margin-left:").append(left)
                            .append("%; margin-right:").append(right)
                            .append("%; height: 14px;border: 1px solid black;\">");
                        // the inner bar for precentage advance
//                        final BigDecimal percentage = new BigDecimal(50);
//                        html4value.append("<div style=\"color:white;text-align:right;background-color:black;")
//                            .append("position:relative; top: 2px; height: 10px;width:").append(percentage)
//                            .append("%;\">")
//                            .append("<span style=\"position: relative; top: -3px;\">")
//                            .append(percentage).append("</span>")
//                            .append("</div>");

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
     * Used as simple chache for Task Objects.
     */
    public class TaskPOs
    {
        /**
         * Instance of this task.
         */
        private Instance instance;

        /**
         * Level of this Task.
         */
        private final int level;

        /**
         * Parent for this task.
         */
        private Instance parentInst = null;

        /**
         * Mapping of attributes to values.
         */
        private final Map<String, Object> attr2value = new HashMap<String, Object>();

        /**
         * The parent task.
         */
        private TaskPOs parent;


        /**
         * Children of this Task.
         */
        private final List<TaskPOs> children  = new ArrayList<TaskPOs>();


        /**
         * @param _instance Instance
         * @param _level    Level
         */
        public TaskPOs(final Instance _instance,
                       final int _level)
        {
            this.instance = _instance;
            this.level = _level;
        }

        /**
         * @param _taskPOs task to set as parent
         */
        protected void setParent(final TaskPOs _taskPOs)
        {
            this.parent = _taskPOs;
            _taskPOs.addChild(this);
        }

        /**
         * @param _taskPOs task to add to the children
         */
        protected void addChild(final TaskPOs _taskPOs)
        {
            this.children.add(_taskPOs);
        }

        /**
         * Getter method for the instance variable {@link #children}.
         *
         * @return value of instance variable {@link #children}
         */
        protected List<TaskPOs> getChildren()
        {
            return this.children;
        }

        /**
         * @param _instance parent instance
         */
        protected void setParentInstance(final Instance _instance)
        {
            if (_instance.isValid()) {
                this.parentInst = _instance;
            }
        }

        /**
         * @param _attrName Name of the attribute
         * @param _value    value for the attribute
         */
        protected void addAttribute(final String _attrName,
                                    final Object _value)
        {
            this.attr2value.put(_attrName, _value);
        }

        /**
         * @param <T> type to cast to
         * @param _attrName name of the attribute
         * @return value casted to <T>
         */
        @SuppressWarnings("unchecked")
        protected <T> T getAttrValue(final String _attrName)
        {
            return (T) this.attr2value.get(_attrName);
        }

        /**
         * @return true if Task has parent
         */
        protected boolean isChild()
        {
            return this.parentInst != null;
        }

        /**
         * Getter method for the instance variable {@link #parent}.
         *
         * @return value of instance variable {@link #parent}
         */
        protected TaskPOs getParent()
        {
            return this.parent;
        }

        /**
         * Getter method for the instance variable {@link #parent}.
         *
         * @return value of instance variable {@link #parent}
         */
        protected Instance getParentInstance()
        {
            return this.parentInst;
        }

        /**
         * Getter method for the instance variable {@link #level}.
         *
         * @return value of instance variable {@link #level}
         */
        protected int getLevel()
        {
            return this.level;
        }

        /**
         * Getter method for the instance variable {@link #instance}.
         *
         * @return value of instance variable {@link #instance}
         */
        protected Instance getInstance()
        {
            return this.instance;
        }

        /**
         * Setter method for instance variable {@link #instance}.
         *
         * @param _instance value for instance variable {@link #instance}
         */

        protected void setInstance(final Instance _instance)
        {
            this.instance = _instance;
        }
    }
}
