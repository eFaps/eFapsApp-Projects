
/*
 * Copyright 2003 - 2013 The eFaps Team
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

package org.efaps.esjp.projects.util;

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.util.cache.CacheReloadException;

/**
 * TODO comment!
 * 
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("7536a95f-c2bb-4e97-beb1-58ef3e75b80a")
@EFapsRevision("$Rev$")
public final class Projects 
{
	/**
	 * Singelton.
	 */
	private Projects() 
	{
	}

	/**
	 * @return the SystemConfigruation for Sales
	 * @throws CacheReloadException
	 *             on error
	 */
	public static SystemConfiguration getSysConfig()
			throws CacheReloadException {
		// Mail-Configuration
		return SystemConfiguration.get(UUID
				.fromString("510dc4fe-86a1-4317-b79d-149cdcf2c748"));
	}
}
