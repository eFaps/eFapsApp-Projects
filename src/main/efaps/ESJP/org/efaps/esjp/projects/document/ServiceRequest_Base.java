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

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("d77c0a30-4414-422a-8a8d-6426a8c3014e")
@EFapsRevision("$Rev$")
public class ServiceRequest_Base
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

        final Insert insert = new Insert(CIProjects.ServiceRequest);
        insert.add(CIProjects.ServiceRequest.Name, _parameter.getParameterValue("name"));
        insert.add(CIProjects.ServiceRequest.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CIProjects.ServiceRequest.Contact, contactid);
        insert.add(CIProjects.ServiceRequest.Date, date);
        insert.add(CIProjects.ServiceRequest.Status, Status.find(CIProjects.ServiceRequestStatus.uuid, "Open").getId());
        insert.execute();
        return new Return();
    }

}
