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
package org.efaps.esjp.projects.dashboard;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.ui.IEsjpSnipplet;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ui.html.dojo.charting.Orientation;
import org.efaps.esjp.ui.html.dojo.charting.PieChart;
import org.efaps.esjp.ui.html.dojo.charting.PieData;
import org.efaps.esjp.ui.html.dojo.charting.Serie;
import org.efaps.util.EFapsBaseException;
import org.efaps.util.EFapsException;

/**
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("e62d7348-da53-4ca0-a004-8bedfcaf4fab")
@EFapsApplication("eFapsApp-Projects")
public abstract class ProjectStatusPiePanel_Base
    implements IEsjpSnipplet
{
   /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    public CharSequence getHtmlSnipplet()
        throws EFapsException
    {
        final Map<Status, Integer> values = new HashMap<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIProjects.ProjectService);
        add2QueryBldr(queryBldr);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProjects.ProjectService.Status);
        multi.execute();
        final int all = multi.getInstanceList().size();
        while (multi.next()) {
            final Status status = Status.get(multi.<Long>getAttribute(CIProjects.ProjectService.Status));
            int count = 0;
            if (values.containsKey(status)) {
                count = values.get(status);
            }
            values.put(status, count + 1);
        }
        final PieChart pie = new PieChart();

        final Serie<PieData> serie = new Serie<>();
        pie.setTitle(getTitle());
        pie.addSerie(serie);

        for (final Entry<Status, Integer> entry : values.entrySet()) {
            final PieData data = new PieData();
            serie.addData(data);
            final Integer y = entry.getValue();
            data.setYValue(y);
            data.setText(entry.getKey().getLabel());
            data.setLegend(entry.getKey().getLabel() + ": " + y);
            final BigDecimal percent = new BigDecimal(y).setScale(8)
                            .divide(new BigDecimal(all), BigDecimal.ROUND_HALF_UP)
                            .multiply(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP);
            data.setTooltip(entry.getKey().getLabel() + ": " + y + " / " + percent + "%");
        }
        pie.setOrientation(Orientation.HORIZONTAL_LEGEND_CHART);
        return pie.getHtmlSnipplet();
    }


    protected String getTitle() throws EFapsException
    {
        return DBProperties.getProperty(ProjectStatusPiePanel.class.getName() + ".Title");
    }

    public Return getHtmlSnipplet(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        ret.put(ReturnValues.SNIPLETT, getHtmlSnipplet());
        return ret;
    }

    /**
     * @param _queryBldr
     */
    protected void add2QueryBldr(final QueryBuilder _queryBldr)
        throws EFapsException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isVisible()
        throws EFapsException
    {
        return true;
    }

    @Override
    public String getIdentifier()
        throws EFapsBaseException
    {
        return "ProjectStatusPiePanel";
    }

}
