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
 * Support for files with XML syntax.
 *
 * @author	Bill Shannon
 */

package org.glassfish.copyright;

import java.io.*;

public class XmlCopyright extends CommonCopyright {
    public XmlCopyright(Copyright c) {
	super(c);
	commentStart = "<!--";
	commentEnd = "-->";
	commentPrefix = "    ";
    }

    /**
     * Is this an XML file?
     */
    protected boolean supports(File file) {
	String fname = file.getName();
	if (
		    fname.endsWith(".xml") || fname.endsWith(".xsl") ||
		    fname.endsWith(".html") || fname.endsWith(".xhtml") ||
		    fname.endsWith(".htm") ||
		    fname.endsWith(".dtd") || fname.endsWith(".xsd") ||
		    fname.endsWith(".wsdl") || fname.endsWith(".inc") ||
		    fname.endsWith(".jnlp") || fname.endsWith(".tld") ||
		    fname.endsWith(".xcs") || fname.endsWith(".jsf") ||
		    fname.endsWith(".hs") || fname.endsWith(".jhm") ||
		    (fname.equals("build.properties") && startsWith(file, "<"))
		) {
	    return true;
	}
	if (startsWith(file, "<?xml"))
	    return true;
	return false;
    }

    protected boolean isPreamble(String line) {
	return startsWith(line, "<?xml ") || startsWith(line, "<!DOCTYPE") ||
		startsWith(line, "<html") || startsWith(line, "<head>") ||
		startsWith(line, "<meta");
    }
}
