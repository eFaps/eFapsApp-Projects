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

package org.efaps.esjp.projects.document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: DocumentAbstract_Base.java 3725 2010-02-15 04:15:09Z jan.moxter
 *          $
 */
@EFapsUUID("f50c42d3-f5c2-4537-a5d1-8f91dec485c5")
@EFapsRevision("$Rev$")
public abstract class DocumentAbstract_Base
    extends CommonDocument
{
    /**
     * Autocomplete for the field used to select a contact. Depending on the
     * first character of the given input it is decided if the search will be
     * done for the name or for the taxnumber/Identity Carde. If the first
     * character is a number it will be searched in the name field, if it is a
     * letter it will be searched in the classification that contains the
     * taxnumber or Identity Card.
     *
     * @param _parameter Parameter as passed from eFaps
     * @return Return containing map needed for an autocomplete field
     * @throws EFapsException on error
     */
    public Return autoComplete4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String type = properties.containsKey("Type") ? (String) properties.get("Type") : "Contacts_Contact";
        final String keyStr = properties.containsKey("Key") ? (String) properties.get("Key") : "OID";
        if (input.length() > 0) {
            final boolean nameSearch = !Character.isDigit(input.charAt(0));
            if (nameSearch) {
                final QueryBuilder queryBldr = new QueryBuilder(Type.get(type));
                queryBldr.addWhereAttrMatchValue("Name", input + "*").setIgnoreCase(true);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(keyStr, "Name");
                multi.execute();
                while (multi.next()) {
                    final String name = multi.<String>getAttribute("Name");
                    final String key = multi.<String>getAttribute(keyStr);
                    final Map<String, String> map = new HashMap<String, String>();
                    map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), key);
                    map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
                    map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), name);
                    list.add(map);
                }
            } else {
                final QueryBuilder queryBldr = new QueryBuilder(CIContacts.ClassOrganisation);
                queryBldr.addWhereAttrMatchValue(CIContacts.ClassOrganisation.TaxNumber, input + "*");
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CIContacts.ClassOrganisation.ContactId, CIContacts.ClassOrganisation.TaxNumber);
                multi.execute();
                final Map<String, Instance> tax2instances = new TreeMap<String, Instance>();
                while (multi.next()) {
                    tax2instances.put(multi.<String>getAttribute("TaxNumber"),
                                    Instance.get(type, multi.<Long>getAttribute("ContactId").toString()));
                }

                final QueryBuilder queryBldr2 = new QueryBuilder(CIContacts.ClassPerson);
                queryBldr2.addWhereAttrMatchValue(CIContacts.ClassPerson.IdentityCard, input + "*");
                final MultiPrintQuery multi2 = queryBldr2.getPrint();
                multi2.addAttribute(CIContacts.ClassPerson.ContactId, CIContacts.ClassPerson.IdentityCard);
                multi2.execute();
                while (multi2.next()) {
                    tax2instances.put(multi2.<String>getAttribute("IdentityCard"),
                                    Instance.get(type, multi2.<Long>getAttribute("ContactId").toString()));
                }

                for (final Entry<String, Instance> entry : tax2instances.entrySet()) {
                    final PrintQuery print = new PrintQuery(entry.getValue());
                    print.addAttribute(keyStr, "Name");
                    if (print.execute()) {
                        final String key = multi.<String>getAttribute(keyStr);
                        final String name = multi.<String>getAttribute("Name");
                        final Map<String, String> map = new HashMap<String, String>();
                        map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), key);
                        map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
                        map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), entry.getKey() + " - " + name);
                        list.add(map);
                    }
                }
            }
        }
        final Return retVal = new Return();
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
        final Instance instance = Instance.get(_parameter.getParameterValue("contact"));
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        if (instance.getId() > 0) {
            map.put("contactData", getFieldValue4Contact(instance));
        } else {
            map.put("contactData", "????");
        }
        list.add(map);
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
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
        final PrintQuery print = new PrintQuery(_instance);
        print.addSelect("class[Sales_Contacts_ClassClient].attribute[BillingAdressStreet]");
        print.addSelect("class[Contacts_ClassOrganisation].attribute[TaxNumber]");
        print.addSelect("class[Contacts_ClassPerson].attribute[IdentityCard]");
        print.execute();
        final String taxnumber = print.<String>getSelect("class[Contacts_ClassOrganisation].attribute[TaxNumber]");
        final String idcard = print.<String>getSelect("class[Contacts_ClassPerson].attribute[IdentityCard]");
        final boolean dni = taxnumber == null || (taxnumber.length() < 1 && idcard != null && idcard.length() > 1);
        final StringBuilder strBldr = new StringBuilder();
        strBldr.append(dni ? DBProperties.getProperty("Contacts_ClassPerson/IdentityCard.Label")
                            : DBProperties.getProperty("Contacts_ClassOrganisation/TaxNumber.Label"))
                        .append(": ").append(dni ? idcard : taxnumber).append("  -  ")
                        .append(DBProperties.getProperty("Sales_Contacts_ClassClient/BillingAdressStreet.Label"))
                        .append(": ")
                        .append(print.getSelect("class[Sales_Contacts_ClassClient].attribute[BillingAdressStreet]"));
        return strBldr.toString();
    }

    /**
     * Method for obtains a javascript.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        return ret;
    }

    /**
     * Get a "eFapsSetFieldValue" Javascript line.
     * @param _idx          index of the field
     * @param _fieldName    name of the field
     * @param _value        value
     * @return StringBuilder
     */
    @Override
    protected StringBuilder getSetFieldValue(final int _idx,
                                             final String _fieldName,
                                             final String _value)
    {
        return getSetFieldValue(_idx, _fieldName, _value, true);
    }

    /**
     * Get a "eFapsSetFieldValue" Javascript line.
     * @param _idx          index of the field
     * @param _fieldName    name of the field
     * @param _value        value
     * @param _escape       must the value be escaped
     * @return StringBuilder
     */
    @Override
    protected StringBuilder getSetFieldValue(final int _idx,
                                             final String _fieldName,
                                             final String _value,
                                             final boolean _escape)
    {
        final StringBuilder ret = new StringBuilder();
        ret.append("eFapsSetFieldValue(").append(_idx).append(",'").append(_fieldName).append("',");
        if (_escape) {
            ret.append("'").append(StringEscapeUtils.escapeEcmaScript(_value)).append("'");
        } else {
            ret.append(_value);
        }
        ret.append(");");
        return ret;
    }

}
