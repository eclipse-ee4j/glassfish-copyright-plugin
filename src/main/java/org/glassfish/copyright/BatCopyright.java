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
 * Support for Windows .bat files.
 *
 * Note that repair will canonicalize the line terminators to CRLF.
 *
 * @author	Bill Shannon
 */

package org.glassfish.copyright;

import java.io.*;
import java.util.regex.*;

public class BatCopyright extends AbstractCopyright {
    public BatCopyright(Copyright c) {
	super(c);
    }

    /**
     * Is this a Windows .bat file?
     */
    protected boolean supports(File file) {
	return file.getName().endsWith(".bat");
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
	    if (line.startsWith("@echo"))
		continue;
	    if (line.equals("REM"))
		continue;
	    if (line.length() != 0)
		break;
	}
	if (line == null || !line.startsWith("REM"))
	    return null;
	String prefix = "REM  ";
	do {
	    if (line.length() == 0)
		break;		// end of comment
	    if (!line.startsWith("REM"))
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
	// wrap it to canonicalize line terminators
	out = new BufferedWriter(new CRLFWriter(out));
	String line;
	StringBuilder header = new StringBuilder();
	// skip blank lines at beginning of file
	while ((line = in.readLine()) != null) {
	    line = strip(line);
	    if (line.startsWith("@echo")) {
		header.append(line).append('\n');
		continue;
	    }
	    if (comment != null && line.equals("REM"))
		continue;
	    if (line.length() != 0)
		break;
	}

	if (header.length() > 0)
	    out.write(header.toString());
	if (comment != null && line != null && line.startsWith("REM")) {
	    boolean sawCopyright = false;
	    do {
		if (line.length() == 0)
		    break;		// end of comment
		if (!line.startsWith("REM"))
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
	out.flush();	// need to flush wrapper
    }

    /**
     * Update the existing copyright statement, changing the copyright
     * year to include lastChanged.
     */
    protected void updateCopyright(BufferedReader in,
				BufferedWriter out, String lastChanged)
				throws IOException {
	// wrap it to canonicalize line terminators
	out = new BufferedWriter(new CRLFWriter(out));
	String line;
	StringBuilder header = new StringBuilder();
	// skip blank lines at beginning of file
	while ((line = in.readLine()) != null) {
	    line = strip(line);
	    if (line.startsWith("@echo")) {
		header.append(line).append('\n');
		continue;
	    }
	    if (line.equals("REM"))
		continue;
	    if (line.length() != 0)
		break;
	}
	if (line == null)
	    throw new IOException("NO CONTENT, repair failed");

	if (header.length() > 0)
	    out.write(header.toString());
	out.write(line);
	out.write('\n');		// line terminator
	if (line.startsWith("REM")) {
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
		if (!line.startsWith("REM"))
		    break;		// end of comment
	    } while ((line = in.readLine()) != null);
	}
	if (line != null) {
	    out.write(line);
	    out.write('\n');		// line terminator
	    // have to copy the rest here so that blanks aren't skipped
	    copy(in, out, false);
	}
	out.flush();	// need to flush wrapper
    }

    /**
     * Line terminator is always CRLF for .bat files.
     */
    protected String guessLineTerminator(Reader in) {
	return "\r\n";
    }

    /**
     * Convert the comment text to .bat syntax.
     */
    protected String toComment(String comment) {
	BufferedReader r = new BufferedReader(new StringReader(comment));
	StringBuilder out = new StringBuilder();
	try {
	    out.append("REM\n");
	    String line;
	    while ((line = r.readLine()) != null)
		out.append(strip("REM  " + line)).append('\n');
	    out.append("REM\n\n");
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
