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
 * Revision:        $Rev: 13342 $
 * Last Changed:    $Date: 2014-07-16 12:27:59 -0500 (Wed, 16 Jul 2014) $
 * Last Changed By: $Author: jan@moxter.net $
 */

package org.efaps.esjp.projects.listener;

import java.math.BigDecimal;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.IEsjpListener;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

/**
 * Contains methods that are executed during the process of creating a document
 * from a document.
 *
 * @author The eFaps Team
 * @version $Id:  $
 */
@EFapsUUID("73964b75-c83a-4548-8a08-27093857601d")
@EFapsRevision("$Rev: 13342 $")
public interface IOnResultReport
    extends IEsjpListener
{

    /**
     * @param _parameter
     * @param _instance
     * @param _net
     * @param _cross
     * @param _add
     * @return
     */
    boolean addAmounts2DocBean(final Parameter _parameter,
                               final Instance _instance,
                               final BigDecimal _net,
                               final BigDecimal _cross,
                               final Object _bean,
                               final boolean _add)
        throws EFapsException;
}
