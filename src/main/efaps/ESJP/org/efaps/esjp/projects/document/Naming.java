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

package org.efaps.esjp.projects.document;

import java.util.Date;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("1df73537-e1e6-415a-ad86-69cb508d3547")
@EFapsRevision("$Rev$")
public class Naming
    extends Naming_Base
{

    public static String getName(final Instance _projectInstance,
                                 final Type _docType,
                                 final boolean _orderByID)
        throws EFapsException
    {
        return Naming_Base.getName(_projectInstance, _docType, null, null, _orderByID);
    }

    public static String getName(final Instance _projectInstance,
                                 final Type _docType,
                                 final NumberGenerator _numGen,
                                 final Date _dateGen,
                                 final boolean _orderByID)
        throws EFapsException
    {
        return Naming_Base.getName(_projectInstance, _docType, _numGen, _dateGen, _orderByID);
    }

    public static String getLast(final Instance _projectInstance,
                                 final Type _docType,
                                 final boolean _orderByID)
        throws EFapsException
    {
        return Naming_Base.getLast(_projectInstance, _docType, _orderByID);
    }

    public static String getName(final Instance _projectInstance,
                                 final Type _docType,
                                 final NumberGenerator _numGen,
                                 final Date _dateGen,
                                 final String _last)
        throws EFapsException
    {
        return Naming_Base.getName(_projectInstance, _docType, _numGen, _dateGen, _last);
    }
}
