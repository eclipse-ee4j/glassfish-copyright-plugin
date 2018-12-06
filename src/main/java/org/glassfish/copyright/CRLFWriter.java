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

package org.glassfish.copyright;

import java.io.*;

/**
 * Convert lines into the canonical Windows format, that is,
 * terminate lines with CRLF. <p>
 */
public class CRLFWriter extends FilterWriter {
    protected int lastb = -1;
    protected static char[] newline = new char[] { '\r', '\n' };

    public CRLFWriter(Writer out) {
	super(out);
    }

    @Override
    public void write(int b) throws IOException {
	if (b == '\r') {
	    out.write(newline);
	} else if (b == '\n') {
	    if (lastb != '\r')
		out.write(newline);
	} else {
	    out.write(b);
	}
	lastb = b;
    }

    @Override
    public void write(char cbuf[]) throws IOException {
	write(cbuf, 0, cbuf.length);
    }

    @Override
    public void write(char cbuf[], int off, int len) throws IOException {
	int start = off;

	len += off;
	for (int i = start; i < len ; i++) {
	    if (cbuf[i] == '\r') {
		out.write(cbuf, start, i - start);
		out.write(newline);
		start = i + 1;
	    } else if (cbuf[i] == '\n') {
		if (lastb != '\r') {
		    out.write(cbuf, start, i - start);
		    out.write(newline);
		}
		start = i + 1;
	    }
	    lastb = cbuf[i];
	}
	if ((len - start) > 0)
	    out.write(cbuf, start, len - start);
    }
}
