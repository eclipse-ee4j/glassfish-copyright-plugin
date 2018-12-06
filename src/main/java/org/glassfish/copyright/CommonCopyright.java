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
 * Common support for files with comment syntax that uses separate
 * being and end markers.
 *
 * @author	Bill Shannon
 */

package org.glassfish.copyright;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public abstract class CommonCopyright extends AbstractCopyright {
    public CommonCopyright(Copyright c) {
	super(c);
    }

    // must be initialized by subclass
    protected String commentStart;
    protected String commentEnd;
    protected String commentPrefix;
    // Include blank lines after the start comment and before the end comment?
    protected boolean blankLines = true;
    // Move the preamble to after the copyright comment block?
    protected boolean movePreamble = false;

    /**
     * Read the first comment block in the file.
     */
    protected String readComment(BufferedReader r) throws IOException {
	StringBuilder comment = new StringBuilder();
	String line;
	// skip blank lines at beginning of file
	while ((line = r.readLine()) != null) {
	    line = strip(line);
	    if (isPreamble(line))
		continue;
	    if (line.length() != 0)
		break;
	}
	if (!isCommentStart(line))
	    return null;
	String prefix = null;
	while ((line = r.readLine()) != null) {
	    if (line.indexOf("/*") >= 0)
		continue;
	    // have we figured out what the prefix is for this block?
	    if (prefix == null) {
		if (line.length() == 0)
		    continue;
		prefix = findPrefix(line);
	    }
	    if (isCommentEnd(line))
		break;		// end of comment
	    if (line.indexOf("*/") >= 0)
		break;		// end of comment
	    if (line.length() >= prefix.length()) {
		if (line.startsWith(prefix))
		    line = line.substring(prefix.length());
	    } else {
		if (prefix.startsWith(line))
		    line = "";
	    }
	    comment.append(strip(line)).append('\n');
	}
	int len = comment.length();
	if (len >= 2 && comment.charAt(len - 1) == '\n' &&
		comment.charAt(len - 2) == '\n')
	    comment.setLength(len - 1);
	return comment.toString();
    }

    /**
     * Should this line be allowed before the first comment line?
     */
    protected boolean isPreamble(String line) {
	return false;
    }

    /**
     * Is this the start of a comment?
     */
    protected boolean isCommentStart(String line) {
	return line != null && line.indexOf(commentStart) >= 0;
    }

    /**
     * Is this the end of a comment?
     */
    protected boolean isCommentEnd(String line) {
	return line.indexOf(commentEnd.trim()) >= 0;
    }

    /**
     * Return text after end of comment.
     */
    protected String commentTrailer(String line) {
	int i = line.indexOf(commentEnd.trim());
	if (i >= 0) {
	    i += commentEnd.trim().length();
	    return line.substring(i).trim();
	}
	return "";
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
	    if (isPreamble(line)) {
		header.append(line).append('\n');
		continue;
	    }
	    if (line.length() != 0)
		break;
	}

	if (header.length() > 0 && !movePreamble)
	    out.write(header.toString());
	if (comment != null && isCommentStart(line)) {
	    boolean sawCopyright = false;
	    String trailer = "";
	    while ((line = in.readLine()) != null) {
		if (!sawCopyright && line.indexOf("Copyright") >= 0) {
		    Matcher m = ypat.matcher(line);
		    if (m.find()) {
			lastChanged = addCopyrightDate(m.group(ypat_YEAR),
							lastChanged);
			sawCopyright = true;
		    }
		}
		if (isCommentEnd(line)) {
		    trailer = commentTrailer(line);
		    break;		// end of comment
		}
	    }
	    writeCopyright(out, lastChanged, comment);
	    out.write(trailer);
	    out.write("\n\n");	// line terminator and blank line
	    if (header.length() > 0 && movePreamble) {
		out.write(header.toString());
		out.write('\n');
	    }
	} else {
	    writeCopyright(out, lastChanged, comment);
	    out.write("\n\n");		// line terminator and blank line
	    if (header.length() > 0 && movePreamble) {
		out.write(header.toString());
		out.write('\n');
	    }
	    if (line != null)
		out.write(line);
	    out.write('\n');		// line terminator
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
	    if (isPreamble(line)) {
		header.append(line).append('\n');
		continue;
	    }
	    if (line.length() != 0)
		break;
	}
	if (line == null)
	    throw new IOException("NO CONTENT, repair failed");

	if (header.length() > 0)
	    out.write(header.toString());
	out.write(line);
	out.write('\n');
	if (isCommentStart(line)) {
	    boolean updated = false;
	    while ((line = in.readLine()) != null) {
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
		if (isCommentEnd(line))
		    break;		// end of comment
	    }
	    out.write('\n');		// blank line
	}
    }

    /**
     * Convert the comment text to the appropriate syntax.
     */
    protected String toComment(String comment) {
	BufferedReader r = new BufferedReader(new StringReader(comment));
	StringBuilder out = new StringBuilder();
	try {
	    out.append(commentStart).append("\n");
	    if (blankLines)
		out.append(strip(commentPrefix)).append("\n");
	    String line;
	    while ((line = r.readLine()) != null)
		out.append(strip(commentPrefix + line)).append('\n');
	    if (blankLines)
		out.append(strip(commentPrefix)).append("\n");
	    //out.append(commentEnd).append("\n\n");
	    out.append(commentEnd);
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
