/*
 * Copyright 2003 - 2014 The eFaps Team
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
package org.efaps.esjp.projects.communicationsMatrix;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIFormProjects;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.util.EFapsException;


/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 * @version $Id: DocumentAbstract.java 5526 2010-09-10 14:17:54Z miguel.a.aranya $
 */
@EFapsUUID("d8c366c8-5a5c-43b8-80e2-708d4511c666")
@EFapsRevision("$Rev: 5526 $")
public class CommunicationsMatrix_Base
{

    public Return create4CommunicationsMatrix(final Parameter _parameter)
        throws EFapsException
    {
        final Instance projectInst = _parameter.getInstance();
        final Insert insert = new Insert(CIProjects.CommunicationsMatrix);
        insert.add(CIProjects.CommunicationsMatrix.ProjectAbstractLink, projectInst.getId());
       insert.add(CIProjects.CommunicationsMatrix.Contact,
                        _parameter.getParameterValue(CIFormProjects.Projects_CommunicationsMatrixForm.contact.name));
        insert.add(CIProjects.CommunicationsMatrix.Email,
                        _parameter.getParameterValue(CIFormProjects.Projects_CommunicationsMatrixForm.email.name));
        insert.add(CIProjects.CommunicationsMatrix.Phone,
                        _parameter.getParameterValue(CIFormProjects.Projects_CommunicationsMatrixForm.phone.name));
        insert.add(CIProjects.CommunicationsMatrix.Origin,
                        _parameter.getParameterValue(CIFormProjects.Projects_CommunicationsMatrixForm.origin.name));
        insert.add(CIProjects.CommunicationsMatrix.Note,
                        _parameter.getParameterValue(CIFormProjects.Projects_CommunicationsMatrixForm.note.name));
        final String pointOfContactLink = _parameter
                        .getParameterValue(CIFormProjects.Projects_CommunicationsMatrixForm.pointOfContact.name);
        if (pointOfContactLink != null) {
            final Instance pointOfContactInst = Instance.get(pointOfContactLink);
            if (pointOfContactInst.isValid()) {
                insert.add(CIProjects.CommunicationsMatrix.PointOfContact, pointOfContactInst.getId());
            }
        }
        insert.execute();
        return new Return();
    }
}
