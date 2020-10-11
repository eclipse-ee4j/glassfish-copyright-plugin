/*
 * Copyright (c) 2010, 2018 Oracle and/or its affiliates. All rights reserved.
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
 * Support for files with .properties file syntax (# comments).
 *
 * @author	Bill Shannon
 */

package org.glassfish.copyright;

import java.io.*;
import java.util.regex.*;

public class PropertiesCopyright extends AbstractCopyright {
    public PropertiesCopyright(Copyright c) {
	super(c);
    }

    /**
     * Is this a properties file, or other file with similar syntax?
     */
    protected boolean supports(File file) {
	String fname = file.getName();
	if ((fname.endsWith(".properties") || fname.endsWith(".prefs") ||
		    fname.endsWith(".py") ||
		    fname.startsWith("Makefile") ||
		    fname.startsWith("GNUmakefile") ||
		    fname.startsWith("Rakefile") ||
		    fname.equals("osgi.bundle") ||
		    fname.endsWith(".sh") || fname.endsWith(".ksh"))) {
	    return true;
	}
	if (startsWith(file, "#"))
	    return true;
	return false;
    }

    /**
     * Read the first comment block in the file.
     */
    protected String readComment(BufferedReader r) throws IOException {
	StringBuilder comment = new StringBuilder();
	String line;
	// skip blank lines at beginning of file
	while ((line = r.readLine()) != null) {
	    line = strip(line);
	    if (skipHeaderLine(line))
		continue;
	    if (line.equals("#"))
		continue;
	    if (line.length() != 0)
		break;
	}
	if (line == null || !line.startsWith("#"))
	    return null;
	String prefix = "# ";
	do {
	    if (line.length() == 0)
		break;		// end of comment
	    if (!line.startsWith("#"))
		break;		// end of comment
	    if (line.length() >= prefix.length()) {
		if (line.startsWith(prefix))
		    line = line.substring(prefix.length());
	    } else {
		if (prefix.startsWith(line))
		    line = "";
	    }
	    comment.append(strip(line)).append('\n');
	} while ((line = r.readLine()) != null);
	int len = comment.length();
	if (len >= 2 && comment.charAt(len - 1) == '\n' &&
		comment.charAt(len - 2) == '\n')
	    comment.setLength(len - 1);
	return comment.toString();
    }

    /**
     * Skip the first comment block, replacing it with the correct copyright.
     */
    protected void replaceCopyright(BufferedReader in,
			BufferedWriter out, String comment, String lastChanged)
			throws IOException {
	String line;
	StringBuilder header = new StringBuilder();
	// skip blank lines at beginning of file
	while ((line = in.readLine()) != null) {
	    line = strip(line);
	    if (skipHeaderLine(line)) {
		header.append(line).append('\n');
		continue;
	    }
	    if (comment != null && line.equals("#"))
		continue;
	    if (line.length() != 0)
		break;
	}

	if (header.length() > 0)
	    out.write(header.toString());
	if (comment != null && line != null && line.startsWith("#")) {
	    boolean sawCopyright = false;
	    do {
		if (line.length() == 0)
		    break;		// end of comment
		if (!line.startsWith("#"))
		    break;		// end of comment
		if (!sawCopyright && line.indexOf("Copyright") >= 0) {
		    Matcher m = ypat.matcher(line);
		    if (m.find()) {
			lastChanged = addCopyrightDate(m.group(ypat_YEAR),
							lastChanged);
			sawCopyright = true;
		    }
		}
	    } while ((line = in.readLine()) != null);
	}
	writeCopyright(out, lastChanged, comment);

	if (line != null) {
	    // the new copyright ends with a blank line so don't write another
	    if (line.length() > 0) {
		out.write(line);
		out.write('\n');		// line terminator
	    }
	    // have to copy the rest here so that blanks aren't skipped
	    copy(in, out, false);
	}
    }

    /**
     * Update the existing copyright statement, changing the copyright
     * year to include lastChanged.
     */
    protected void updateCopyright(BufferedReader in,
				BufferedWriter out, String lastChanged)
				throws IOException {
	String line;
	StringBuilder header = new StringBuilder();
	// skip blank lines at beginning of file
	while ((line = in.readLine()) != null) {
	    line = strip(line);
	    if (skipHeaderLine(line)) {
		header.append(line).append('\n');
		continue;
	    }
	    if (line.equals("#"))
		continue;
	    if (line.length() != 0)
		break;
	}
	if (line == null)
	    throw new IOException("NO CONTENT, repair failed");

	if (header.length() > 0)
	    out.write(header.toString());
	out.write("#\n");       // start with an empty comment line
	if (line.startsWith("#")) {
	    boolean updated = false;
	    do {
		if (!updated && line.indexOf("Copyright") >= 0) {
		    Matcher m = ypat.matcher(line);
		    if (m.find()) {
			String y = addCopyrightDate(m.group(ypat_YEAR),
						    lastChanged);
			line = line.substring(0, m.start(ypat_YEAR)) + y +
					    line.substring(m.end(ypat_YEAR));
			updated = true;
		    }
		}
		out.write(line);
		out.write('\n');
		if (line.length() == 0)
		    break;		// end of comment
		if (!line.startsWith("#"))
		    break;		// end of comment
	    } while ((line = in.readLine()) != null);
	}
	if (line != null) {
	    out.write(line);
	    out.write('\n');		// line terminator
	    // have to copy the rest here so that blanks aren't skipped
	    copy(in, out, false);
	}
    }

    /**
     * Convert the comment text to .properties syntax.
     */
    protected String toComment(String comment) {
	BufferedReader r = new BufferedReader(new StringReader(comment));
	StringBuilder out = new StringBuilder();
	try {
	    out.append("#\n");
	    String line;
	    while ((line = r.readLine()) != null)
		out.append(strip("# " + line)).append('\n');
	    out.append("#\n\n");
	} catch (IOException ioex) {
	    // can't happen
	} finally {
	    try {
		r.close();
	    } catch (IOException ex) { }
	}
	return out.toString();
    }

    /**
     * Skip this header line?
     */
    protected boolean skipHeaderLine(String line) {
	return line.startsWith("#!") || line.startsWith("# -*-");
    }
}
