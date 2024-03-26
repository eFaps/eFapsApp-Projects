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
package org.efaps.esjp.projects.task;

import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ui.structurbrowser.StandartStructurBrowser;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * 
 */
@EFapsUUID("d8664446-22f0-4d7f-af76-0e5137889d87")
@EFapsApplication("eFapsApp-Projects")
public abstract class TaskStructurBrowser_Base
    extends StandartStructurBrowser
{

    /**
     * {@inheritDoc}
     */
    @Override
    protected Return checkHideColumn4Row(final Parameter _parameter)
    {
        return new Return();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addCriteria(final Parameter _parameter,
                               final QueryBuilder _queryBldr)
        throws EFapsException
    {
        _queryBldr.addWhereAttrIsNull(CIProjects.TaskAbstract.ParentTaskAbstractLink);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Return onNodeRemove(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        if (instance != null && instance.isValid()) {
            @SuppressWarnings("unchecked")
            final Map<String, String> oidMap = (Map<String, String>) _parameter.get(ParameterValues.OIDMAP4UI);
            oidMap.put(instance.getOid(), "delete");
        }
        return super.onNodeRemove(_parameter);
    }

}
