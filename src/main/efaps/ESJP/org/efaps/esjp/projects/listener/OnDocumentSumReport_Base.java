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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.efaps.admin.common.MsgPhrase;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.erp.FilteredReport_Base.InstanceFilterValue;
import org.efaps.esjp.sales.listener.IOnDocumentSumReport;
import org.efaps.esjp.sales.report.DocumentSumGroupedByDate_Base.ValueList;
import org.efaps.util.EFapsException;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("e89020ac-c671-4549-b3e4-92b3696e6665")
@EFapsApplication("eFapsApp-Projects")
public abstract class OnDocumentSumReport_Base
    implements IOnDocumentSumReport
{

    @Override
    public void add2ColumnDefinition(final Parameter _parameter,
                                     final JasperReportBuilder _builder,
                                     final CrosstabBuilder _crosstab)
        throws EFapsException
    {
        // Not used here
    }

    @Override
    public void prepend2ColumnDefinition(final Parameter _parameter,
                                         final JasperReportBuilder _builder,
                                         final CrosstabBuilder _crosstab)
        throws EFapsException
    {
        final Map<String, Object> filter = new FilteredReport().getFilterMap(_parameter);
        boolean project = false;
        if (filter.containsKey("projectGroup")) {
            project = (Boolean) filter.get("projectGroup");
        }
        if (project) {
            final CrosstabRowGroupBuilder<String> projectGroup = DynamicReports.ctab.rowGroup("project", String.class)
                            .setHeaderWidth(150);
            _crosstab.addRowGroup(projectGroup);
        }
    }

    @Override
    public void add2ValueList(final Parameter _parameter,
                              final ValueList _valueList)
        throws EFapsException
    {
        if (!_valueList.isEmpty()) {
            final Map<String, Object> filter = new FilteredReport().getFilterMap(_parameter);
            boolean project = false;
            if (filter.containsKey("projectGroup")) {
                project = (Boolean) filter.get("projectGroup");
            }
            if (project) {
                final Map<Instance, String> map = new HashMap<>();

                final QueryBuilder queryBldr = new QueryBuilder(CIProjects.Project2DocumentAbstract);
                queryBldr.addWhereAttrEqValue(CIProjects.Project2DocumentAbstract.ToAbstract,
                                _valueList.getDocInstances().toArray());

                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selProj = SelectBuilder.get()
                                .linkto(CIProjects.Project2DocumentAbstract.FromAbstract);
                final SelectBuilder selDocInst = SelectBuilder.get()
                                .linkto(CIProjects.Project2DocumentAbstract.ToAbstract)
                                .instance();
                // Project_ProjectMsgPhrase
                final MsgPhrase msgPhrase = MsgPhrase.get(UUID.fromString("64c30826-cb22-4579-a3d5-bd10090f155e"));
                multi.addMsgPhrase(selProj, msgPhrase);
                multi.addSelect(selDocInst);
                multi.execute();
                while (multi.next()) {
                    final Instance docInst = multi.getSelect(selDocInst);
                    final String projectStr = multi.getMsgPhrase(selProj, msgPhrase);
                    map.put(docInst, projectStr);
                }

                for (final Map<String, Object> tmpMap : _valueList) {
                    final Instance docInstance = (Instance) tmpMap.get("docInstance");
                    if (docInstance != null && docInstance.isValid()) {
                        tmpMap.put("project", map.get(docInstance));
                    }
                }
            }
        }
    }

    @Override
    public void add2QueryBuilder(final Parameter _parameter,
                                 final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final Map<String, Object> filter = new FilteredReport().getFilterMap(_parameter);

        if (filter.containsKey("project")) {
            final InstanceFilterValue filterValue = (InstanceFilterValue) filter.get("project");
            if (filterValue != null && filterValue.getObject().isValid()) {
                final QueryBuilder queryBldr = new QueryBuilder(CIProjects.ProjectService2DocumentAbstract);
                queryBldr.addWhereAttrEqValue(CIProjects.ProjectService2DocumentAbstract.FromService,
                                filterValue.getObject());
                _queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID,
                                queryBldr.getAttributeQuery(CIProjects.ProjectService2DocumentAbstract.ToDocument));
            }
        }
    }


    @Override
    public int getWeight()
    {
        return 0;
    }
}
