/*
 * Copyright 2003 - 2016 The eFaps Team
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
 */

package org.efaps.esjp.projects.document;

import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIFormProjects;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("8d8ef20d-8e38-4e39-b2d3-a555959dcaff")
@EFapsApplication("eFapsApp-Projects")
public abstract class WorkOrder_Base
    extends DocumentAbstract
{
    /**
     * Method used to create a new WorkOrder.
     *
     * @param _parameter Parameter as passed from the eFasp API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Create create = new Create()
        {

            @Override
            protected void add2basicInsert(final Parameter _parameter,
                                           final Insert _insert)
                throws EFapsException
            {
                add2basicInsert4create(_parameter, _insert);
            }

            @Override
            public void connect(final Parameter _parameter,
                                final Instance _instance)
                throws EFapsException
            {
                connect4create(_parameter, _instance);
            }
        };
        return create.execute(_parameter);
    }


    /**
     * To allow easy override.
     *
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _instance the instance
     * @throws EFapsException on error
     */
    protected void connect4create(final Parameter _parameter,
                                  final Instance _instance)
        throws EFapsException
    {
        final Instance projectInst = Instance.get(_parameter
                        .getParameterValue(CIFormProjects.Projects_WorkOrderForm.project.name));
        if (projectInst.isValid()) {
            final Insert insert = new Insert(CIProjects.ProjectService2WorkOrder);
            insert.add(CIProjects.ProjectService2WorkOrder.FromLink, projectInst.getId());
            insert.add(CIProjects.ProjectService2WorkOrder.ToLink, _instance.getId());
            insert.execute();
        }
    }

    /**
     * Add additional values to the basic insert, prior to execution.
     * To allow easy override.
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _insert       Insert the values can be added to
     * @throws EFapsException on error
     */
    protected void add2basicInsert4create(final Parameter _parameter,
                                          final Insert _insert)
        throws EFapsException
    {
        final Instance projectInst = Instance.get(_parameter
                        .getParameterValue(CIFormProjects.Projects_WorkOrderForm.project.name));

        if (projectInst.isValid()) {
            final PrintQuery print = new PrintQuery(projectInst);
            print.addAttribute(CIProjects.ProjectAbstract.Contact);
            print.execute();

            _insert.add(CIProjects.WorkOrder.Contact, print.<Long>getAttribute(CIProjects.ProjectAbstract.Contact));
        }
        // Projects-Configuration
        if (SystemConfiguration.get(UUID.fromString("7536a95f-c2bb-4e97-beb1-58ef3e75b80a"))
                        .getAttributeValueAsBoolean("WorkOrder_AutomaticNumbering")) {
            // Projects_WorkOrderSequence
            final NumberGenerator numGen = NumberGenerator.get(
                            UUID.fromString("e56c2b01-4f60-411b-9d36-3291aa736b93"));
            _insert.add(CIProjects.WorkOrder.Name, numGen.getNextVal());
        }
    }


    /**
     * Method returns a javascript to set the values for the contact.
     *
     * @param _parameter Parameter as passed from eFaps
     * @return Return with javascript
     * @throws EFapsException on error
     */
    @Override
    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Instance inst = _parameter.getCallInstance();

        final StringBuilder js = new StringBuilder();
        js.append("<script type=\"text/javascript\">");
        if (inst != null && inst.getType().getUUID().equals(CIProjects.ProjectService.uuid)) {
            final PrintQuery print = new PrintQuery(inst);
            print.addSelect("linkto[Contact].oid", "linkto[Contact].attribute[Name]");
            print.execute();
            final String contactOID = print.<String>getSelect("linkto[Contact].oid");
            final String contactName = print.<String>getSelect("linkto[Contact].attribute[Name]");
            final String contactData = getFieldValue4Contact(Instance.get(contactOID));

            js.append("Wicket.Event.add(window, \"domready\", function(event) {")
                .append(" document.getElementsByName('contact')[0].value='").append(contactOID).append("';")
                .append("document.getElementsByName('contactAutoComplete')[0].value='").append(contactName).append("';")
                .append("document.getElementsByName('contactData')[0].appendChild(document.createTextNode('")
                .append(contactData).append("'));")
                .append(" });");
        }
        js.append("</script>");
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, js.toString());
        return retVal;
    }

}
