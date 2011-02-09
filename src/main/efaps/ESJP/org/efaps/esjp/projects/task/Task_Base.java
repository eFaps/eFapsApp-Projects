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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
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

                        final BigDecimal left = new BigDecimal(100).setScale(8)
                                        .divide(new BigDecimal(days), BigDecimal.ROUND_HALF_UP)
                                        .multiply(new BigDecimal(d2.getDays()))
                                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                        final BigDecimal right = new BigDecimal(100).setScale(8)
                                        .divide(new BigDecimal(days), BigDecimal.ROUND_HALF_UP)
                                        .multiply(new BigDecimal(d3.getDays()))
                                        .setScale(2, BigDecimal.ROUND_HALF_UP);
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
