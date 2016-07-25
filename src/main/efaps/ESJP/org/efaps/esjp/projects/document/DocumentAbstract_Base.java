/*
 * Copyright 2003 - 2013 The eFaps Team
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

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: DocumentAbstract_Base.java 3725 2010-02-15 04:15:09Z jan.moxter
 *          $
 */
@EFapsUUID("f50c42d3-f5c2-4537-a5d1-8f91dec485c5")
@EFapsApplication("eFapsApp-Projects")
public abstract class DocumentAbstract_Base
    extends CommonDocument
{
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
        return new Contacts().autoComplete4Contact(_parameter);
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
        final Contacts contact = new Contacts()
        {
            @Override
            public String getFieldValue4Contact(final Instance _instance)
                throws EFapsException
            {
                return DocumentAbstract_Base.this.getFieldValue4Contact(_instance);
            };
        };
        return contact.updateFields4Contact(_parameter);
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
        return new Contacts().getFieldValue4Contact(_instance);
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
}
