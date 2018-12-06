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
 * Support for files with asciidoc file syntax.
 * Comments of the form:
 * /////////////////////////////////////////////////////////////////////////////
 * comment
 * /////////////////////////////////////////////////////////////////////////////
 *
 * @author	Bill Shannon
 */

package org.glassfish.copyright;

import java.io.*;
import java.util.regex.*;

public class AsciidocCopyright extends CommonCopyright {
    private String firstComment;
    private static final String commentRegex = "////+";
    private static final Pattern pat = Pattern.compile(commentRegex);
    private static final String commentDelim =
	"///////////////////////////////////////" +
	"////////////////////////////////////////";	// 79 slashes

    public AsciidocCopyright(Copyright c) {
	super(c);
	commentStart = commentDelim;
	commentEnd = commentDelim;
	commentPrefix = "    ";
    }

    /**
     * Is this an asciidoc file?
     */
    protected boolean supports(File file) {
	return file.getName().endsWith(".adoc");
    }

    /**
     * Is this the start of a comment?
     */
    protected boolean isCommentStart(String line) {
	if (line != null && line.matches(commentRegex)) {
	    firstComment = line;
	    return true;
	} else
	    return false;
    }

    /**
     * Is this the end of a comment?
     */
    protected boolean isCommentEnd(String line) {
	return line.equals(firstComment);
    }

    /**
     * Return text after end of comment.
     */
    protected String commentTrailer(String line) {
	Matcher m = pat.matcher(line);
	if (m.matches()) {
	    int end = m.end();
	    return line.substring(end).trim();
	}
	return "";
    }
}
