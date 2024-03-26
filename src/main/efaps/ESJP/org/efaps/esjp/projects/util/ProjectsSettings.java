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
package org.efaps.esjp.projects.util;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("eac97366-119e-4797-ad33-6c7481a9efce")
@EFapsApplication("eFapsApp-Projects")
public interface ProjectsSettings
{
    /**
     * Base String for settings in projects.
     */
    String BASE = "org.efaps.projects.";

    /**
     * Properties.<br/>
     * Can be concatenated.<br/>
     * Set a Price List for a Type used to Calculator. Used for Sales Documents.
     */
    String NAMING = ProjectsSettings.BASE + "Naming";

    /**
     * Properties.<br/>
     * Can be concatenated.<br/>
     * Configure the Connections to projects.
     */
    String CONNECT2DOC = ProjectsSettings.BASE + "Connect2Doc";
}
