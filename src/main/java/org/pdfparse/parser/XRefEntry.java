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

package org.pdfparse.parser;

import org.pdfparse.cos.COSObject;

public class XRefEntry {
    public int id;
    public int gen;
    public int fileOffset;
    public int containerObjId;
    public int indexWithinContainer;

    public boolean isCompressed;

    public COSObject cachedObject;
    public PDFRawData decompressedStreamData;

    @Override
    public String toString() {
        String s, name = "";
        if (cachedObject != null)
            name = cachedObject.getClass().getName();

        if (isCompressed) {
            s = String.format("(%d %d R)/%s @ [%d + %d]", id, gen, name, containerObjId, indexWithinContainer);
        } else {
            s = String.format("(%d %d R)/%s @ %d", id, gen, name, fileOffset);
        };
        return s;
    }

    public byte[] getTextRef() {
        String s = String.format("%d %d R", id, gen);
        return s.getBytes();
    }
}
