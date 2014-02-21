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
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */


package org.efaps.esjp.projects.dashboard;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;


/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("3421e1be-0f01-463d-b252-c84ab13794d3")
@EFapsRevision("$Rev$")
public class ProjectStatusPiePanel
    extends ProjectStatusPiePanel_Base
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    protected String getTitle()
    {
        return DBProperties.getProperty(ProjectStatusPiePanel.class.getName() + ".Title");
    }
}