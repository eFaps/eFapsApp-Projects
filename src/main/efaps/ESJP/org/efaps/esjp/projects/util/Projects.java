/*
 * Copyright 2003 - 2018 The eFaps Team
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

package org.efaps.esjp.projects.util;

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.annotation.EFapsSysConfAttribute;
import org.efaps.api.annotation.EFapsSystemConfiguration;
import org.efaps.esjp.admin.common.systemconfiguration.BooleanSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.StringSysConfAttribute;
import org.efaps.esjp.ci.CINumGenProjects;
import org.efaps.util.cache.CacheReloadException;

/**
 * @author The eFaps Team
 */
@EFapsUUID("7536a95f-c2bb-4e97-beb1-58ef3e75b80a")
@EFapsApplication("eFapsApp-Projects")
@EFapsSystemConfiguration("7536a95f-c2bb-4e97-beb1-58ef3e75b80a")
public final class Projects
{
    /** The base. */
    public static final String BASE = "org.efaps.projects.";

    /** Projects-Configuration. */
    public static final UUID SYSCONFUUID = UUID.fromString("7536a95f-c2bb-4e97-beb1-58ef3e75b80a");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(Projects.BASE + "Activate")
                    .description(" Main switch that permits to activate/deactivate Projects.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ASSIGNWAREHOUSE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Project.AssignWarehouse")
                    .description(" Create and assign a Warehouse on creation of a Project.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PROJECT_NUMGEN = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Project.NumberGenerator")
                    .defaultValue(CINumGenProjects.ProjectServiceSequence.uuid.toString())
                    .description("NumberGenerator for Projects.");

    /**
     * Singelton.
     */
    private Projects()
    {
    }

    /**
     * @return the SystemConfiguration for projects
     * @throws CacheReloadException on error
     */
    public static SystemConfiguration getSysConfig()
        throws CacheReloadException
    {
        // Project-Configuration
        return SystemConfiguration.get(SYSCONFUUID);
    }
}
