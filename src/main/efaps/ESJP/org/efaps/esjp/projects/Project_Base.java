/*
 * Copyright 2003 - 2015 The eFaps Team
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

package org.efaps.esjp.projects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.BooleanUtils;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
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
import org.efaps.esjp.accounting.Period;
import org.efaps.esjp.ci.CIAccounting;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.erp.AbstractWarning;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.erp.WarningUtil;
import org.efaps.esjp.projects.listener.IOnProject;
import org.efaps.esjp.projects.listener.OnCreateFromDocument;
import org.efaps.esjp.projects.util.Projects;
import org.efaps.esjp.projects.util.ProjectsSettings;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("e0c8e7d5-ca9c-46b3-967e-8bd307b17c93")
@EFapsApplication("eFapsApp-Projects")
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
                _insert.add(CIProjects.ProjectService.Name, getDocName4Create(_parameter));
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

                final Instance periodInst = new Period().evaluateCurrentPeriod(_parameter);

                if (periodInst != null && periodInst.isValid()) {
                    final PrintQuery print = new PrintQuery(_instance);
                    print.addAttribute(CIProjects.ProjectAbstract.Name, CIProjects.ProjectAbstract.Description);
                    print.execute();

                    final Insert insert = new Insert(CIAccounting.LabelProject);
                    insert.add(CIAccounting.LabelProject.Name, print.getAttribute(CIProjects.ProjectAbstract.Name));
                    insert.add(CIAccounting.LabelProject.Description,
                                    print.getAttribute(CIProjects.ProjectAbstract.Description));
                    insert.add(CIAccounting.LabelProject.PeriodAbstractLink, periodInst);
                    insert.add(CIAccounting.LabelProject.Status, Status.find(CIAccounting.LabelStatus.Active));
                    insert.executeWithoutAccessCheck();

                    final Insert relInsert = new Insert(CIProjects.ProjectService2Label);
                    relInsert.add(CIProjects.ProjectService2Label.FromLink, _instance);
                    relInsert.add(CIProjects.ProjectService2Label.ToLink, insert.getInstance());
                    relInsert.executeWithoutAccessCheck();
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

        final Map<String, Map<String, String>> sortMap = new TreeMap<String, Map<String, String>>();
        if (input.length() > 0) {
            final String choiceFormat = containsProperty(_parameter, "ChoiceFormat")
                            ? getProperty(_parameter, "ChoiceFormat") : "%s - %s - %s";
            final String valueFormat = containsProperty(_parameter, "ValueFormat")
                            ? getProperty(_parameter, "ValueFormat") : "%s - %s";

            final QueryBuilder filterAttrQueryBldr = new QueryBuilder(CIProjects.ProjectAbstract);
            filterAttrQueryBldr.setOr(true);
            filterAttrQueryBldr.addWhereAttrMatchValue(CIProjects.ProjectAbstract.Name, input + "*")
                            .setIgnoreCase(true);
            filterAttrQueryBldr.addWhereAttrMatchValue(CIProjects.ProjectAbstract.Description, input + "*")
                            .setIgnoreCase(true);
            final AttributeQuery filterAttrQuery = filterAttrQueryBldr.getAttributeQuery(CIProjects.ProjectAbstract.ID);

            final QueryBuilder queryBldr = new QueryBuilder(CIProjects.ProjectAbstract);
            queryBldr.addWhereAttrInQuery(CIProjects.ProjectAbstract.ID, filterAttrQuery);
            queryBldr.addOrderByAttributeAsc(CIProjects.ProjectAbstract.Name);
            InterfaceUtils.addMaxResult2QueryBuilder4AutoComplete(_parameter, queryBldr);
            final List<Status> statusList = getStatusListFromProperties(_parameter);
            if (!statusList.isEmpty()) {
                queryBldr.addWhereAttrEqValue(CIProjects.ProjectAbstract.StatusAbstract, statusList.toArray());
            }

            final MultiPrintQuery print = queryBldr.getPrint();
            print.addAttribute(CIProjects.ProjectAbstract.OID, CIProjects.ProjectAbstract.Name,
                            CIProjects.ProjectAbstract.Description);
            final SelectBuilder selContactName = SelectBuilder.get()
                            .linkto(CIProjects.ProjectAbstract.Contact)
                            .attribute(CIContacts.Contact.Name);
            print.addSelect(selContactName);
            print.execute();
            while (print.next()) {
                final String name = print.<String>getAttribute(CIProjects.ProjectAbstract.Name);
                final String oid = print.<String>getAttribute(CIProjects.ProjectAbstract.OID);
                final String description = print.<String>getAttribute(CIProjects.ProjectAbstract.Description);
                final String contactName = print.<String>getSelect(selContactName);

                final String choice = String.format(Context.getThreadContext().getLocale(), choiceFormat, name,
                                description, contactName);
                final String value = String.format(Context.getThreadContext().getLocale(), valueFormat, name,
                                description, contactName);

                final Map<String, String> map = new HashMap<String, String>();
                map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), oid);
                map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), value);
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


    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return listmap for fieldupdate event
     * @throws EFapsException on error
     */
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
                map.put(contactField, new String[] {contactInst.getOid(), print.<String>getSelect(contName)});
            }
        }

        final String projDataField = getProperty(_parameter, "Project_DataField", "projectData");
        map.put(projDataField, getProjectData(_parameter, instance).toString());

        for (final IOnProject listener : Listener.get().<IOnProject>invoke(IOnProject.class)) {
            listener.updateField4Project(_parameter, instance, map);
        }

        list.add(map);
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }


    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @param _projInst instance of the project
     * @return StringBuilder containing information abouty the project
     * @throws EFapsException on error
     */
    public StringBuilder getProjectData(final Parameter _parameter,
                                        final Instance _projInst)
        throws EFapsException
    {

        final StringBuilder ret = new StringBuilder();
        if (_projInst != null && _projInst.isValid()) {
            final PrintQuery print = new PrintQuery(_projInst);
            final SelectBuilder contName = SelectBuilder.get().linkto(CIProjects.ProjectAbstract.Contact)
                            .attribute(CIContacts.Contact.Name);
            print.addSelect(contName);
            print.addAttribute(CIProjects.ProjectAbstract.Description, CIProjects.ProjectAbstract.Name);
            print.executeWithoutAccessCheck();
            ret.append(print.<String>getAttribute(CIProjects.ProjectAbstract.Name))
                .append(" - ").append(print.<String>getAttribute(CIProjects.ProjectAbstract.Description))
                .append(" - ")
                .append(print.<String>getSelect(contName));
        }
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
     * Method returns a javacript to set the values for the contact.
     *
     * @param _parameter Parameter as passed from eFaps
     * @return Return with javascript
     * @throws EFapsException on error
     */
    public Return getJavaScript4ProjectUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Instance inst = _parameter.getInstance();
        final StringBuilder js = new StringBuilder();
        if (inst != null && inst.isValid()) {
            final List<Instance> instances = new ArrayList<Instance>();
            instances.add(inst);
            final StringBuilder pJs = new OnCreateFromDocument().add2JavaScript4Document(_parameter, instances);

            final boolean readOnly = "true".equalsIgnoreCase(getProperty(_parameter, "ReadOnly"));

            if (pJs.length() > 0) {
                js.append("<script type=\"text/javascript\">\n")
                    .append("require([\"dojo/ready\", \"dojo/query\",\"dojo/dom-construct\"],")
                    .append(" function(ready, query, domConstruct){\n")
                    .append(" ready(1500, function(){")
                    .append(pJs);
                if (readOnly) {
                    final String fieldName = containsProperty(_parameter, "FieldName") ? getProperty(_parameter,
                                    "FieldName") : "project";
                    js.append(getSetFieldReadOnlyScript(_parameter, fieldName));
                }
                js.append("});").append("});\n</script>\n");
            }
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, js.toString());
        return retVal;
    }


    /**
     * Method to connect a document to a service. the type of the relation is
     * evaluated and set automatically.
     * @param _parameter Parameter as passed from eFaps
     * @return empty Return
     * @throws EFapsException on error
     */
    public Return connectDocument(final Parameter _parameter)
        throws EFapsException
    {
        final Instance callInstance = _parameter.getCallInstance();
        if (callInstance.getType().isKindOf(CIProjects.ProjectService.getType())) {
            for (final Instance childInst : getSelectedInstances(_parameter)) {
                final Properties properties = Projects.getSysConfig().getAttributeValueAsProperties(
                                ProjectsSettings.CONNECT2DOC, true);

                final Map<Integer, String> childtypes = analyseProperty(_parameter, properties, "ConnectChildType");
                final Map<Integer, String> connectTypes = analyseProperty(_parameter, properties, "ConnectType");
                final BidiMap<Integer, String> bidi = new DualHashBidiMap<>(childtypes);

                if (bidi.containsValue(childInst.getType().getName())) {
                    final Insert insert = new Insert(connectTypes.get(bidi.getKey(childInst.getType().getName())));
                    insert.add(CIProjects.Project2DocumentAbstract.FromAbstract, callInstance);
                    insert.add(CIProjects.Project2DocumentAbstract.ToAbstract, childInst);
                    insert.execute();
                }
            }
        }
        return new Return();
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
        final List<IWarning> warnings = new ArrayList<IWarning>();
        for (final Instance childInst : getSelectedInstances(_parameter)) {
            final Properties properties = Projects.getSysConfig().getAttributeValueAsProperties(
                            ProjectsSettings.CONNECT2DOC, true);

            final Map<Integer, String> childtypes = analyseProperty(_parameter, properties, "ConnectChildType");
            final Map<Integer, String> connectTypes = analyseProperty(_parameter, properties, "ConnectType");
            final Map<Integer, String> unique = analyseProperty(_parameter, properties, "Unique");
            final BidiMap<Integer, String> bidi = new DualHashBidiMap<>(childtypes);

            if (bidi.containsValue(childInst.getType().getName())) {
                final Type type = Type.get(connectTypes.get(bidi.getKey(childInst.getType().getName())));

                final QueryBuilder queryBldr = new QueryBuilder(type);
                queryBldr.addWhereAttrEqValue(CIProjects.Project2DocumentAbstract.FromAbstract,
                                _parameter.getCallInstance());
                queryBldr.addWhereAttrEqValue(CIProjects.Project2DocumentAbstract.ToAbstract, childInst);
                if (!queryBldr.getQuery().execute().isEmpty()) {
                    warnings.add(new Project2DocDuplicateWarning(childInst));
                } else if (BooleanUtils.toBoolean(unique.get(bidi.getKey(childInst.getType().getName())))) {
                    final QueryBuilder queryBldr2 = new QueryBuilder(type);
                    queryBldr2.addWhereAttrEqValue(CIProjects.Project2DocumentAbstract.ToAbstract, childInst);
                    if (!queryBldr2.getQuery().execute().isEmpty()) {
                        warnings.add(new Project2DocUniqueWarning(childInst));
                    }
                }
            }
        }
        if (warnings.isEmpty()) {
            ret.put(ReturnValues.TRUE, true);
        } else {
            ret.put(ReturnValues.SNIPLETT, WarningUtil.getHtml4Warning(warnings).toString());
            if (!WarningUtil.hasError(warnings)) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    /**
     *  @param _parameter Parameter as passed from the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return getTypeFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Properties properties = Projects.getSysConfig().getAttributeValueAsProperties(
                        ProjectsSettings.CONNECT2DOC, true);
        final List<DropDownPosition> positions = new ArrayList<>();
        for (final String typeStr : analyseProperty(_parameter, properties, "ConnectChildType").values()) {
            final Type type = Type.get(typeStr);
            if (type != null) {
                final DropDownPosition position = new DropDownPosition(type.getId(), type.getLabel());
                positions.add(position);
            }
        }
        Collections.sort(positions, new Comparator<DropDownPosition>()
        {

            @SuppressWarnings("unchecked")
            @Override
            public int compare(final DropDownPosition _arg0,
                               final DropDownPosition _arg1)
            {
                return _arg0.getOrderValue().compareTo(_arg1.getOrderValue());
            }
        });
        final Return ret = new Return();
        ret.put(ReturnValues.SNIPLETT, new Field().getDropDownField(_parameter, positions));
        return ret;
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
        createLabel4Project(_parameter, _parameter.getInstance());
        return new Return();
    }

    /**
     * Create a Label for accounting from a project.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _projectInst the _project inst
     * @throws EFapsException on error
     */
    public void createLabel4Project(final Parameter _parameter,
                                    final Instance _projectInst)
        throws EFapsException
    {

        final Instance periodeInstance = new Period().evaluateCurrentPeriod(_parameter);

        final PrintQuery print = new PrintQuery(_projectInst);
        print.addAttribute(CIProjects.ProjectAbstract.Name, CIProjects.ProjectAbstract.Description);
        print.executeWithoutAccessCheck();

        final Insert insert = new Insert(CIAccounting.LabelProject);
        insert.add(CIAccounting.LabelProject.Name.name, print.getAttribute(CIProjects.ProjectAbstract.Name));
        insert.add(CIAccounting.LabelProject.Description, print.getAttribute(CIProjects.ProjectAbstract.Description));
        insert.add(CIAccounting.LabelProject.PeriodAbstractLink, periodeInstance.getId());
        insert.execute();

        final Insert relInsert = new Insert(CIProjects.ProjectService2Label);
        relInsert.add(CIProjects.ProjectService2Label.FromLink, _projectInst);
        relInsert.add(CIProjects.ProjectService2Label.ToLink, insert.getInstance().getId());
        relInsert.execute();
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
                        /**  */
                        private static final long serialVersionUID = 1L;

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
                            /**  */
                            private static final long serialVersionUID = 1L;

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

    /**
     * The Class DocWarning.
     *
     */
    public abstract static class AbstractDocWarning
        extends AbstractWarning
    {

        /**
         * Instantiates a new doc warning.
         *
         * @param _instance the _instance
         * @throws EFapsException on error
         */
        public AbstractDocWarning(final Instance _instance)
            throws EFapsException
        {
            if (_instance.getType().isCIType(CIERP.DocumentAbstract)) {
                final PrintQuery print = new PrintQuery(_instance);
                print.addAttribute(CIERP.DocumentAbstract.Name);
                print.execute();
                addObject(_instance.getType().getLabel(), print.getAttribute(CIERP.DocumentAbstract.Name));
            }
        }
    }

    /**
     * Warning for amount greater zero.
     */
    public static class Project2DocDuplicateWarning
        extends AbstractDocWarning
    {

        /**
         * Constructor.
         *
         * @param _instance the _instance
         * @throws EFapsException on error
         */
        public Project2DocDuplicateWarning(final Instance _instance)
            throws EFapsException
        {
            super(_instance);
            setError(true);
        }
    }

    /**
     * Warning for amount greater zero.
     */
    public static class Project2DocUniqueWarning
        extends AbstractDocWarning
    {

        /**
         * Constructor.
         *
         * @param _instance the _instance
         * @throws EFapsException on error
         */
        public Project2DocUniqueWarning(final Instance _instance)
            throws EFapsException
        {
            super(_instance);
            setError(true);
        }
    }
}
