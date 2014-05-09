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

import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.sales.listener.IOnCreateFromDocument;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("e761f76e-3ad4-4782-a743-636e53c4327a")
@EFapsRevision("$Rev$")
public abstract class OnCreateFromDocument_Base
    extends CommonDocument
    implements IOnCreateFromDocument
{

    @Override
    public StringBuilder add2JavaScript4Document(final Parameter _parameter,
                                                 final List<Instance> _instances)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        if (!_instances.isEmpty()) {
            final Instance instance = _instances.get(0);
            if (instance.isValid()) {
                final PrintQuery print = new PrintQuery(instance);
                SelectBuilder projBaseSel = null;
                if (instance.getType().isKindOf(CISales.IncomingInvoice.getType())) {
                    projBaseSel = SelectBuilder.get()
                                    .linkfrom(CIProjects.ProjectService2IncomingInvoice,
                                                    CIProjects.ProjectService2IncomingInvoice.ToLink)
                                    .linkto(CIProjects.ProjectService2IncomingInvoice.FromLink);
                } else if (instance.getType().isKindOf(CISales.OrderOutbound.getType())) {
                    projBaseSel = SelectBuilder.get()
                                    .linkfrom(CIProjects.ProjectService2OrderOutbound,
                                                    CIProjects.ProjectService2OrderOutbound.ToLink)
                                    .linkto(CIProjects.ProjectService2OrderOutbound.FromLink);
                }

                if (projBaseSel != null) {
                    final SelectBuilder projInstSel = new SelectBuilder(projBaseSel).instance();
                    final SelectBuilder projNameSel = new SelectBuilder(projBaseSel)
                                    .attribute(CIProjects.ProjectAbstract.Name);
                    final SelectBuilder projDescSel = new SelectBuilder(projBaseSel)
                                    .attribute(CIProjects.ProjectAbstract.Description);

                    print.addSelect(projInstSel, projNameSel, projDescSel);
                    print.execute();
                    final Instance projInst = print.<Instance>getSelect(projInstSel);
                    if (projInst != null && projInst.isValid()) {
                        ret.append(getSetFieldValue(0, "project", projInst.getOid(),
                                        print.<String>getSelect(projNameSel) + " - "
                                                        + print.<String>getSelect(projDescSel)));
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public int getWeight()
    {
        return 0;
    }
}
