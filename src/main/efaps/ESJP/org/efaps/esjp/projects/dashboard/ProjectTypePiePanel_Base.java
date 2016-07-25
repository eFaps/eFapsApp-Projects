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

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.ui.IEsjpSnipplet;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
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
@EFapsUUID("8d254e40-5892-48a6-bf27-59c5b532abd0")
@EFapsApplication("eFapsApp-Projects")
public abstract class ProjectTypePiePanel_Base
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
        final Map<String, Integer> values = new HashMap<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIProjects.ProjectService);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder sel = SelectBuilder.get().linkto(CIProjects.ProjectService.ProjectTypeLink)
                        .attribute(CIProjects.AttributeDefinitionProjectType.Value);
        multi.addSelect(sel);
        multi.execute();
        final int all = multi.getInstanceList().size();
        while (multi.next()) {
            final String proType = multi.<String>getSelect(sel);
            int count = 0;
            if (values.containsKey(proType)) {
                count = values.get(proType);
            }
            values.put(proType, count + 1);
        }
        final PieChart pie = new PieChart();

        final Serie<PieData> serie = new Serie<>();
        pie.addSerie(serie);

        for (final Entry<String, Integer> entry : values.entrySet()) {
            final PieData data = new PieData();
            serie.addData(data);
            final Integer y = entry.getValue();
            data.setYValue(y);
            data.setText(entry.getKey());
            data.setLegend(entry.getKey() + ": " + y);
            final BigDecimal percent = new BigDecimal(y).setScale(8)
                            .divide(new BigDecimal(all), BigDecimal.ROUND_HALF_UP)
                            .multiply(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP);
            data.setTooltip(entry.getKey() + ": " + y + " / " + percent + "%");
        }
        pie.setOrientation(Orientation.HORIZONTAL_LEGEND_CHART);
        return pie.getHtmlSnipplet();
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
        return "ProjectTypePiePanel";
    }
}

