/*
 * Copyright (c) 2013 Anton Golinko
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 */

package org.pdfparse.cos;

import org.pdfparse.parser.PDFRawData;
import org.pdfparse.parser.ParsingContext;

import java.io.IOException;
import java.io.OutputStream;

public class COSBool implements COSObject {

    static private final byte[] TRUE = {0x74, 0x72, 0x75, 0x65};
    static private final byte[] FALSE = {0x66, 0x61, 0x6c, 0x73, 0x65};
    public boolean value;

    public COSBool(Boolean val) {
        value = val;
    }

    @Override
    public void parse(PDFRawData src, ParsingContext context) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void produce(OutputStream dst, ParsingContext context) throws IOException {
        if (value) {
            dst.write(TRUE);
        } else {
            dst.write(FALSE);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
