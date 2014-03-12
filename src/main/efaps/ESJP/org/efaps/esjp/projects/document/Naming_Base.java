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

package org.efaps.esjp.projects.document;

import java.util.Date;
import java.util.Formatter;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.projects.util.Projects;
import org.efaps.esjp.projects.util.ProjectsSettings;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("5839cc85-a7be-41d6-bad7-e1a68527d412")
@EFapsRevision("$Rev$")
public abstract class Naming_Base
{

    public static String getName(final Instance _projectInstance,
                                 final Type _docType,
                                 final boolean _orderByID)
        throws EFapsException
    {
        return Naming_Base.getName(_projectInstance, _docType, null, null, _orderByID);
    }

    public static String getName(final Instance _projectInstance,
                                 final Type _docType,
                                 final NumberGenerator _numGen,
                                 final Date _dateGen,
                                 final boolean _orderByID)
        throws EFapsException
    {
        return Naming_Base.getName(_projectInstance, _docType, _numGen, _dateGen,
                        Naming_Base.getLast(_projectInstance, _docType, _orderByID));
    }

    public static String getLast(final Instance _projectInstance,
                                 final Type _docType,
                                 final boolean _orderByID)
        throws EFapsException
    {
        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProjects.Project2DocumentAbstract);
        attrQueryBldr.addWhereAttrEqValue(CIProjects.Project2DocumentAbstract.FromAbstract, _projectInstance.getId());
        final AttributeQuery attrQUery = attrQueryBldr.getAttributeQuery(
                        CIProjects.Project2DocumentAbstract.ToAbstract);

        final QueryBuilder queryBldr = new QueryBuilder(_docType);
        queryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.ID, attrQUery);
        if (_orderByID) {
            queryBldr.addOrderByAttributeDesc(CIERP.DocumentAbstract.ID);
        } else {
            queryBldr.addOrderByAttributeDesc(CIERP.DocumentAbstract.Name);
        }

        final InstanceQuery query = queryBldr.getQuery();
        query.setLimit(1);
        query.setIncludeChildTypes(false);
        final MultiPrintQuery multi = new MultiPrintQuery(query.execute());
        multi.addAttribute(CIERP.DocumentAbstract.Name);
        multi.executeWithoutAccessCheck();

        String ret = null;
        while (multi.next()) {
            ret = multi.<String>getAttribute(CIERP.DocumentAbstract.Name);
        }
        return ret;
    }

    public static String getName(final Instance _projectInstance,
                                 final Type _docType,
                                 final NumberGenerator _numGen,
                                 final Date _dateGen,
                                 final String _last)
        throws EFapsException
    {
        final SystemConfiguration config = Projects.getSysConfig();
        final Properties props = config.getAttributeValueAsProperties(ProjectsSettings.NAMING, true);
        String ret = props.getProperty(_docType.getName() + ".Name", "%s-%s");

        final PrintQuery print = new PrintQuery(_projectInstance);
        print.addAttribute(CIProjects.ProjectService.Name);
        print.executeWithoutAccessCheck();
        final String projectName = print.<String>getAttribute(CIProjects.ProjectService.Name);

        Integer number = 1;
        if (_last != null) {
            final String regex = props.getProperty(_docType.getName() + ".RegEx", "\\d*$");
            final Pattern pattern = Pattern.compile(regex);
            final Matcher matcher = pattern.matcher(_last);
            matcher.find();
            final String prev = matcher.group();
            if (prev != null) {
                final int i = Integer.parseInt(prev);
                number = i + 1;
            }
        }
        final Formatter formatter = new Formatter();
        if (_numGen == null) {
            formatter.format(ret, projectName, number);
        } else {
            final String val;
            if (_dateGen != null) {
                val = _numGen.getNextVal(_dateGen);
            } else {
                val = _numGen.getNextVal();
            }
            formatter.format(ret, projectName, number, val);
        }
        ret = formatter.toString();
        formatter.close();
        return ret;
    }
}
