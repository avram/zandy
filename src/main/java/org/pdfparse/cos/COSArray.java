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
import org.pdfparse.parser.PDFRawData;
import org.pdfparse.parser.ParsingContext;
import org.pdfparse.parser.PDFParser;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;


public class COSArray extends ArrayList<COSObject> implements COSObject {

    public COSArray() {
        super();
    }

    public COSArray(PDFRawData src, ParsingContext context) throws EParseError {
        super();
        parse(src, context);
    }

    @Override
    public void parse(PDFRawData src, ParsingContext context) throws EParseError {
        src.pos++; // Skip '['
        src.skipWS();

        while (src.pos < src.length) {
            if (src.src[src.pos] == 0x5D)
                break; // ']'
            this.add( PDFParser.parseObject(src, context) );
            src.skipWS();
        }
        if (src.pos == src.length)
            return;
        src.pos++;
        src.skipWS();
    }

    @Override
    public void produce(OutputStream dst, ParsingContext context) throws IOException {
        dst.write(0x5B); // "["
        for (int i = 0; i < this.size(); i++) {
            if (i != 0) {
                if (i % 20 == 0) {
                    dst.write(0x0A); // "\n"
                } else {
                    dst.write(0x20); // " ";
                }
            }
            this.get(i).produce(dst, context);
        }
        dst.write(0x5D); // "]";
    }

    @Override
    public String toString() {
        return String.format("[ %d ]", this.size());
    }

    public int getInt(int idx) {
        COSObject obj = get(idx);
         if (obj instanceof COSNumber) return ((COSNumber)obj).intValue();
         else return 0;
    }

    public float getFloat(int idx) {
        COSObject obj = get(idx);
        if (obj instanceof COSNumber) return ((COSNumber)obj).floatValue();
        else return 0;
    }
}
