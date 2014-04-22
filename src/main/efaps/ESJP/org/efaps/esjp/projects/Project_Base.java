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
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.projects;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.user.Group;
import org.efaps.admin.user.Role;
import org.efaps.ci.CIAdminUser;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.accounting.transaction.Transaction_Base;
import org.efaps.esjp.ci.CIAccounting;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("e0c8e7d5-ca9c-46b3-967e-8bd307b17c93")
@EFapsRevision("$Rev$")
public abstract class Project_Base
    extends CommonDocument
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
                final String contactOid = _parameter.getParameterValue("contact");
                _insert.add(CIProjects.ProjectService.Contact, Instance.get(contactOid).getId());
                _insert.add(CIProjects.ProjectService.Status,
                                Status.find(CIProjects.ProjectServiceStatus.uuid, "Open").getId());

                add2ProjectCreate(_parameter, _insert);
            }

            @Override
            public void connect(final Parameter _parameter,
                                final Instance _instance)
                throws EFapsException
            {
                final Instance callInst = _parameter.getCallInstance();
                // if the call instance is a service request the status can be set to
                // the next,
                // and connect to the service
                if (callInst != null && callInst.getType().equals(CIProjects.ServiceRequest.getType())) {
                    final Update update = new Update(callInst);
                    update.add(CIProjects.ServiceRequest.Status,
                                    Status.find(CIProjects.ServiceRequestStatus.uuid, "Accepted").getId());
                    update.execute();

                    final Insert relInsert = new Insert(CIProjects.ProjectService2Request);
                    relInsert.add(CIProjects.ProjectService2Request.FromLink, _instance);
                    relInsert.add(CIProjects.ProjectService2Request.ToLink, callInst.getId());
                    relInsert.execute();
                }

                connect2ProjectCreate(_parameter, _instance);
            }
        }.execute(_parameter);
    }

    /**
     * Add additional attributes for the project.
     *
     * @param _parameter passed from eFaps API
     * @param _insert Insert of project.
     * @throws EFapsException on error.
     */
    protected void add2ProjectCreate(final Parameter _parameter,
                                     final Insert _insert)
        throws EFapsException
    {

    }

    /**
     * Add additional relations for the project.
     *
     * @param _parameter passed from eFaps API
     * @param _instance of the project created.
     * @throws EFapsException on error.
     */
    protected void connect2ProjectCreate(final Parameter _parameter,
                                         final Instance _instance)
        throws EFapsException
    {

    }

    /**
     * Autocomplete for the field used to select a contact.
     *
     * @param _parameter Parameter as passed from eFaps
     * @return Return containing map needed for an autocomplete field
     * @throws EFapsException on error
     */
    public Return autoComplete4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final Contacts contact = new Contacts();
        return contact.autoComplete4Contact(_parameter);
    }

    /**
     * Autocomplete for the field used to select a project.
     *
     * @param _parameter Parameter as passed from eFaps
     * @return Return containing map needed for an autocomplete field
     * @throws EFapsException on error
     */
    public Return autoComplete4Project(final Parameter _parameter)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final Map<?, ?> props =  (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final Map<String, Map<String, String>> sortMap = new TreeMap<String, Map<String, String>>();
        if (input.length() > 0) {
            final String formatStr = props.containsKey("FormatStr") ? (String) props.get("FormatStr") : "%s - %s - %s";
            final QueryBuilder queryBldr = new QueryBuilder(CIProjects.ProjectAbstract);
            queryBldr.addWhereAttrMatchValue(CIProjects.ProjectAbstract.Name, input + "*").setIgnoreCase(true);
            if (props.containsKey("StatusGroup")) {
                final Status status = Status.find((String) props.get("StatusGroup"), (String) props.get("Status"));
                if (status != null) {
                    queryBldr.addWhereAttrEqValue(CIProjects.ProjectAbstract.StatusAbstract, status.getId());
                }
            }
            final MultiPrintQuery print = queryBldr.getPrint();
            print.addAttribute(CIProjects.ProjectAbstract.OID, CIProjects.ProjectAbstract.Name,
                            CIProjects.ProjectAbstract.Description);
            final SelectBuilder selContactName = SelectBuilder.get()
                                        .linkto(CIProjects.ProjectAbstract.Contact)
                                            .attribute(CIContacts.Contact.Name);
                                                print.addSelect(selContactName);
            print.addSelect(selContactName);
            print.execute();
            while (print.next()) {
                final String name = print.<String>getAttribute(CIProjects.ProjectAbstract.Name);
                final String oid = print.<String>getAttribute(CIProjects.ProjectAbstract.OID);
                final String description = print.<String>getAttribute(CIProjects.ProjectAbstract.Description);
                final String contactName = print.<String>getSelect(selContactName);
                final Formatter formatter = new Formatter(Context.getThreadContext().getLocale());
                formatter.format(formatStr, name, description, contactName);
                final String choice = formatter.toString();
                formatter.close();
                final Map<String, String> map = new HashMap<String, String>();
                map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), oid);
                map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
                map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), choice);
                sortMap.put(choice, map);
            }
        }
        final Return retVal = new Return();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        list.addAll(sortMap.values());
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Method to update the fields for the contact.
     *
     * @param _parameter Parameter as passed from eFaps
     * @return Return containing map needed to update the fields
     * @throws EFapsException on error
     */
    public Return updateFields4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final Contacts contacts = new Contacts() {
            @Override
            public String getFieldValue4Contact(final Instance _instance)
                throws EFapsException
            {
                return Project_Base.this.getFieldValue4Contact(_instance);
            }
        };
        return contacts.updateFields4Contact(_parameter);
    }


    public Return updateField4Project(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        final Instance instance = Instance.get(_parameter.getParameterValue("project"));
        final String contactField = getProperty(_parameter, "ContactField");
        final String contactData = getProperty(_parameter, "ContactData");
        if (contactField != null || contactData != null) {
            final PrintQuery print = new PrintQuery(instance);
            final SelectBuilder contInst = SelectBuilder.get().linkto(CIProjects.ProjectAbstract.Contact).instance();
            final SelectBuilder contName = SelectBuilder.get().linkto(CIProjects.ProjectAbstract.Contact)
                            .attribute(CIContacts.Contact.Name);
            print.addSelect(contInst, contName);
            print.executeWithoutAccessCheck();

            final Instance contactInst = print.<Instance>getSelect(contInst);
            if (contactData != null) {
                final Contacts contacts = new Contacts();
                final String data = contacts.getFieldValue4Contact(contactInst);
                map.put(contactData, data);
            }
            if (contactField != null) {
                map.put(contactField + "AutoComplete", print.<String>getSelect(contName));
                map.put(contactField, new String[] {contactInst.getOid(), print.<String>getSelect(contName)});
            }
        }
        list.add(map);
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }


    /**
     * Method to get the value for the field directly under the Contact.
     *
     * @param _instance Instacne of the contact
     * @return String for the field
     * @throws EFapsException on error
     */
    protected String getFieldValue4Contact(final Instance _instance)
        throws EFapsException
    {
        final Contacts contacts = new Contacts();
        return contacts.getFieldValue4Contact(_instance);
    }

    /**
     * Method returns a javacript to set the values for the contact.
     *
     * @param _parameter Parameter as passed from eFaps
     * @return Return with javascript
     * @throws EFapsException on error
     */
    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Instance inst = _parameter.getCallInstance();

        final StringBuilder js = new StringBuilder();
        js.append("<script type=\"text/javascript\">");
        if (inst != null && inst.getType().getUUID().equals(CIProjects.ServiceRequest.uuid)) {
            final PrintQuery print = new PrintQuery(inst);
            print.addSelect("linkto[Contact].oid", "linkto[Contact].attribute[Name]");
            print.execute();
            final String contactOID = print.<String>getSelect("linkto[Contact].oid");
            final String contactName = print.<String>getSelect("linkto[Contact].attribute[Name]");
            final String contactData = getFieldValue4Contact(Instance.get(contactOID));

            js.append("Wicket.Event.add(window, \"domready\", function(event) {")
                .append(getSetFieldValue(0, "contact", contactOID, contactName))
                .append(getSetFieldValue(0, "contactAutoComplete", contactName))
                .append(getSetFieldValue(0, "contactData", contactData))
                .append(" });");
        }
        js.append("</script>");
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, js.toString());
        return retVal;
    }

    /**
     * Method to connect a document to a service. the type of the relation is
     * evaluated and set automatically.
     * Can be extended passing properties:
     * connect[N] = UUID of Type; UUID of Connection Type
     * e.g.<br/>
     * &lt;property name=&quot;connect0&quot;&gt;3d81d32d-71ab-47d7-a25c-379a2af214be;53ec5f98-dcff-4277-8952-f552101ae121&lt;/property&gt;<br/>
     * &lt;property name=&quot;connect1&quot;&gt;4b041e2c-04db-46c6-bcbf-af4100ad5075;68e4823f-0dac-4d87-b54e-7acb02c1e460&lt;/property&gt;<br/>
     * @param _parameter Parameter as passed from eFaps
     * @return empty Return
     * @throws EFapsException on error
     */
    public Return connectDocument(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> others = (HashMap<?, ?>) _parameter.get(ParameterValues.OTHERS);
        final Map<?, ?> props = (HashMap<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String[] childOids = (String[]) others.get("selectedRow");
        if (childOids != null) {
            final Instance callInstance = _parameter.getCallInstance();
            for (final String childOid : childOids) {
                final Instance child = Instance.get(childOid);
                Insert insert = null;
                if (callInstance.getType().isKindOf(CIProjects.ProjectService.getType())) {
                    //defaults
                    if (child.getType().equals(CIProjects.ServiceRequest.getType())) {
                        insert = new Insert(CIProjects.ProjectService2Request);
                    } else if (child.getType().equals(CIProjects.WorkOrder.getType())) {
                        insert = new Insert(CIProjects.ProjectService2WorkOrder);
                    } else if (child.getType().equals(CIProjects.WorkReport.getType())) {
                        insert = new Insert(CIProjects.ProjectService2WorkReport);
                    } else if (child.getType().equals(CISales.CreditNote.getType())) {
                        insert = new Insert(CIProjects.ProjectService2CreditNote);
                    } else if (child.getType().equals(CISales.DeliveryNote.getType())) {
                        insert = new Insert(CIProjects.ProjectService2DeliveryNote);
                    } else if (child.getType().equals(CISales.GoodsIssueSlip.getType())) {
                        insert = new Insert(CIProjects.ProjectService2GoodsIssueSlip);
                    } else if (child.getType().equals(CISales.Invoice.getType())) {
                        insert = new Insert(CIProjects.ProjectService2Invoice);
                    } else if (child.getType().equals(CISales.OrderOutbound.getType())) {
                        insert = new Insert(CIProjects.ProjectService2OrderOutbound);
                    } else if (child.getType().equals(CISales.Quotation.getType())) {
                        insert = new Insert(CIProjects.ProjectService2Quotation);
                    } else if (child.getType().equals(CISales.Receipt.getType())) {
                        insert = new Insert(CIProjects.ProjectService2Receipt);
                    } else if (child.getType().equals(CISales.RecievingTicket.getType())) {
                        insert = new Insert(CIProjects.ProjectService2RecievingTicket);
                    } else if (child.getType().equals(CISales.ReturnSlip.getType())) {
                        insert = new Insert(CIProjects.ProjectService2ReturnSlip);
                    } else if (child.getType().equals(CISales.Reminder.getType())) {
                        insert = new Insert(CIProjects.ProjectService2Reminder);
                    } else if (child.getType().equals(CISales.CostSheet.getType())) {
                        insert = new Insert(CIProjects.ProjectService2CostSheet);
                    } else if (child.getType().equals(CISales.UsageReport.getType())) {
                        insert = new Insert(CIProjects.ProjectService2UsageReport);
                    } else if (child.getType().equals(CISales.ReturnUsageReport.getType())) {
                        insert = new Insert(CIProjects.ProjectService2ReturnUsageReport);
                    }
                    int i = 0;
                    while (insert == null && props.containsKey("connect" + i)) {
                        final String connectUUIDStr = (String) props.get("connect" + i);
                        final String[] connectUUIDs = connectUUIDStr.split(";");
                        if (child.getType().getUUID().equals(UUID.fromString(connectUUIDs[0]))) {
                            insert = new Insert(UUID.fromString(connectUUIDs[1]));
                        }
                        i++;
                    }
                }
                if (insert != null) {
                    insert.add(CIProjects.Project2DocumentAbstract.FromAbstract, callInstance.getId());
                    insert.add(CIProjects.Project2DocumentAbstract.ToAbstract, child.getId());
                    insert.execute();
                }
            }
        }
        return new Return();
    }

    /**
     * Check if the a relation of the given type already exists for this
     * instance.
     *
     * @param _typeUUID uuid of the ration type
     * @param _instance instance to be checked
     * @return true if already an relation of the given type exists for the
     *         instance, else false
     * @throws EFapsException on error
     */
    protected MultiPrintQuery check4Relation(final UUID _typeUUID,
                                     final Instance _instance)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(_typeUUID);
        queryBldr.addWhereAttrMatchValue(CIProjects.Project2DocumentAbstract.ToAbstract, _instance.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProjects.Project2DocumentAbstract.OID);
        multi.execute();
        return multi;
    }

    /**
     * Method to validate if a document is connected to a project.
     *
     * @param _parameter as passed from eFaps API.
     * @return Return with a DBproperty.
     * @throws EFapsException on error.
     */
    public Return validateConnectDocument(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> others = (HashMap<?, ?>) _parameter.get(ParameterValues.OTHERS);
        final StringBuilder html = new StringBuilder();
        final String[] childOids = (String[]) others.get("selectedRow");
        boolean validate = true;
        if (childOids != null) {
            final Instance callInstance = _parameter.getCallInstance();
            for (final String childOid : childOids) {
                final Instance child = Instance.get(childOid);
                if (callInstance.getType().isKindOf(CIProjects.ProjectService.getType())) {
                    if (child.getType().equals(CIProjects.ServiceRequest.getType())
                                    && check4Relation(CIProjects.ProjectService2Request.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CIProjects.WorkOrder.getType())
                                    && check4Relation(CIProjects.ProjectService2WorkOrder.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CIProjects.WorkReport.getType())
                                    && check4Relation(CIProjects.ProjectService2WorkReport.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.CreditNote.getType())
                                    && check4Relation(CIProjects.ProjectService2CreditNote.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.DeliveryNote.getType())
                                    && check4Relation(CIProjects.ProjectService2DeliveryNote.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.GoodsIssueSlip.getType())
                                    && check4Relation(CIProjects.ProjectService2GoodsIssueSlip.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.Invoice.getType())
                                    && check4Relation(CIProjects.ProjectService2Invoice.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.OrderOutbound.getType())
                                    && check4Relation(CIProjects.ProjectService2OrderOutbound.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.Quotation.getType())
                                    && check4Relation(CIProjects.ProjectService2Quotation.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.Receipt.getType())
                                    && check4Relation(CIProjects.ProjectService2Receipt.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.RecievingTicket.getType())
                                    && check4Relation(CIProjects.ProjectService2RecievingTicket.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.ReturnSlip.getType())
                                    && check4Relation(CIProjects.ProjectService2ReturnSlip.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.Reminder.getType())
                                    && check4Relation(CIProjects.ProjectService2Reminder.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.CostSheet.getType())
                                    && check4Relation(CIProjects.ProjectService2CostSheet.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    }
                }
            }
            if (validate) {
                ret.put(ReturnValues.TRUE, true);
                html.append(DBProperties.getProperty("org.efaps.esjp.projects.Project.validateConnectDoc"));
                ret.put(ReturnValues.SNIPLETT, html.toString());
            } else {
                html.insert(0, DBProperties.getProperty("org.efaps.esjp.projects.Project.invalidateConnectDoc")
                                                                                                            + "<p>");
                ret.put(ReturnValues.SNIPLETT, html.toString());
            }
        }
        return ret;
    }

    /**
     * Method to obtain the name of the document.
     *
     * @param _child with the instance of the document.
     * @return StringBuilder with the name of the document.
     * @throws EFapsException on error.
     */
    protected StringBuilder getString4ReturnInvalidate(final Instance _child)
        throws EFapsException
    {
        final StringBuilder html = new StringBuilder();
        final PrintQuery print = new PrintQuery(_child);
        print.addAttribute(CISales.DocumentAbstract.Name);
        print.execute();
        return html.append(_child.getType().getLabel()
                            + " - " + print.<String>getAttribute(CISales.DocumentAbstract.Name));
    }

    /**
     * Create a Label for accounting from a project.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return createLabel4Project(final Parameter _parameter)
        throws EFapsException
    {
        final Instance periodeInstance = (Instance) Context.getThreadContext().getSessionAttribute(
                        Transaction_Base.PERIODE_SESSIONKEY);

        final Insert insert = new Insert(CIAccounting.LabelProject);
        insert.add(CIAccounting.LabelProject.Name.name, _parameter.getParameterValue("projectAutoComplete"));
        insert.add(CIAccounting.LabelProject.Description, _parameter.getParameterValue("description"));
        insert.add(CIAccounting.LabelProject.PeriodeAbstractLink, periodeInstance.getId());
        insert.execute();

        final Instance projInstance = Instance.get(_parameter.getParameterValue("project"));
        final Insert relInsert = new Insert(CIProjects.ProjectService2Label);
        relInsert.add(CIProjects.ProjectService2Label.FromLink, projInstance.getId());
        relInsert.add(CIProjects.ProjectService2Label.ToLink, insert.getInstance().getId());
        relInsert.execute();
        return new Return();
    }

    @Override
    public Return getSalesPersonFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final org.efaps.esjp.common.uiform.Field field = new org.efaps.esjp.common.uiform.Field() {

            @Override
            public DropDownPosition getDropDownPosition(final Parameter _parameter,
                                                           final Object _value,
                                                           final Object _option)
                throws EFapsException
            {
                final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
                final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
                DropDownPosition pos;
                if (TargetMode.EDIT.equals(fieldValue.getTargetMode())) {
                    pos = new DropDownPosition(_value, _option) {
                        @Override
                        public boolean isSelected()
                        {
                            boolean ret = false;
                            final Long persId = (Long) fieldValue.getValue();
                            ret = getValue().equals(persId);
                            return ret;
                        }
                    };
                } else {
                    if ("true".equalsIgnoreCase((String) props.get("SelectCurrent"))) {
                        pos = new DropDownPosition(_value, _option) {

                            @Override
                            public boolean isSelected()
                            {
                                boolean ret = false;
                                long persId = 0;
                                try {
                                    persId = Context.getThreadContext().getPerson().getId();
                                } catch (final EFapsException e) {
                                    // nothing must be done at all
                                    e.printStackTrace();
                                }
                                ret = new Long(persId).equals(getValue());
                                return ret;
                            }
                        };
                    } else {
                        pos = super.getDropDownPosition(_parameter, _value, _option);
                    }
                }
                return pos;
            }

            @Override
            protected void add2QueryBuilder4List(final Parameter _parameter,
                                                 final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
                final String rolesStr = (String) props.get("Roles");
                final String groupsStr = (String) props.get("Groups");
                if (rolesStr != null && !rolesStr.isEmpty()) {
                    final String[] roles = rolesStr.split(";");
                    final List<Long> roleIds = new ArrayList<Long>();
                    for (final String role : roles) {
                        final Role aRole = Role.get(role);
                        if (aRole != null) {
                            roleIds.add(aRole.getId());
                        }
                    }
                    if (!roleIds.isEmpty()) {
                        final QueryBuilder queryBldr = new QueryBuilder(CIAdminUser.Person2Role);
                        queryBldr.addWhereAttrEqValue(CIAdminUser.Person2Role.UserToLink, roleIds.toArray());

                        _queryBldr.addWhereAttrInQuery(CIAdminUser.Abstract.ID,
                                        queryBldr.getAttributeQuery(CIAdminUser.Person2Role.UserFromLink));
                    }
                }
                if (groupsStr != null && !groupsStr.isEmpty()) {
                    final String[] groups;
                    boolean and = true;
                    if (groupsStr.contains("|")) {
                        groups = groupsStr.split("\\|");
                    } else {
                        groups = groupsStr.split(";");
                        and = false;
                    }

                    final List<Long> groupIds = new ArrayList<Long>();
                    for (final String group : groups) {
                        final Group aGroup = Group.get(group);
                        if (aGroup != null) {
                            groupIds.add(aGroup.getId());
                        }
                    }
                    if (!groupIds.isEmpty()) {
                        if (and) {
                            for (final Long group : groupIds) {
                                final QueryBuilder queryBldr = new QueryBuilder(CIAdminUser.Person2Group);
                                queryBldr.addWhereAttrEqValue(CIAdminUser.Person2Group.UserToLink, group);
                                final AttributeQuery attribute = queryBldr
                                                        .getAttributeQuery(CIAdminUser.Person2Group.UserFromLink);
                                _queryBldr.addWhereAttrInQuery(CIAdminUser.Abstract.ID, attribute);
                            }
                        } else {
                            final QueryBuilder queryBldr = new QueryBuilder(CIAdminUser.Person2Group);
                            queryBldr.addWhereAttrEqValue(CIAdminUser.Person2Group.UserToLink, groupIds.toArray());
                            final AttributeQuery attribute = queryBldr
                                                        .getAttributeQuery(CIAdminUser.Person2Group.UserFromLink);
                            _queryBldr.addWhereAttrInQuery(CIAdminUser.Abstract.ID, attribute);
                        }

                    }
                }
                super.add2QueryBuilder4List(_parameter, _queryBldr);
            }
        };
        return field.dropDownFieldValue(_parameter);
    }
}
