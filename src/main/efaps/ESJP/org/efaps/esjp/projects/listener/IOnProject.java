/*
 * Copyright 2003 - 2015 The eFaps Team
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
package org.efaps.esjp.projects.listener;

import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.IEsjpListener;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

/**
 * The Interface IOnProject.
 *
 * @author The eFaps Team
 */
@EFapsUUID("47d9e38b-c583-4097-b3fc-42893cbf0b0b")
@EFapsApplication("eFapsApp-Projects")
public interface IOnProject
    extends IEsjpListener
{

    /**
     * Update field 4 project.
     *
     * @param _parameter the _parameter
     * @param _projectInstance the _project instance
     * @param _uiMap the _ui map
     * @throws EFapsException on error
     */
    void updateField4Project(final Parameter _parameter,
                             final Instance _projectInstance,
                             final Map<String, Object> _uiMap)
       throws EFapsException;

    /**
     * Add2 java script4 project4 document.
     *
     * @param _parameter the _parameter
     * @param _docInstance the doc instance
     * @param _projectInstance the project instance
     * @return the char sequence
     * @throws EFapsException on error
     */
    CharSequence add2JavaScript4Project4Document(final Parameter _parameter,
                                                 final Instance _docInstance,
                                                 final Instance _projectInstance)
        throws EFapsException;
}
