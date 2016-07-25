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

package org.efaps.esjp.projects.listener;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.accounting.listener.IOnPurchaseRecord;
import org.efaps.esjp.accounting.util.Accounting.Taxed4PurchaseRecord;
import org.efaps.esjp.ci.CIAccounting;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("a125853e-6c2b-4bb6-9016-b6570de823e1")
@EFapsApplication("eFapsApp-Projects")
public abstract class OnPurchaseRecord_Base
    implements IOnPurchaseRecord
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInstance of the document to be evaluated
     * @return Taxed4PurchaseRecord, null if not found
     * @throws EFapsException on error
     */
    @Override
    public Taxed4PurchaseRecord evalTaxed(final Parameter _parameter,
                                          final Instance _docInstance)
        throws EFapsException
    {
        Taxed4PurchaseRecord ret = null;
        final QueryBuilder attrQueryBldr = new QueryBuilder(CIAccounting.PurchaseRecordConfigProjectUntaxed);

        final QueryBuilder queryBldr = new QueryBuilder(CIProjects.ProjectService2DocumentAbstract);
        queryBldr.addWhereAttrEqValue(CIProjects.ProjectService2DocumentAbstract.ToDocument, _docInstance);
        queryBldr.addWhereAttrInQuery(CIProjects.ProjectService2DocumentAbstract.FromService,
                        attrQueryBldr.getAttributeQuery(CIAccounting.PurchaseRecordConfigProjectUntaxed.Projectlink));
        if (!queryBldr.getQuery().execute().isEmpty()) {
            ret = Taxed4PurchaseRecord.UNTAXED;
        }
        return ret;
    }

    @Override
    public int getWeight()
    {
        return 0;
    }
}
