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

import java.util.Map;
import java.util.Stack;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;


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
                        while (!parents.empty() && parents.peek().level >= posGrp.level) {
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
                    posGrp.instance = update.getInstance();
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
     * Used as simple chache for Task Objects.
     */
    private class TaskPOs
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
    }
}
