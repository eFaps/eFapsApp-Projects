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
 * Revision:        $Rev: 6402 $
 * Last Changed:    $Date: 2011-04-05 10:30:11 -0500 (mar, 05 abr 2011) $
 * Last Changed By: $Author: jan@moxter.net $
 */

package org.efaps.esjp.projects.document;

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.util.DateTimeUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateMidnight;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: WorkOrder_Base.java 6402 2011-04-05 15:30:11Z jan@moxter.net $
 */
@EFapsUUID("2d28dc1c-33a6-4312-973f-8f9bbfefd882")
@EFapsRevision("$Rev: 6402 $")
public abstract class PayRequest_Base
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
        createDoc(_parameter);
        return new Return();
    }

    protected CreatedDoc createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        // Sales-Configuration
        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
        final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(),
                        _parameter.getParameterValue("rateCurrencyId"));

        final Insert insert = new Insert(CIProjects.PayRequest);
        insert.add(CIProjects.PayRequest.Contact, contactid);
        insert.add(CIProjects.PayRequest.Date, date == null
                                        ? DateTimeUtil.normalize(new DateMidnight().toDateTime()) : date);
        insert.add(CIProjects.PayRequest.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CIProjects.PayRequest.Name, _parameter.getParameterValue("name"));
        insert.add(CIProjects.PayRequest.Status, ((Long) Status.find(CIProjects.PayRequestStatus.uuid, "Open")
                                                        .getId()).toString());
        insert.add(CIProjects.PayRequest.Note, _parameter.getParameterValue("note"));
        insert.execute();

        final CreatedDoc createdDoc = new CreatedDoc(insert.getInstance());
        createPositions(_parameter, createdDoc);
        return createdDoc;
    }

    /**
     * Internal Method to create the positions for this Document.
     * @param _parameter    Parameter as passed from eFaps API.
     * @param _calcList     List of Calculators
     * @param _createdDoc   cretaed Document
     * @throws EFapsException on error
     */
    protected void createPositions(final Parameter _parameter,
                                   final CreatedDoc _createdDoc)
        throws EFapsException
    {
     // Sales-Configuration
        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
        final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(),
                        _parameter.getParameterValue("rateCurrencyId"));

        Integer i = 0;
        for (final String desc : _parameter.getParameterValues("description")) {
            final Insert posIns = new Insert(CIProjects.PayRequestPosition);
            posIns.add(CIProjects.PayRequestPosition.PayRequest, _createdDoc.getInstance().getId());
            posIns.add(CIProjects.PayRequestPosition.PositionNumber, i);
            posIns.add(CIProjects.PayRequestPosition.PayTypeId, _parameter.getParameterValues("payType")[i]);
            posIns.add(CIProjects.PayRequestPosition.Description, desc);
            posIns.add(CIProjects.PayRequestPosition.Quantity, _parameter.getParameterValues("quantity")[i]);
            posIns.execute();
            i++;
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
