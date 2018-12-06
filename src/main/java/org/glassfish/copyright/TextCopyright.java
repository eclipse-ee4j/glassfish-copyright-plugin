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
 * Support for arbitrary text files.
 * No repair, because we don't know where the comment ends.
 *
 * @author	Bill Shannon
 */

package org.glassfish.copyright;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class TextCopyright extends AbstractCopyright {
    public TextCopyright(Copyright c) {
	super(c);
    }

    /**
     * Is this a plain text file?
     */
    protected boolean supports(File file) {
	return true;	// XXX - should check for text content
    }

    /**
     * Read the first comment block in a non-Java file.
     * Don't know how to do this so just return up to the first 100 lines.
     */
    protected String readComment(BufferedReader r) throws IOException {
	StringBuilder comment = new StringBuilder();
	String line;
	int nlines = 0;
	while ((line = r.readLine()) != null) {
	    String cline = canon(line);
	    if (comment.length() == 0) {
		// skip shell lines
		if (line.startsWith("#!"))
		    continue;
		// skip empty lines
		if (cline.length() == 0)
		    continue;
	    }
	    comment.append(cline).append('\n');
	    if (++nlines >= 100)
		break;
	}
	return comment.toString();
    }

    /**
     * Does the string match the pattern?
     * Since we don't know where the comment text might end,
     * we just insist that it match starting at the beginning
     * of the text.
     */
    protected boolean matches(Pattern pat, String s) {
	Matcher m = pat.matcher(s);
	return m.find() && m.start() == 0;
    }

    /**
     * Repair the c.errors in the file.
     *
     * Repair cases and strategy:
     *
     *	Missing copyright
     *		Insert correct copyright
     *
     *	Wrong copyright
     *		Try to extract copyright date.
     *		Insert correct copyright.
     *
     *	Wrong date
     *		Update existing date in existing copyright.
     */
    protected void repair(File file, String comment, RepairType type)
				throws IOException {
	// no repair for text files
    }

    /**
     * Skip the first comment block, replacing it with the correct copyright.
     * If the file starts with a "package" statement,
     * save it and write it out after the new copyright.
     */
    protected void replaceCopyright(BufferedReader in,
			BufferedWriter out, String comment, String lastChanged)
			throws IOException {
    }

    /**
     * Update the existing copyright statement, changing the copyright
     * year to include lastChanged.
     */
    protected void updateCopyright(BufferedReader in,
				BufferedWriter out, String lastChanged)
				throws IOException {
    }

    /**
     * Convert the comment text to the appropriate syntax.
     */
    protected String toComment(String comment) {
	return comment;
    }

    /**
     * Canonicalize line by removing leading special characters.
     * (Don't remove special characters that are known to occur
     * in text we care about.)
     */
    private static String canon(String line) {
	for (int i = 0; i < line.length(); i++) {
	    char c = line.charAt(i);
	    if (Character.isLetterOrDigit(c) ||
		    c == '\"' || c == '[' || c == '(')
		return line.substring(i).trim();
	}
	return "";
    }
}
