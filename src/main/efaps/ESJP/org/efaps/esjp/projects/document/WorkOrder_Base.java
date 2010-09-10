/*
 * Copyright 2003 - 2009 The eFaps Team
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

package org.efaps.esjp.projects.document;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("8d8ef20d-8e38-4e39-b2d3-a555959dcaff")
@EFapsRevision("$Rev$")
public class WorkOrder_Base
    extends DocumentAbstract
{
    /**
     * Method used to create a new ServiceRequest.
     *
     * @param _parameter Parameter as passed from the eFasp API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final String date = _parameter.getParameterValue("date");

        final Insert insert = new Insert(CIProjects.WorkOrder);
        insert.add(CIProjects.WorkOrder.Name, _parameter.getParameterValue("name"));
        insert.add(CIProjects.WorkOrder.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CIProjects.WorkOrder.Contact, contactid);
        insert.add(CIProjects.WorkOrder.Date, date);
        insert.add(CIProjects.WorkOrder.Status, Status.find(CIProjects.WorkOrderStatus.uuid, "Open").getId());
        insert.execute();
        return new Return();
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
