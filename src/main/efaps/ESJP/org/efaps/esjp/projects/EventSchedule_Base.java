/*
 * Copyright 2003 - 2010 The eFaps Team
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
 * Revision:        $Rev: 11050 $
 * Last Changed:    $Date: 2013-11-20 17:25:09 -0500 (mié, 20 nov 2013) $
 * Last Changed By: $Author: jorge.cueva@moxter.net $
 */

package org.efaps.esjp.projects;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Project_Base.java 11050 2013-11-20 22:25:09Z jorge.cueva@moxter.net $
 */
@EFapsUUID("232949c1-26ed-4bc0-859f-512541cfb78b")
@EFapsRevision("$Rev: 11050 $")
public abstract class EventSchedule_Base
    extends AbstractCommon
{
    /**
     * Method top create a new Project.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return empty Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        return new Create() {
            @Override
            protected void add2basicInsert(final Parameter _parameter,
                                           final Insert _insert)
                throws EFapsException
            {
                final String eventDefinition = _parameter.getParameterValue("eventDefinition");
                _insert.add(CIProjects.EventSchedule.DefinitionLinkAbstract, Instance.get(eventDefinition));
                _insert.add(CIProjects.EventSchedule.Status, Status.find(CIProjects.EventScheduleStatus.Open));

                add2EventScheduleCreate(_parameter, _insert);
            }

            @Override
            public void connect(final Parameter _parameter,
                                final Instance _instance)
                throws EFapsException
            {
                final Instance projInst = _parameter.getInstance();

                if (projInst.isValid()) {
                    final Insert relInsert = new Insert(CIProjects.ProjectService2EventSchedule);
                    relInsert.add(CIProjects.ProjectService2EventSchedule.FromLink, projInst);
                    relInsert.add(CIProjects.ProjectService2EventSchedule.ToLink, _instance);
                    relInsert.execute();
                }

                connect2EventScheduleCreate(_parameter, _instance);
            }
        }.execute(_parameter);
    }


    /**
     * Add additional attributes for the EventSchedule.
     *
     * @param _parameter passed from eFaps API
     * @param _insert Insert of project.
     * @throws EFapsException on error.
     */
    protected void add2EventScheduleCreate(final Parameter _parameter,
                                     final Insert _insert)
        throws EFapsException
    {

    }

    /**
     * Add additional relations for the EventSchedule.
     *
     * @param _parameter passed from eFaps API
     * @param _instance of the project created.
     * @throws EFapsException on error.
     */
    protected void connect2EventScheduleCreate(final Parameter _parameter,
                                         final Instance _instance)
        throws EFapsException
    {

    }

    /**
     * Autocomplete for the field used to select a project.
     *
     * @param _parameter Parameter as passed from eFaps
     * @return Return containing map needed for an autocomplete field
     * @throws EFapsException on error
     */
    public Return autoComplete4EventDefinition(final Parameter _parameter)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final Map<?, ?> props =  (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final Map<String, Map<String, String>> sortMap = new TreeMap<String, Map<String, String>>();
        if (input.length() > 0) {
            final String formatStr = props.containsKey("FormatStr") ? (String) props.get("FormatStr") : "%s - %s";

            final Map<Integer, String> types = analyseProperty(_parameter, "Type");
            for (final Entry<Integer, String> entry : types.entrySet()) {
                final Type type = Type.get(entry.getValue());
                final QueryBuilder queryBldr = new QueryBuilder(type);
                queryBldr.addWhereAttrMatchValue(CIERP.EventDefinitionAbstract.Name, input + "*").setIgnoreCase(true);
                queryBldr.addWhereAttrEqValue(CIERP.EventDefinitionAbstract.Active, true);

                final MultiPrintQuery print = queryBldr.getPrint();
                print.addAttribute(CIERP.EventDefinitionAbstract.Name,
                                CIERP.EventDefinitionAbstract.Description);
                print.execute();
                while (print.next()) {
                    final String name = print.<String>getAttribute(CIProjects.ProjectAbstract.Name);
                    final String oid = print.getCurrentInstance().getOid();
                    final String description = print.<String>getAttribute(CIProjects.ProjectAbstract.Description);
                    final Formatter formatter = new Formatter(Context.getThreadContext().getLocale());
                    formatter.format(formatStr, name, description);
                    final String choice = formatter.toString();
                    formatter.close();
                    final Map<String, String> map = new HashMap<String, String>();
                    map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), oid);
                    map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
                    map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), choice);
                    sortMap.put(choice, map);
                }
            }
        }
        final Return retVal = new Return();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        list.addAll(sortMap.values());
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }
}
