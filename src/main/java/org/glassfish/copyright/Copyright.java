/*
 * Copyright (c) 2010, 2019 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Check that the copyright/license notice in a file is the correct one.
 * Optionally repair any that are wrong.
 *
 * Usage: java -jar copyright.jar
 *		[-w] -[y] [-r] [-n] [-s] [-h] [-m] [-g] [-S] [-c] [-q] [-j] [-x]
 *		[-p] [-t] [-e] [-N] [-D] [-X pat] [-C file] [-A file] [-B file] [-P]
 *		[-v] [-V] [files ...]
 *
 * Options:
 *	-w	suppress warnings
 *	-y	don't check that year is correct (much faster)
 *	-r	repair files that are wrong
 *	-n	with -r, leave the updated file in file.new
 *	-s	skip files not under source control (slower)
 *	-h	check hidden files too
 *	-m	use Mercurial
 *	-g	use git (default)
 *	-S	use SVN
 *	-c	count errors and print summary
 *	-q	don't print errors for each file
 *	-j	check Java syntax files
 *	-x	check XML syntax files
 *	-p	check properties syntax files
 *	-t	check other text files
 *	-e	exclude files that contains DO NOT ALTER OR REMOVE COPYRIGHT NOTICES
 *	-N	normalize format of repaired copyright to match template
 *	-D	use dash instead of comma in years when repairing files
 *	-X	exclude files matching pat (substring only)
 *	-C	file containing correct copyright template, using Java syntax
 *	-A	file(s) containing alternate correct copyright template(s)
 *	-B	file containing correct BSD copyright template
 *	-P	preserve original copyrights
 *	-v	verbose output
 *	-V	print version number
 *
 * @author	Bill Shannon
 */

package org.glassfish.copyright;

import java.io.*;
import java.util.*;

public class Copyright {

    public boolean debug = false;
    public boolean warn = true;
    public boolean ignoreYear = false;
    public boolean useDash = false;
    public boolean doRepair = false;
    public boolean dontUpdate = false;
    public boolean normalize = false;
    public boolean skipNoSVN = false;
    public boolean doHidden = false;
    public boolean mercurial = false;
    public boolean git = true;
    public static boolean count = false;
    public boolean quiet = false;
    public boolean doJava = false;
    public boolean doXml = false;
    public boolean doProps = false;
    public boolean doText = false;
    public boolean preserveCopyrights = false;
    public boolean verbose = false;
    public boolean explicitExclude = false;
    public File correctTemplate;
    public List<File> alternateTemplates = new ArrayList<File>();
    public File correctBSDTemplate;

    public int nMissing;
    public int nEmpty;
    public int nSun;
    public int nSunApache;
    public int nSunBSD;
    public int nOldBSD;
    public int nOldCDDL;
    public int nCDDLGPLCE;
    public int nNoCE;
    public int nWrong;
    public int nNoYear;
    public int nDate;
    public int errors;
    public boolean sawUnknown;

    public List<String> excludes = new ArrayList<String>();

    private AbstractCopyright javaCopyright;
    private AbstractCopyright xmlCopyright;
    private AbstractCopyright textCopyright;
    private AbstractCopyright sigCopyright;
    private AbstractCopyright propsCopyright;
    private AbstractCopyright batCopyright;
    private AbstractCopyright mdCopyright;
    private AbstractCopyright adCopyright;
    private AbstractCopyright jspCopyright;

    public static final List<String> ignoredDirs =
		    Collections.unmodifiableList(
			Arrays.asList(".m2", ".svn", ".hg", ".git", "target"));

    private void init() {
	if (javaCopyright == null) {
	    javaCopyright = new JavaCopyright(this);
	    xmlCopyright = new XmlCopyright(this);
	    textCopyright = new TextCopyright(this);
	    sigCopyright = new SigCopyright(this);
	    propsCopyright = new PropertiesCopyright(this);
	    batCopyright = new BatCopyright(this);
	    mdCopyright = new MarkdownCopyright(this);
	    adCopyright = new AsciidocCopyright(this);
	    jspCopyright = new JspCopyright(this);

	    if (!doJava && !doXml && !doProps && !doText)
		// by default, do them all
		doJava = doXml = doProps = doText = true;
	}
    }

