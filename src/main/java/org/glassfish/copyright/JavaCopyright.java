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
 * Support for files with Java syntax.
 *
 * @author	Bill Shannon
 */

package org.glassfish.copyright;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class JavaCopyright extends CommonCopyright {
    public JavaCopyright(Copyright c) {
	super(c);
	commentStart = "/*";
	commentEnd = " */";
	commentPrefix = " * ";
	blankLines = false;
	movePreamble = true;
    }

    /**
     * Does this class support this file?
     */
    protected boolean supports(File file) {
	String fname = file.getName();
	if (fname.endsWith(".java") || fname.endsWith(".g") ||
		fname.endsWith(".c") || fname.endsWith(".h") ||
		fname.endsWith(".css") || fname.endsWith(".js")) {
	    return true;
	}
	if (startsWith(file, "/*\n"))
	    return true;
	return false;
    }

    protected boolean isPreamble(String line) {
	return line.startsWith("package ");
    }
}
