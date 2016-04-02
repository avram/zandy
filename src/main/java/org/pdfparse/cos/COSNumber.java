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

import org.pdfparse.exception.*;
import org.pdfparse.parser.PDFRawData;
import org.pdfparse.parser.ParsingContext;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <CODE>COSNumber</CODE> provides two types of numbers, integer and real.
 * <P>
 * Integers may be specified by signed or unsigned constants. Reals may only be
 * in decimal format.<BR>
 * This object is described in the 'Portable Document Format Reference Manual
 * version 1.7' section 3.3.2 (page 52-53).
 *
 * @see		COSObject
 * @see		EParseError
 */

public final class COSNumber implements COSObject {

    /**
     * actual value of this <CODE>COSNumber</CODE>, represented as a
     * <CODE>double</CODE>
     */
    private double value;
    private boolean isInteger;

    public COSNumber(double val) {
        value = val;
        isInteger = false;
    }
    public COSNumber(float val) {
        value = val;
        isInteger = false;
    }
    public COSNumber(int val) {
        value = val;
        isInteger = true;
    }
    public COSNumber(long val) {
        value = val;
        isInteger = true;
    }

    public COSNumber(PDFRawData src, ParsingContext context) {
        parse(src, context);
    }


    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o)
    {
        return o instanceof COSNumber && (((COSNumber)o).value == value);
        // TODO: make decision about precision
        // return o instanceof COSNumber && (Math.abs(((COSNumber)o).value - value) < 0.000001);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        //taken from java.lang.Long
        return Float.floatToIntBits((float)value);
    }


        /**
         * Returns the primitive <CODE>int</CODE> value of this object.
         *
         * @return The value as <CODE>int</CODE>
         */
    public int intValue() {
        return (int) value;
    }

    /**
     * Returns the primitive <CODE>long</CODE> value of this object.
     *
     * @return The value as <CODE>long</CODE>
     */
    public long longValue() {
        return (long) value;
    }

    /**
     * Returns the primitive <CODE>double</CODE> value of this object.
     *
     * @return The value as <CODE>double</CODE>
     */
    public double doubleValue() {
        return value;
    }

    /**
     * Returns the primitive <CODE>float</CODE> value of this object.
     *
     * @return The value as <CODE>float</CODE>
     */
    public float floatValue() {
        return (float)value;
    }


    @Override
    public void parse(PDFRawData src, ParsingContext context) throws EParseError {
        int prev = src.pos;
        float sign = 1;
        float divider = 10;


        boolean hasFractional = false;
        isInteger = true;
        value = 0;

        while (src.pos < src.length) {
            switch (src.src[src.pos]) {
                case 0x30:
                case 0x31:
                case 0x32:
                case 0x33:
                case 0x34: // 0..4
                case 0x35:
                case 0x36:
                case 0x37:
                case 0x38:
                case 0x39: // 5..9
                    if (hasFractional) {
                        value += (src.src[src.pos] - 0x30) / divider;
                        divider *= 10;
                    } else
                        value = value * 10 + (src.src[src.pos] - 0x30);
                    src.pos++;
                    break;
                case 0x2B: // +
                    if (src.pos == prev) {
                        sign = 1;
                        src.pos++;
                        break;
                    }
                    throw new EParseError("'+' not allowed here (invalid number)");
                case 0x2D: // -
                    if (src.pos == prev) {
                        sign = -1;
                        src.pos++;
                        break;
                    }
                    throw new EParseError("'-' not allowed here (invalid number)");
                case 0x2E: // .
                    if (hasFractional)
                        throw new EParseError("'.' not allowed here (invalid number)");
                    hasFractional = true;
                    isInteger = false;
                    src.pos++;
                    break;

                // Separators
                case 0x00:  case 0x09:  case 0x0A:  case 0x0D:  case 0x20:
                // Delimeters
                case 0x28:  case 0x29:  case 0x3C:  case 0x3E:  case 0x2F:
                case 0x5B:  case 0x5D:  case 0x7B:  case 0x7D:  case 0x25:
                    if (prev == src.pos)
                        throw new EParseError("Number expected, got no value");

                    value = sign * value;
                    return;
                default:
                    throw new EParseError("Number expected, got invalid value");
            } // switch
        } // while

        if (prev == src.pos)
            throw new EParseError("Number expected, got no value (2)");

        value = sign * value;
        return;
    }

    @Override
    public void produce(OutputStream dst, ParsingContext context) throws IOException {
        dst.write(this.toString().getBytes());
    }

    @Override
    public String toString() {
        if (isInteger)
            return String.valueOf((long)value);
        else return String.format("%f.3", value);
    }


    static public void writeInteger(int val, OutputStream dst) throws IOException {
        String str = Integer.toString(val);
        for (int i = 0; i < str.length(); i++) {
            dst.write(str.codePointAt(i));
        }
    }

    static public int readInteger(PDFRawData src) throws EParseError {
        int prev = src.pos;
        int res = 0;
        int sign = 1;

        while (src.pos < src.length) {
            switch (src.src[src.pos]) {
                case 0x30:
                case 0x31:
                case 0x32:
                case 0x33:
                case 0x34: // 0..4
                case 0x35:
                case 0x36:
                case 0x37:
                case 0x38:
                case 0x39: // 5..9
                    res = res * 10 + (src.src[src.pos] - 0x30);
                    src.pos++;
                    break;
                case 0x2B: // +
                    if (src.pos == prev) {
                        sign = 1;
                        src.pos++;
                        break;
                    }
                    throw new EParseError("Invalid integer value");
                case 0x2D: // -
                    if (src.pos == prev) {
                        sign = -1;
                        src.pos++;
                        break;
                    }
                    throw new EParseError("Invalid integer value");

                    // Separators
                case 0x00:  case 0x09:  case 0x0A:  case 0x0D:  case 0x20:
                    // Delimeters
                case 0x28:  case 0x29:  case 0x3C:  case 0x3E:  case 0x2F:
                case 0x5B:  case 0x5D:  case 0x7B:  case 0x7D:  case 0x25:
                    if (prev == src.pos)
                        throw new EParseError("Number expected, got no value");

                    return sign * res;

                default:
                    throw new EParseError("Number expected, got invalid value (2)");
            } // switch
        } // while

        if (prev == src.pos)
            throw new EParseError("Number expected, got no value (3)");

        return sign * res;
    }

}