    /**
     * Check the file.  If the file is a directory, recurse.
     */
    public void check(File file) throws IOException {
	init();
	check(file, false);
    }

    /**
     * Check a Maven project directory.
     * Skip subdirectories that contain a pom.xml file.
     */
    public void checkMaven(File file) throws IOException {
	init();
	if (!file.exists()) {
	    System.out.println(file + ": doesn't exist");
	    return;
	}
	if (!file.canRead()) {
	    System.out.println(file + ": can't read");
	    return;
	}
	if (!doHidden && file.isHidden() && !file.getName().equals("."))
	    return;
	if (file.isDirectory()) {
	    String name = file.getName();
	    if (ignoredDirs.contains(name))
		return;
	    File[] files = file.listFiles();
	    for (File f : files)
		check(f, true);
	} else
	    checkFile(file);
    }

    /**
     * Check the file.  If the file is a directory, recurse.
     * If skipMavenDir is true, skip directories that contain
     * a pom.xml file.
     */
    private void check(File file, boolean skipMavenDir) throws IOException {
	if (!file.exists()) {
	    System.out.println(file + ": doesn't exist");
	    return;
	}
	if (!file.canRead()) {
	    System.out.println(file + ": can't read");
	    return;
	}
	if (!doHidden && file.isHidden() && !file.getName().equals(".")) {
	    if (verbose)
		System.out.println("Hidden file skipped: " + file);
	    return;
	}
	if (file.isDirectory()) {
	    String name = file.getName();
	    if (ignoredDirs.contains(name)) {
		if (verbose)
		    System.out.println("Ignored directory skipped: " + file);
		return;
	    }
	    if (skipMavenDir) {
		File pom = new File(file, "pom.xml");
		if (pom.exists()) {
		    if (verbose)
			System.out.println("Maven subproject skipped: " + file);
		    return;
		}
	    }
	    File[] files = file.listFiles();
	    for (File f : files)
		check(f);
	} else
	    checkFile(file);
    }

    /**
     * Check the copyright in the named file.
     */
    private void checkFile(File file) throws IOException {
	// ignore empty files
	if (file.length() == 0) {
	    if (verbose)
		System.out.println("Empty file, skipped: " + file);
	    return;
	}

	String pname = file.getPath();
	for (String ex : excludes) {
	    if (pname.indexOf(ex) >= 0) {
		if (verbose)
		    System.out.println("Excluded by pattern \"" + ex +
					"\": " + pname);
		return;
	    }
	}
	if (javaCopyright.supports(file)) {
	    if (debug)
		System.out.println("File " + file + " is a Java file");
	    if (doJava)
		javaCopyright.checkCopyright(file);
	} else if (jspCopyright.supports(file)) {
	    if (debug)
		System.out.println("File " + file + " is a JSP file");
	    if (doXml)
		jspCopyright.checkCopyright(file);
	} else if (xmlCopyright.supports(file)) {
	    if (debug)
		System.out.println("File " + file + " is an XML file");
	    if (doXml)
		xmlCopyright.checkCopyright(file);
	} else if (batCopyright.supports(file)) {
	    if (debug)
		System.out.println("File " + file + " is a BAT file");
	    if (doText)
		batCopyright.checkCopyright(file);
	} else if (mdCopyright.supports(file)) {
	    if (debug)
		System.out.println("File " + file + " is a markdown file");
	    if (doText)
		mdCopyright.checkCopyright(file);
	} else if (adCopyright.supports(file)) {
	    if (debug)
		System.out.println("File " + file + " is an asciidoc file");
	    if (doText)
		adCopyright.checkCopyright(file);
	} else if (sigCopyright.supports(file)) {
	    if (debug)
		System.out.println("File " + file + " is a signature file");
	    if (doProps)
		sigCopyright.checkCopyright(file);
	} else if (propsCopyright.supports(file)) {
	    if (debug)
		System.out.println("File " + file + " is a properties file");
	    if (doProps)
		propsCopyright.checkCopyright(file);
	} else {
	    if (debug)
		System.out.println("File " + file + " is a text file");
	    if (doText)
		textCopyright.checkCopyright(file);
	}
    }

