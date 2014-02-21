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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ui.html.dojo.charting.Orientation;
import org.efaps.esjp.ui.html.dojo.charting.PieChart;
import org.efaps.esjp.ui.html.dojo.charting.PieData;
import org.efaps.esjp.ui.html.dojo.charting.Serie;
import org.efaps.ui.wicket.models.IEsjpSnipplet;
import org.efaps.util.EFapsException;

/**
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("e62d7348-da53-4ca0-a004-8bedfcaf4fab")
@EFapsRevision("$Rev$")
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
        final Map<Status, Integer> values = new HashMap<Status, Integer>();
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

        final Serie<PieData> serie = new Serie<PieData>();
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

    /**
     * @return
     */
    protected String getTitle()
    {
        // TODO Auto-generated method stub
        return null;
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

}
