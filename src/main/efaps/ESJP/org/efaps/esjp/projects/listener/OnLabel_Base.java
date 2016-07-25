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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.accounting.listener.IOnLabel;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("97ff2c4a-0e28-421f-9787-2f3a20188ec5")
@EFapsApplication("eFapsApp-Projects")
public abstract class OnLabel_Base
    implements IOnLabel
{
    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _instance of the document to be evaluated
     * @return collection of instances
     * @throws EFapsException on error
     */
    @Override
    public Collection<? extends Instance> evalLabelsForDocument(final Parameter _parameter,
                                                                final Instance _instance)
        throws EFapsException
    {
        final List<Instance> ret = new ArrayList<>();
        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProjects.Project2DocumentAbstract);
        attrQueryBldr.addWhereAttrEqValue(CIProjects.Project2DocumentAbstract.ToAbstract, _instance);

        final QueryBuilder queryBldr = new QueryBuilder(CIProjects.ProjectService2Label);
        queryBldr.addWhereAttrInQuery(CIProjects.ProjectService2Label.FromLink,
                        attrQueryBldr.getAttributeQuery(CIProjects.Project2DocumentAbstract.FromAbstract));
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selInst = SelectBuilder.get().linkto(CIProjects.ProjectService2Label.ToLink).instance();
        multi.addSelect(selInst);
        multi.execute();
        while (multi.next()) {
            ret.add(multi.<Instance>getSelect(selInst));
        }
        return ret;
    }

    @Override
    public int getWeight()
    {
        return 0;
    }
}
