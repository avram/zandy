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

import org.pdfparse.exception.EParseError;
import org.pdfparse.parser.PDFParser;
import org.pdfparse.parser.PDFRawData;
import org.pdfparse.parser.ParsingContext;

import java.io.IOException;
import java.io.OutputStream;


public class COSStream extends COSDictionary {
    private byte[] data = null;

    public COSStream(COSDictionary dict, PDFRawData src, ParsingContext context) throws EParseError {
        super(dict, context);

        data = PDFParser.fetchStream(src, this.getUInt(COSName.LENGTH, context.objectCache, 0), true);
    }

    @Override
    public void parse(PDFRawData src, ParsingContext context) throws EParseError {
        super.parse(src, context);
        data = PDFParser.fetchStream(src, this.getUInt(COSName.LENGTH, context.objectCache,0), true);
    }
    @Override
    public void produce(OutputStream dst, ParsingContext context) throws IOException {
        //throw new ENotSupported();
        super.produce(dst, context);
        //dst.write(null);
    }

    public byte[] getData() {
        return data;
    }
}
