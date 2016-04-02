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

package org.pdfparse.cds;

import org.pdfparse.parser.PDFRawData;
import org.pdfparse.parser.ParsingContext;
import org.pdfparse.cos.COSArray;
import org.pdfparse.cos.COSObject;
import org.pdfparse.exception.EParseError;

import java.io.IOException;
import java.io.OutputStream;

public class PDFRectangle implements COSObject {
    /** lower left x */
    private float llx = 0;

    /** lower left y */
    private float lly = 0;

    /** upper right x */
    private float urx = 0;

    /** upper right y */
    private float ury = 0;

    // constructors

    /**
     * Constructs a <CODE>PdfRectangle</CODE>-object.
     *
     * @param		llx			lower left x
     * @param		lly			lower left y
     * @param		urx			upper right x
     * @param		ury			upper right y
     *
     */

    public PDFRectangle(float llx, float lly, float urx, float ury) {
        this.llx = llx;
        this.lly = lly;
        this.urx = urx;
        this.ury = ury;
        normalize();
    }

    public PDFRectangle(COSArray array) {
        this.llx = array.getInt(0);
        this.lly = array.getInt(1);
        this.urx = array.getInt(2);
        this.ury = array.getInt(3);
        normalize();
    }

    @Override
    public void parse(PDFRawData src, ParsingContext context) throws EParseError {
        COSArray array = new COSArray(src, context);
        this.llx = array.getInt(0);
        this.lly = array.getInt(1);
        this.urx = array.getInt(2);
        this.ury = array.getInt(3);
        normalize();
    }

    @Override
    public void produce(OutputStream dst, ParsingContext context) throws IOException {
        String s = String.format("[%.2f %.2f %.2f %.2f]", llx, lly, urx, ury);
        dst.write(s.getBytes());
    }

    /**
     * Return a string representation of this rectangle.
     *
     * @return This object as a string.
     */
    public String toString()
    {
        return String.format("[%.2f %.2f %.2f %.2f]", llx, lly, urx, ury);
    }

    public void normalize() {
        float t;
        if (llx > urx) {
            t = llx;
            llx = urx;
            urx = t;
        }

        if (lly > ury) {
            t = lly;
            lly = ury;
            ury = t;
        }
    }

    /**
     * Method to determine if the x/y point is inside this rectangle.
     * @param x The x-coordinate to test.
     * @param y The y-coordinate to test.
     * @return True if the point is inside this rectangle.
     */
    public boolean contains( float x, float y ) {
        return x >= llx && x <= urx &&
                y >= lly && y <= ury;
    }

    /**
     * Get the width of this rectangle as calculated by
     * upperRightX - lowerLeftX.
     *
     * @return The width of this rectangle.
     */
    public float getWidth() {
        return urx - llx;
    }

    /**
     * Get the height of this rectangle as calculated by
     * upperRightY - lowerLeftY.
     *
     * @return The height of this rectangle.
     */
    public float getHeight() {
        return ury - lly;
    }

    /**
     * Move the rectangle the given relative amount.
     *
     * @param dx positive values will move rectangle to the right, negative's to the left.
     * @param dy positive values will move the rectangle up, negative's down.
     */
    public void move(float dx, float dy) {
        llx += dx;
        lly += dy;
        urx += dx;
        ury += dy;
    }
}
