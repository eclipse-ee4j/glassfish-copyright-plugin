/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.model.Resource;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceCreationException;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Check copyrights of files.
 */
public abstract class AbstractCopyrightMojo extends AbstractMojo {
    /**
     * File(s) containing exclude patterns. <p>
     *
     * This parameter is resolved as a resource, then a URL, then a file.
     * This is a comma-separated list.
     */
    @Parameter(property = "copyright.exclude")
    protected String excludeFile;

    /**
     * Exclude pattern list.
     */
    @Parameter
    protected String[] exclude;

    /**
     * Base directory for project.
     * Should not need to be set.
     */
    @Parameter(defaultValue = "${project.basedir}")
    protected File baseDirectory;

    /**
     * Source directory.
     *
    @Parameter(defaultValue = "${project.build.sourceDirectory}"
     */
    protected File sourceDirectory;

    /**
     * Resources.
     */
    @Parameter(defaultValue = "${project.resources}")
    protected List<Resource> resources;

    /**
     * Check all files in project or just the source and resource
     * files maven knows about?
     */
    @Parameter(property = "copyright.mavenonly")
    protected boolean mavenOnly;

    /**
     * Select SCM system - git (default), mercurial, svn.
     */
    @Parameter(property = "copyright.scm")
    protected String scm;

    /**
     * Turn on debugging.
     */
    @Parameter(property = "copyright.debug")
    protected boolean debug;

    /**
     * Turn off warnings.
     */
    @Parameter(property = "copyright.warn", defaultValue = "true")
    protected boolean warn = true;

    /**
     * Don't check that the year is correct?
     */
    @Parameter(property = "copyright.ignoreyear")
    protected boolean ignoreYear;

    /**
     * Normalize format of repaired copyright to match template?
     */
    @Parameter(property = "copyright.normalize")
    protected boolean normalize;

    /**
     * Use dash instead of comma between years?
     */
    @Parameter(property = "copyright.usedash")
    protected boolean useDash;

    /**
     * Set to true to preserve original copyright entries.
     */
    @Parameter(property = "copyright.preservecopyrights")
    protected boolean preserveCopyrights;


    /**
     * Skip files not under SCM?
     */
    @Parameter(property = "copyright.scmonly")
    protected boolean scmOnly;

    /**
     * Check hidden files too?
     *
    @Parameter(property = "copyright.hidden"
     */
    protected boolean doHidden;

    /**
     * Copyright template file.
     */
    @Parameter(property = "copyright.template")
    protected String templateFile;

    /**
     * Alternate copyright template file.
     */
    // XXX - for compatibility
    @Parameter(property = "copyright.alternatetemplate")
    protected String alternateTemplateFile;

    /**
     * Alternate copyright template files.
     */
    @Parameter(property = "copyright.alternatetemplates")
    protected List<String> alternateTemplateFiles;

    /**
     * Copyright BSD template file.
     */
    @Parameter(property = "copyright.bsdtemplate")
    protected String bsdTemplateFile;

    /**
     * Log output, initialize this in the execute method.
     */
    protected Log log;

    /**
     * @component
     * @required
     * @readonly
     */
    @Component
    private ResourceManager resourceManager;

    /**
     * Initialize the Copyright object with the options from this mojo.
     */
    protected void initializeOptions(Copyright c) {
	if (excludeFile != null) {
	    String[] files = excludeFile.split(",");
	    for (String file : files) {
		log.debug("copyright: exclude file: " + file);
		String rfile = getResourceFile(file).getPath();
		try {
		    c.addExcludes(rfile);
		} catch (IOException ex) {
		    log.warn("Failed to add excludes from file: " + file, ex);
		}
	    }
	}
	if (exclude != null) {
	    for (String ex : exclude) {
		log.debug("copyright: exclude pattern: " + ex);
		c.addExclude(ex);
	    }
	}

	if (scm == null || scm.equalsIgnoreCase("git"))
	    ;	// nothing to do, default case
	else if (scm.equalsIgnoreCase("mercurial") ||
		    scm.equalsIgnoreCase("hg"))
	    c.mercurial = true;
	else if (scm.equalsIgnoreCase("svn"))
	    c.git = false;
	else
	    log.warn("Unknown SCM system ignored: " + scm);

	c.debug = debug;
	c.warn = warn;
	c.ignoreYear = ignoreYear;
	c.normalize = normalize;
	c.useDash = useDash;
	c.preserveCopyrights = preserveCopyrights;
	c.skipNoSVN = scmOnly;
	c.doHidden = doHidden;

	if (templateFile != null)
	    c.correctTemplate = 
		new File(getResourceFile(templateFile).getPath());
	if (alternateTemplateFile != null) {
	    c.alternateTemplates.add(
		new File(getResourceFile(alternateTemplateFile).getPath()));
	}
	if (alternateTemplateFiles != null)
	    for (String alt : alternateTemplateFiles)
		c.alternateTemplates.add(
		    new File(getResourceFile(alt).getPath()));
	if (bsdTemplateFile != null)
	    c.correctBSDTemplate = 
		new File(getResourceFile(bsdTemplateFile).getPath());
    }

