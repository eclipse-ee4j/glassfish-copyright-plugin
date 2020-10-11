/*
 * Copyright (c) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 Contributors to Eclipse Foundation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.copyright;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Repairs copyrights of files.
 */
@Mojo(name = "repair", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class RepairCopyrightMojo extends AbstractCopyrightMojo {
    /**
     * Set to false to disable updating files in place.
     */
    @Parameter(property = "copyright.update", defaultValue = "true")
    private boolean update = true;

    public void execute() throws MojoExecutionException {
	log = getLog();

	Copyright c = new Copyright();
	c.doRepair = true;
	c.dontUpdate = !update;
	log.debug("copyright: update: " + update);
	initializeOptions(c);

	check(c);
    }
}