    public void addExclude(String ex) {
	if (ex == null || ex.length() == 0)
	    return;
	if (debug)
	    System.out.println("Add exclude: " + ex);
	excludes.add(ex);
    }

    public void addExcludes(String file) throws IOException {
	BufferedReader r = null;
	try {
	    r = new BufferedReader(new FileReader(file));
	    String line;
	    while ((line = r.readLine()) != null) {
		if (line.trim().startsWith("#"))
		    continue;		// ignore comment lines
		addExclude(line);
	    }
	} finally {
	    try {
		if (r != null)
		    r.close();
	    } catch (IOException ioex) { }
	}
    }

    public static void main(String[] argv) throws Exception {
	Copyright c = new Copyright();

	int optind;
	for (optind = 0; optind < argv.length; optind++) {
	    if (argv[optind].equals("-d")) {
		c.debug = true;
	    } else if (argv[optind].equals("-w")) {
		c.warn = false;
	    } else if (argv[optind].equals("-y")) {
		c.ignoreYear = true;
	    } else if (argv[optind].equals("-N")) {
		c.normalize = true;
	    } else if (argv[optind].equals("-D")) {
		c.useDash = true;
	    } else if (argv[optind].equals("-r")) {
		c.doRepair = true;
	    } else if (argv[optind].equals("-n")) {
		c.dontUpdate = true;
	    } else if (argv[optind].equals("-s")) {
		c.skipNoSVN = true;
	    } else if (argv[optind].equals("-h")) {
		c.doHidden = true;
	    } else if (argv[optind].equals("-m")) {
		c.mercurial = true;
	    } else if (argv[optind].equals("-g")) {
		c.git = true;
	    } else if (argv[optind].equals("-S")) {
		c.git = false;
	    } else if (argv[optind].equals("-c")) {
		count = true;
	    } else if (argv[optind].equals("-q")) {
		c.quiet = true;
	    } else if (argv[optind].equals("-j")) {
		c.doJava = true;
	    } else if (argv[optind].equals("-x")) {
		c.doXml = true;
	    } else if (argv[optind].equals("-p")) {
		c.doProps = true;
	    } else if (argv[optind].equals("-t")) {
		c.doText = true;
	    } else if (argv[optind].equals("-e")) {
		c.explicitExclude = true;
	    } else if (argv[optind].equals("-X")) {
		String ex = argv[++optind];
		if (ex.startsWith("@"))
		    c.addExcludes(ex.substring(1));
		else
		    c.addExclude(ex);
	    } else if (argv[optind].equals("-C")) {
		c.correctTemplate = new File(argv[++optind]);
	    } else if (argv[optind].equals("-A")) {
		for (String alt : argv[++optind].split(File.pathSeparator))
		    c.alternateTemplates.add(new File(alt));
	    } else if (argv[optind].equals("-B")) {
		c.correctBSDTemplate = new File(argv[++optind]);
	    } else if (argv[optind].equals("-P")) {
		c.preserveCopyrights = true;
	    } else if (argv[optind].equals("-v")) {
		c.verbose = true;
	    } else if (argv[optind].equals("-V")) {
		System.out.println("Version: " + Version.getVersion());
		System.exit(0);
	    } else if (argv[optind].equals("--")) {
		optind++;
		break;
	    } else if (argv[optind].startsWith("-")) {
		System.out.println("Usage: copyright " +
		    "[-w] [-y] [-r] [-n] [-s] [-h] [-m] [-c] [-S] [-q] [-j] " +
		    "[-x] [-p] [-t] [-N] [-D] [-V] [-X pat] [-C file] " +
                    "[-A file(s)] [-B file] [-P] [-v] [files...]");
		System.out.println("\t-w\tsuppress warnings");
		System.out.println("\t-y\tdon't check that year is correct " +
				    "(much faster)");
		System.out.println("\t-r\trepair files that are wrong");
		System.out.println("\t-n\twith -r, leave the updated file in " +
				    "file.new");
		System.out.println("\t-s\tskip files not under source " +
				    "control (slower)");
		System.out.println("\t-h\tcheck hidden files too");
		System.out.println("\t-m\tuse Mercurial");
		System.out.println("\t-g\tuse Git (default)");
		System.out.println("\t-S\tuse SVN");
		System.out.println("\t-c\tcount errors and print summary");
		System.out.println("\t-q\tdon't print errors for each file");
		System.out.println("\t-j\tcheck Java syntax files");
		System.out.println("\t-x\tcheck XML syntax files");
		System.out.println("\t-p\tcheck properties syntax files");
		System.out.println("\t-t\tcheck other text files");
		System.out.println("\t-N\tnormalize format of repaired " +
                                    "copyright to match template");
		System.out.println("\t-D\tdash instead of comma between years");
		System.out.println("\t-X\texclude files matching pat " +
				    "(substring only)");
		System.out.println("\t-C\tfile containing correct copyright " +
				    "template, using Java syntax");
		System.out.println("\t-A\tfile(s) containing alternate " +
				    "correct copyright template(s)");
		System.out.println("\t-B\tfile containing correct BSD " +
				    "copyright template");
		System.out.println("\t-P\tpreserve original copyrights");
		System.out.println("\t-v\tverbose output");
		System.out.println("\t-V\tprint version number");
		System.exit(-1);
	    } else {
		break;
	    }
	}

	if (optind >= argv.length)
	    c.check(new File("."));
	else
	    while (optind < argv.length)
		c.check(new File(argv[optind++]));

	if (count)
	    summary(c);
	System.exit(c.errors);
    }

