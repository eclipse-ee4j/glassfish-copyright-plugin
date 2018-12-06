/*
 * Copyright (c) 2010, 2018 Oracle and/or its affiliates. All rights reserved.
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
 * Support for files with markdown file syntax.
 * Comments of the form:
 * [//]: # ( comment )
 *
 * @author	Bill Shannon
 */

package org.glassfish.copyright;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class MarkdownCopyright extends AbstractCopyright {
    protected String commentPrefix = "[//]: # \" ";
    protected String commentSuffix = " \"";

    public MarkdownCopyright(Copyright c) {
	super(c);
    }

    /**
     * Is this a markdown file?
     */
    protected boolean supports(File file) {
	String fname = file.getName();
	if (fname.endsWith(".md") || fname.endsWith(".md.vm"))
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
	    if (line.length() != 0)
		break;
	}
	if (line == null || !line.startsWith(commentPrefix))
	    return null;
	do {
	    if (line.length() == 0)
		break;		// end of comment
	    if (!line.startsWith(commentPrefix))
		break;		// end of comment
	    line = line.substring(commentPrefix.length());
	    if (line.endsWith(commentSuffix))
		line = line.substring(0,
					line.length() - commentSuffix.length());
	    comment.append(strip(line).replace("''", "\"")).append('\n');
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
	// skip blank lines at beginning of file
	while ((line = in.readLine()) != null) {
	    line = strip(line);
	    if (line.length() != 0)
		break;
	}

	if (comment != null && line != null && line.startsWith(commentPrefix)) {
	    boolean sawCopyright = false;
	    do {
		if (line.length() == 0)
		    break;		// end of comment
		if (!line.startsWith(commentPrefix))
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
	// skip blank lines at beginning of file
	while ((line = in.readLine()) != null) {
	    line = strip(line);
	    if (line.length() != 0)
		break;
	}
	if (line == null)
	    throw new IOException("NO CONTENT, repair failed");

	if (line.startsWith(commentPrefix)) {
	    boolean updated = false;
	    do {
		if (!line.startsWith(commentPrefix))
		    break;		// end of comment
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
	    } while ((line = in.readLine()) != null);
	}
	if (line != null) {
	    if (line.length() != 0)
		out.write('\n');	// make sure there's a blank line
	    out.write(line);
	    out.write('\n');		// line terminator
	    // have to copy the rest here so that blanks aren't skipped
	    copy(in, out, false);
	}
    }

    /**
     * Convert the comment text to markdown syntax.
     */
    protected String toComment(String comment) {
	BufferedReader r = new BufferedReader(new StringReader(comment));
	StringBuilder out = new StringBuilder();
	try {
	    String line;
	    //out.append(commentPrefix).append(commentSuffix).append('\n');
	    while ((line = r.readLine()) != null)
		out.append(commentPrefix).
		    append(strip(line).replace("\"", "''")).
		    append(commentSuffix).append('\n');
	    //out.append(commentPrefix).append(commentSuffix).append('\n');
	    out.append("\n");
	} catch (IOException ioex) {
	    // can't happen
	} finally {
	    try {
		r.close();
	    } catch (IOException ex) { }
	}
	return out.toString();
    }
}
