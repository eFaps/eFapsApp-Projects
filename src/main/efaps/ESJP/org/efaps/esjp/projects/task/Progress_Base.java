/*
 * Copyright 2003 - 2016 The eFaps Team
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
 */

package org.efaps.esjp.projects.task;

import java.util.Map;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.datamodel.ui.UIInterface;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.field.Field.Display;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("ec8afa02-acd5-468b-bb72-af1f5685b777")
@EFapsApplication("eFapsApp-Projects")
public abstract class Progress_Base
{
    /**
     * Get the UoM Fieldvalue for a progress that belongs to a task and
     * therefore only can contain UoM that have the same dimension as the task.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return HTML Snipplet
     * @throws EFapsException on error
     */
    public Return getProgressUoMFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final IUIValue fieldValue = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        final Long uomValue = (Long) fieldValue.getObject();
        final StringBuilder html = new StringBuilder();
        if (Display.EDITABLE.equals(fieldValue.getDisplay())) {
            final Instance instance = _parameter.getInstance();
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            final SelectBuilder sel = new SelectBuilder();
            if (instance.getType().isKindOf(CIProjects.TaskAbstract.getType())) {
                sel.attribute(CIProjects.TaskAbstract.UoM);
            } else {
                sel.linkto(CIProjects.ProgressTaskAbstract.TaskAbstractLink).attribute(CIProjects.TaskAbstract.UoM);
            }
            print.addSelect(sel);
            print.execute();
            final Long uoMId = print.<Long>getSelect(sel);
            if (uoMId == null) {
                html.append(DBProperties.getProperty("org.efaps.esjp.projects.task.Progress.noUoM4Task"));
            } else {
                final Dimension dim = Dimension.getUoM(uoMId).getDimension();

                html.append("<select ").append(UIInterface.EFAPSTMPTAG).append("name=\"")
                    .append(fieldValue.getField().getName()).append("\" size=\"1\">");

                for (final UoM uom : dim.getUoMs()) {
                    html.append("<option value=\"").append(uom.getId());
                    if ((uomValue == null && uom.equals(dim.getBaseUoM()))
                                    || (uomValue != null && uomValue.equals(uom))) {
                        html.append("\" selected=\"selected");
                    }
                    html.append("\">").append(uom.getName()).append("</option>");
                }
                html.append("</select>");
            }
        } else {
            if (uomValue != null) {
                html.append(Dimension.getUoM(uomValue).getName());
            } else {
                html.append(DBProperties.getProperty("org.efaps.esjp.projects.task.Progress.noUoM4Progress"));
            }
        }
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    /**
     * Called to create porgress for various Tasks at once from an StructurBrowser.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return HTML Snipplet
     * @throws EFapsException on error
     */
    public Return create4Tasks(final Parameter _parameter)
        throws EFapsException
    {
        @SuppressWarnings("unchecked")
        final Map<String, String> oid4ui = (Map<String, String>) _parameter.get(ParameterValues.OIDMAP4UI);
        final String[] rowIds = _parameter.getParameterValues(EFapsKey.TABLEROW_NAME.getKey());
        final String[] progress = _parameter.getParameterValues("progress");
        final String date = _parameter.getParameterValue("progressDate");

        for (int i = 0; i < rowIds.length; i++) {
            final String progr = progress[i];
            if (progr != null && !progr.isEmpty()) {
                final Instance taskInst = Instance.get(oid4ui.get(rowIds[i]));
                final PrintQuery print = new PrintQuery(taskInst);
                print.addAttribute(CIProjects.TaskAbstract.UoM);
                print.execute();

                final Insert insert = new Insert(CIProjects.ProgressTaskScheduled);
                insert.add(CIProjects.ProgressTaskAbstract.TaskAbstractLink, taskInst.getId());
                insert.add(CIProjects.ProgressTaskAbstract.Progress, progr);
                insert.add(CIProjects.ProgressTaskAbstract.Date, date);
                insert.add(CIProjects.ProgressTaskAbstract.UoM,
                                print.<Object>getAttribute(CIProjects.TaskAbstract.UoM));
                insert.execute();
            }
        }
        return new Return();
    }
}
