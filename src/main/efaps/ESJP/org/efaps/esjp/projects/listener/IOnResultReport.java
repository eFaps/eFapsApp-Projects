/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.projects.listener;

import java.math.BigDecimal;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
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
@EFapsApplication("eFapsApp-Projects")
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