    /**
     * Run the copyright checker using the specified options
     * on the specified files in this project.
     */
    protected void check(Copyright c) throws MojoExecutionException {
	try {
	    if (mavenOnly)
		checkMaven(c);
	    else
		checkAll(c);
	} catch (IOException ioex) {
	    log.error("IOException: " + ioex);
	    throw new MojoExecutionException(
			    "IOException while checking copyrights", ioex);
	}
    }

    /**
     * Only check the source files and resource files, and the pom.xml.
     */
    private void checkMaven(Copyright c) throws IOException {
	/*
	 * This seems like the right way, but it misses many files
	 * in the project directory.
	 */
	log.debug("copyright: base directory: " + baseDirectory);
	if (baseDirectory.exists()) {
	    File pom = new File(baseDirectory, "pom.xml");
	    c.check(pom);
	}
	log.debug("copyright: source directory: " + sourceDirectory);
	if (sourceDirectory.exists())
	    c.check(sourceDirectory);

	if (resources != null) {
	    /*
	     * Iterate over all the resources, and all the files in each
	     * resource, taking into account any includes and excludes.
	     */
	    for (Resource r : resources) {
		File dir = new File(r.getDirectory());
		List<String> incl = r.getIncludes();
		List<String> excl = r.getExcludes();
		if (log.isDebugEnabled()) {
		    log.debug("copyright: resource directory: " + dir);
		    log.debug("copyright:   includes: " + incl);
		    log.debug("copyright:   excludes: " + excl);
		}
		// XXX - need to add the ignored directories to the exclude
		// list, otherwise FileUtils.getFiles will return files in
		// those directories
		for (String ig : Copyright.ignoredDirs)
		    excl.add("**/" + ig + "/**");
		if (dir.exists()) {
		    List<File> files = FileUtils.getFiles(dir,
						commaSeparated(incl),
						commaSeparated(excl), true);
		    if (log.isDebugEnabled())
			log.debug("copyright:   files: " + files);
		    for (File f : files)
			c.check(f);
		}
	    }
	}
    }

    /**
     * Check all the files in the project, skipping files in
     * subdirectories that are also maven projects.
     */
    private void checkAll(Copyright c) throws IOException {
	/*
	 * The simple way - just check every file in the project.
	 */
	log.debug("copyright: base directory: " + baseDirectory);
	if (baseDirectory.exists())
	    c.checkMaven(baseDirectory);
    }

    /**
     * Get the File reference for a File passed in as a string reference.
     *
     * @param resource
     *            The file for the resource manager to locate
     * @return The File of the resource
     *
     */
    protected File getResourceFile(String resource) {

	assert resource != null;

	log.debug("resource is " + resource);

	try {
	    File resourceFile = resourceManager.getResourceAsFile(resource);
	    log.debug("copyright: location of file is " + resourceFile);
	    return resourceFile;
	} catch (ResourceNotFoundException ex) {
	    return new File(resource);
	} catch (FileResourceCreationException ex) {
	    return new File(resource);
	}
    }

    /**
     * Convert a list of strings into a comma separated list in a single string.
     */
    private static String commaSeparated(List<String> l) {
	if (l == null || l.size() == 0)
	    return null;
	StringBuilder sb = new StringBuilder();
	for (String s : l) {
	    if (sb.length() > 0)
		sb.append(',');
	    sb.append(s);
	}
	return sb.toString();
    }
}
