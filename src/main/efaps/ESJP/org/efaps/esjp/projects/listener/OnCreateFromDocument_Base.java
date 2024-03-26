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

import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.projects.Project;
import org.efaps.esjp.sales.listener.IOnCreateFromDocument;
import org.efaps.util.EFapsException;
/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("e761f76e-3ad4-4782-a743-636e53c4327a")
@EFapsApplication("eFapsApp-Projects")
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
                if (instance.getType().isKindOf(CISales.IncomingInvoice)) {
                    projBaseSel = SelectBuilder.get()
                                    .linkfrom(CIProjects.ProjectService2IncomingInvoice.ToLink)
                                    .linkto(CIProjects.ProjectService2IncomingInvoice.FromLink);
                } else if (instance.getType().isKindOf(CISales.OrderOutbound)) {
                    projBaseSel = SelectBuilder.get()
                                    .linkfrom(CIProjects.ProjectService2OrderOutbound.ToLink)
                                    .linkto(CIProjects.ProjectService2OrderOutbound.FromLink);
                } else if (instance.getType().isKindOf(CISales.ServiceOrderOutbound)) {
                    projBaseSel = SelectBuilder.get()
                                    .linkfrom(CIProjects.ProjectService2ServiceOrderOutbound.ToLink)
                                    .linkto(CIProjects.ProjectService2ServiceOrderOutbound.FromLink);
                } else if (instance.getType().isKindOf(CISales.Invoice)) {
                    projBaseSel = SelectBuilder.get()
                                        .linkfrom(CIProjects.ProjectService2Invoice.ToLink)
                                        .linkto(CIProjects.ProjectService2Invoice.FromLink);
                } else if (instance.getType().isKindOf(CISales.DeliveryNote)) {
                    projBaseSel = SelectBuilder.get()
                                    .linkfrom(CIProjects.ProjectService2DeliveryNote.ToLink)
                                    .linkto(CIProjects.ProjectService2DeliveryNote.FromLink);
                } else if (instance.getType().isKindOf(CISales.QuoteRequest)) {
                    projBaseSel = SelectBuilder.get()
                                    .linkfrom(CIProjects.ProjectService2QuoteRequest.ToLink)
                                    .linkto(CIProjects.ProjectService2QuoteRequest.FromLink);
                } else if (instance.getType().isKindOf(CISales.ProductRequest)) {
                    projBaseSel = SelectBuilder.get()
                                    .linkfrom(CIProjects.ProjectService2ProductRequest.ToLink)
                                    .linkto(CIProjects.ProjectService2ProductRequest.FromLink);
                } else if (instance.getType().isKindOf(CISales.ServiceRequest)) {
                    projBaseSel = SelectBuilder.get()
                                    .linkfrom(CIProjects.ProjectService2ServiceRequest.ToLink)
                                    .linkto(CIProjects.ProjectService2ServiceRequest.FromLink);
                } else if (instance.getType().isKindOf(CISales.AccountPettyCash)) {
                    projBaseSel = SelectBuilder.get()
                                    .linkfrom(CIProjects.ProjectService2PettyCash.ToLink)
                                    .linkto(CIProjects.ProjectService2PettyCash.FromLink);
                } else if (instance.getType().isKindOf(CISales.AccountFundsToBeSettled)) {
                    projBaseSel = SelectBuilder.get()
                                    .linkfrom(CIProjects.ProjectService2FundsToBeSettled.ToLink)
                                    .linkto(CIProjects.ProjectService2FundsToBeSettled.FromLink);
                } else if (instance.getType().isKindOf(CISales.RecievingTicket)) {
                    projBaseSel = SelectBuilder.get()
                                    .linkfrom(CIProjects.ProjectService2RecievingTicket.ToLink)
                                    .linkto(CIProjects.ProjectService2RecievingTicket.FromLink);
                } else if (instance.getType().isKindOf(CISales.Reservation)) {
                    projBaseSel = SelectBuilder.get()
                                    .linkfrom(CIProjects.ProjectService2Reservation.ToLink)
                                    .linkto(CIProjects.ProjectService2Reservation.FromLink);
                } else {
                    projBaseSel = getSelectBuilder(_parameter, _instances);
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

                        final String projDataField = getProperty(_parameter, "Project_DataFieldName", "projectData");
                        final String fieldName = getProperty(_parameter, "Project_FieldName", "project");

                        ret.append(getSetFieldValue(0, fieldName, projInst.getOid(),
                                        print.<String>getSelect(projNameSel) + " - "
                                                        + print.<String>getSelect(projDescSel)))
                            .append("\n")
                            .append(getSetFieldValue(0, projDataField,
                                        new Project().getProjectData(_parameter, projInst).toString(), null, true));

                        for (final IOnProject listener : Listener.get().<IOnProject>invoke(IOnProject.class)) {
                            ret.append(listener.add2JavaScript4Project4Document(_parameter, instance, projInst));
                        }
                    }
                }
            }
        }
        return ret;
    }

    /**
     * @param _parameter
     * @return
     */
    protected SelectBuilder getSelectBuilder(final Parameter _parameter,
                                             final List<Instance> _instances)
        throws EFapsException
    {
        // to be used by implementations
        return null;
    }

    @Override
    public int getWeight()
    {
        return 0;
    }
}
