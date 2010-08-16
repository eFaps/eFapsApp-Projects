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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.SearchQuery;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f50c42d3-f5c2-4537-a5d1-8f91dec485c5")
@EFapsRevision("$Rev$")
public abstract class DocumentAbstract_Base
{
    /**
     * Autocomplete for the field used to select a contact. Depending on the
     * first character of the given input it is decided if the search will be
     * done for the name or for the taxnumber/Identity Carde. If the first
     * character is a number it will be searched in the name field, if it is
     * a letter it will be searched in the classification that contains the
     * taxnumber or Identity Card.
     *
     * @param _parameter    Parameter as passed from eFaps
     * @return Return containing map needed for an autocomplete field
     * @throws EFapsException on error
     */
    public Return autoComplete4Contact(final Parameter _parameter) throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String type = properties.containsKey("Type") ? (String) properties.get("Type") : "Contacts_Contact";
        if (input.length() > 0) {
            final boolean nameSearch = !Character.isDigit(input.charAt(0));
            if (nameSearch) {
                final SearchQuery query = new SearchQuery();
                query.setQueryTypes(type);
                query.addWhereExprMatchValue("Name", input + "*").setIgnoreCase(true);
                query.addSelect("OID");
                query.addSelect("Name");
                query.execute();
                while (query.next()) {
                    final String name = (String) query.get("Name");
                    final String oid = (String) query.get("OID");
                    final Map<String, String> map = new HashMap<String, String>();
                    map.put("eFapsAutoCompleteKEY", oid);
                    map.put("eFapsAutoCompleteVALUE", name);
                    map.put("eFapsAutoCompleteCHOICE", name);
                    list.add(map);
                }
            } else {
                final SearchQuery query = new SearchQuery();
                query.setQueryTypes("Contacts_ClassOrganisation");
                query.addWhereExprMatchValue("TaxNumber", input + "*");
                query.addSelect("ContactId");
                query.addSelect("TaxNumber");
                query.execute();
                final Map<String, Instance> tax2instances = new TreeMap<String, Instance>();
                while (query.next()) {
                    tax2instances.put((String) query.get("TaxNumber"),
                                      Instance.get(type, ((Long) query.get("ContactId")).toString()));
                }
                query.close();

                final SearchQuery query2 = new SearchQuery();
                query2.setQueryTypes("Contacts_ClassPerson");
                query2.addWhereExprMatchValue("IdentityCard", input + "*");
                query2.addSelect("ContactId");
                query2.addSelect("IdentityCard");
                query2.execute();
                while (query2.next()) {
                    tax2instances.put((String) query2.get("IdentityCard"),
                                      Instance.get(type, ((Long) query2.get("ContactId")).toString()));
                }
                query2.close();

                for (final Entry<String, Instance> entry : tax2instances.entrySet()) {
                    final PrintQuery print = new PrintQuery(entry.getValue());
                    print.addAttribute("OID", "Name");
                    if (print.execute()) {
                        final Map<String, String> map = new HashMap<String, String>();
                        map.put("eFapsAutoCompleteKEY", (String) print.getAttribute("OID"));
                        map.put("eFapsAutoCompleteVALUE", (String) print.getAttribute("Name"));
                        map.put("eFapsAutoCompleteCHOICE", entry.getKey() + " - " + print.getAttribute("Name"));
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
     * @param _parameter    Parameter as passed from eFaps
     * @return Return containing map needed to update the fields
     * @throws EFapsException on error
     */
    public Return updateFields4Contact(final Parameter _parameter) throws EFapsException
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
     * @return  String for the field
     * @throws EFapsException   on error
     */
    protected String getFieldValue4Contact(final Instance _instance) throws EFapsException
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


    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        return ret;
    }
}