    /**
     * Print a summary of errors.
     */
    private static void summary(Copyright c) {
	if (c.errors == 0) {
	    System.out.println("No errors");
	    return;
	}

	if (!c.quiet)
	    System.out.println();

	if (c.nMissing > 0)
	    System.out.println("No Copyright:\t\t" + c.nMissing);
	if (c.nEmpty > 0)
	    System.out.println("Empty Copyright:\t" + c.nEmpty);
	if (c.nSun > 0)
	    System.out.println("Sun Copyright:\t" + c.nSun);
	if (c.nSunApache > 0)
	    System.out.println("Sun+Apache Copyright:\t" + c.nSunApache);
	if (c.nSunBSD > 0)
	    System.out.println("Sun BSD Copyright:\t" + c.nSunBSD);
	if (c.nOldBSD > 0)
	    System.out.println("Old BSD Copyright:\t" + c.nOldBSD);
	if (c.nOldCDDL > 0)
	    System.out.println("Old CDDL Copyright:\t" + c.nOldCDDL);
	if (c.nCDDLGPLCE > 0)
	    System.out.println("CDL+GPL+CE Copyright:\t" + c.nCDDLGPLCE);
	if (c.nNoCE > 0)
	    System.out.println("Copyright without CE:\t" + c.nNoCE);
	if (c.nWrong > 0)
	    System.out.println("Wrong Copyright:\t" + c.nWrong);
	if (c.nNoYear > 0)
	    System.out.println("No Copyright Year:\t" + c.nNoYear);
	if (!c.ignoreYear && c.nDate > 0)
	    System.out.println("Wrong Copyright Date:\t" + c.nDate);
    }
}
