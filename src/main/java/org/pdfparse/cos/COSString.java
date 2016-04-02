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
import org.pdfparse.utils.ByteBuffer;
import org.pdfparse.utils.IntIntHashtable;

import java.io.IOException;
import java.io.OutputStream;

public final class COSString implements COSObject {

    private static final byte[] C28 = {'\\', '('};
    private static final byte[] C29 = {'\\', ')'};
    private static final byte[] C5C = {'\\', '\\'};
    private static final byte[] C0A = {'\\', 'n'};
    private static final byte[] C0D = {'\\', 'r'};

    static final char winansiByteToChar[] = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
        32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,
        64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79,
        80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95,
        96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111,
        112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127,
        8364, 65533, 8218, 402, 8222, 8230, 8224, 8225, 710, 8240, 352, 8249, 338, 65533, 381, 65533,
        65533, 8216, 8217, 8220, 8221, 8226, 8211, 8212, 732, 8482, 353, 8250, 339, 65533, 382, 376,
        160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175,
        176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191,
        192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207,
        208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223,
        224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239,
        240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255};
    static final IntIntHashtable winansi = new IntIntHashtable();

    static final char pdfEncodingByteToChar[] = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
        32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,
        64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79,
        80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95,
        96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111,
        112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127,
        0x2022, 0x2020, 0x2021, 0x2026, 0x2014, 0x2013, 0x0192, 0x2044, 0x2039, 0x203a, 0x2212, 0x2030, 0x201e, 0x201c, 0x201d, 0x2018,
        0x2019, 0x201a, 0x2122, 0xfb01, 0xfb02, 0x0141, 0x0152, 0x0160, 0x0178, 0x017d, 0x0131, 0x0142, 0x0153, 0x0161, 0x017e, 65533,
        0x20ac, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175,
        176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191,
        192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207,
        208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223,
        224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239,
        240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255};
    static final IntIntHashtable pdfEncoding = new IntIntHashtable();
    static {
        for (int k = 128; k < 161; ++k) {
            char c = winansiByteToChar[k];
            if (c != 65533)
                winansi.put(c, k);
        }
        for (int k = 128; k < 161; ++k) {
            char c = pdfEncodingByteToChar[k];
            if (c != 65533)
                pdfEncoding.put(c, k);
        }
    }

    private static final int[] HEX2V = { // '0'..'f'
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -1, -1, -1, -1, -1,
            -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, 10, 11, 12, 13, 14, 15};
    private static final byte[] V2HEX = { // '0'..'f'
            0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
            0x61, 0x62, 0x63, 0x64, 0x65, 0x66};

    private static final byte[] EMPTY = {};

    private String value;
    private byte[] binaryValue;
    private boolean forceHexForm;

    public COSString(String val) {
        value = val;
        binaryValue = val.getBytes();
    }

    public COSString(PDFRawData src, ParsingContext context) throws EParseError {
        parse(src, context);
    }

    public void clear() {
        value = "";
        binaryValue = EMPTY;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String val) {
        value = val;
        binaryValue = convertToBytes(val, null);
    }

    public byte[] getBinaryValue() {
        return binaryValue;
    }

    public void setBinaryValue(byte[] val) {
        if (val == null)
            binaryValue = EMPTY;
        else binaryValue = val;

        value = convertToString(binaryValue);
    }

    /**
     * Forces the string to be written in literal form instead of hexadecimal form.
     *
     * @param v
     *            if v is true the string will be written in literal form, otherwise it will be written in hexa if
     *            necessary.
     */

    public void setForceLiteralForm(boolean v)
    {
        forceHexForm = !v;
    }

    /**
     * Forces the string to be written in hexadecimal form instead of literal form.
     *
     * @param v
     *            if v is true the string will be written in hexadecimal form otherwise it will be written in literal if
     *            necessary.
     */

    public void setForceHexForm(boolean v)
    {
        forceHexForm = v;
    }

        @Override
    public void parse(PDFRawData src, ParsingContext context) throws EParseError {
        int nesting_brackets = 0;
        int v;
        byte ch;
        value = "";
        binaryValue = EMPTY;

        if (src.src[src.pos] == '<') {
            src.pos++; // Skip the opening bracket '<'
            byte[] bytes = parseHexStream(src, context);
            setBinaryValue(bytes);
            forceHexForm = true;
            return;
        }

        // === this is a literal string
        forceHexForm = false;
        src.pos++; // Skip the opening bracket '('

        ByteBuffer buffer = context.tmpBuffer;
        buffer.reset();

        while (src.pos < src.length) {
            ch = src.src[src.pos];
            switch (ch) {
                case 0x5C: // '\'
                    src.pos++;
                    if (src.pos >= src.length)
                        break; // finish. ignore this reverse solidus

                    ch = src.src[src.pos];
                    switch (ch) {
                        case 0x6E: // 'n'
                            buffer.append(0x0A);
                            break;
                        case 0x72: // 'r'
                            buffer.append(0x0D);
                            break;
                        case 0x74: // 't'
                            buffer.append(0x09);
                            break;
                        case 0x62: // 'b'
                            buffer.append(0x08);
                            break;
                        case 0x66: // 'f'
                            buffer.append(0x0C);
                            break;
                        case 0x28: // '('
                            buffer.append(0x28);
                            break;
                        case 0x29: // ')'
                            buffer.append(0x29);
                            break;
                        case 0x5C: // '\'
                            buffer.append(0x5C);
                            break;
                        case 0x30: // '0'..'7'
                        case 0x31:
                        case 0x32:
                        case 0x33:
                        case 0x34:
                        case 0x35:
                        case 0x36:
                        case 0x37:
                            v = ch - 0x30; // convert first char to number
                            if ((src.src[src.pos + 1] >= 0x30) && (src.src[src.pos + 1] <= 0x37)) {
                                src.pos++;
                                v = v * 8 + (src.src[src.pos] - 0x30);
                                if ((src.src[src.pos + 1] >= 0x30) && (src.src[src.pos + 1] <= 0x37)) {
                                    src.pos++;
                                    v = v * 8 + (src.src[src.pos] - 0x30);
                                }
                            }
                            buffer.append(v);
                            break;
                        case 0x0A:
                            if ((src.pos < src.length) &&  (src.src[src.pos + 1] == 0x0D)) {
                                src.pos++;
                            }
                            break;
                        case 0x0D:
                            break;

                        default:
                            // If the character following the REVERSE SOLIDUS is not one of those shown in Table 3,
                            // the REVERSE SOLIDUS shall be ignored.
                            buffer.append(src.src[src.pos]); //add this char
                    }//switch after '\'

                    src.pos++;
                    break;
                case 0x28: // '('
                    nesting_brackets++;
                    buffer.append(0x28);
                    src.pos++;
                    break;
                case 0x29: // ')'
                    nesting_brackets--;
                    if (nesting_brackets < 0) {  //found closing bracket. End of string
                        src.pos++;
                        binaryValue = buffer.toByteArray();
                        value = convertToString(binaryValue);
                        return;
                    }
                    buffer.append(0x29);
                    src.pos++;
                    break;
                case 0x0D: // '\r':
                case 0x0A: // '\n':
                    // An end-of-line marker appearing within a literal string without a preceding REVERSE SOLIDUS shall be treated
                    // as a byte value of (0Ah), irrespective of whether the end-of-line marker was a CARRIAGE RETURN (0Dh), a
                    // LINE FEED (0Ah), or both.

                    buffer.append(0x0A);
                    src.pos++;
                    break;
                default:
                    buffer.append(src.src[src.pos]);
                    src.pos++;
            } // switch
        } // while ...

        // Reach end-of-file/data?
        if (src.pos < src.length) {
            src.pos++;
        }

        context.softAssertSyntaxComliance(nesting_brackets == 0, "Unbalanced brackets and illegal nesting while parsing string object");

        binaryValue = buffer.toByteArray();
        value = convertToString(binaryValue);
    }

    @Override
    public void produce(OutputStream dst, ParsingContext context) throws IOException {
        int i, j, len;
        len = binaryValue.length;

        if (forceHexForm) {
            // === Hexadecimal form
            int b;
            // TODO: use context.tmpBuffer
            byte[] hex = new byte[binaryValue.length * 2];
            for (i = 0, j = 0; i < len; i++, j += 2) {
                b = binaryValue[i] & 0xFF;
                hex[j] = V2HEX[b >> 4];
                hex[j + 1] = V2HEX[b & 0xF];
            }
            dst.write(0x3C); // "<"
            dst.write(hex);
            dst.write(0x3E); // ">"
            return;
        }

        // === Literal form
        dst.write('(');
        for (i = 0; i < len; i++) {
            switch (binaryValue[i]) {
                case 0x28:
                    dst.write(C28);
                    break;
                case 0x29:
                    dst.write(C29);
                    break;
                case 0x5C:
                    dst.write(C5C);
                    break;
                case 0x0A:
                    dst.write(C0A);
                    break;
                case 0x0D:
                    dst.write(C0D);
                    break;

                default:
                    dst.write(binaryValue[i]);
                    break;
            }
        }
        dst.write(')');
    }

    @Override
    public String toString() {
        return value;
    }


    /** Converts a <CODE>String</CODE> to a </CODE>byte</CODE> array according
     * to the font's encoding.
     * @return an array of <CODE>byte</CODE> representing the conversion according to the font's encoding
     * @param encoding the encoding
     * @param text the <CODE>String</CODE> to be converted
     */
    public static final byte[] convertToBytes(String text, String encoding) {
        if (text == null)
            return new byte[0];
        if (encoding == null || encoding.length() == 0) {
            int len = text.length();
            byte b[] = new byte[len];
            for (int k = 0; k < len; ++k)
                b[k] = (byte)text.charAt(k);
            return b;
        }

        return text.getBytes();
        /*
        ExtraEncoding extra = extraEncodings.get(encoding.toLowerCase());
        if (extra != null) {
            byte b[] = extra.charToByte(text, encoding);
            if (b != null)
                return b;
         }
        IntIntHashtable hash = null;
        if (encoding.equals(BaseFont.WINANSI))
            hash = winansi;
        else if (encoding.equals(PdfObject.TEXT_PDFDOCENCODING))
            hash = pdfEncoding;
        if (hash != null) {
            char cc[] = text.toCharArray();
            int len = cc.length;
            int ptr = 0;
            byte b[] = new byte[len];
            int c = 0;
            for (int k = 0; k < len; ++k) {
                char char1 = cc[k];
                if (char1 < 128 || char1 > 160 && char1 <= 255)
                    c = char1;
                else
                    c = hash.get(char1);
                if (c != 0)
                    b[ptr++] = (byte)c;
            }
            if (ptr == len)
                return b;
            byte b2[] = new byte[ptr];
            System.arraycopy(b, 0, b2, 0, ptr);
            return b2;
        }
        if (encoding.equals(PdfObject.TEXT_UNICODE)) {
            // workaround for jdk 1.2.2 bug
            char cc[] = text.toCharArray();
            int len = cc.length;
            byte b[] = new byte[cc.length * 2 + 2];
            b[0] = -2;
            b[1] = -1;
            int bptr = 2;
            for (int k = 0; k < len; ++k) {
                char c = cc[k];
                b[bptr++] = (byte)(c >> 8);
                b[bptr++] = (byte)(c & 0xff);
            }
            return b;
        }
        try {
            Charset cc = Charset.forName(encoding);
            CharsetEncoder ce = cc.newEncoder();
            ce.onUnmappableCharacter(CodingErrorAction.IGNORE);
            CharBuffer cb = CharBuffer.wrap(text.toCharArray());
            java.nio.ByteBuffer bb = ce.encode(cb);
            bb.rewind();
            int lim = bb.limit();
            byte[] br = new byte[lim];
            bb.get(br);
            return br;
        }
        catch (IOException e) {
            throw new ExceptionConverter(e);
        } */
    }


    /** Converts a </CODE>byte</CODE> array to a <CODE>String</CODE> trying to detect encoding
     * @param bytes the bytes to convert
     * @return the converted <CODE>String</CODE>
     */
    public static final String convertToString(byte bytes[], int offset, int length) {
        if (bytes == null)
            return "";
        // trying to detect encoding
        if (bytes.length > 2 && ((bytes[0] & 0xFF) == 0xFE) && ((bytes[1] & 0xFF) == 0xFF)) { // UTF-16 BE
            try {
                return
                    new String(bytes, offset, length, "UTF-16");
            } catch (Exception e) {}
        }

        if (offset + length > bytes.length)
            length = bytes.length - offset;
        if (length <= 0)
            return "";

        int dest = offset + length;
        char c[] = new char[length];
        int i = 0;

        char[] map = pdfEncodingByteToChar; // use PDFEncoding

        for (int k = offset; k < dest; k++)
            c[i++] = (char)map[bytes[k] & 0xff];
        return new String(c);
    }

    public static final String convertToString(byte bytes[]) {
        return convertToString(bytes, 0, bytes.length);
    }

    public static final String convertToString(ByteBuffer bytes) {
        return convertToString(bytes.getBuffer(), 0, bytes.size());
    }

    public static final String convertToString(byte bytes[], int offset, int length, String encoding) {
        if (bytes == null)
            return "";
        // trying to detect encoding
        if (bytes.length > 2 && ((bytes[0] & 0xFF) == 0xFE) && ((bytes[1] & 0xFF) == 0xFF)) { // UTF-16 BE
            try {
                return
                    new String(bytes, offset, length, "UTF-16");
            } catch (Exception e) {}
        }

        if (offset + length > bytes.length)
            length = bytes.length - offset;
        if (length <= 0)
            return "";

        int dest = offset + length;
        char c[] = new char[length];
        int i = 0;

        char[] map;
        if (encoding.equals("/WinAnsiEncoding"))
            map = winansiByteToChar;
        else
            map = pdfEncodingByteToChar; // use PDFEncoding

        for (int k = offset; k < dest; k++)
            c[i++] = (char)map[bytes[k] & 0xff];
        return new String(c);
    }
    /** Checks is <CODE>text</CODE> only has PdfDocEncoding characters.
     * @param text the <CODE>String</CODE> to test
     * @return <CODE>true</CODE> if only PdfDocEncoding characters are present
     */
    public static boolean isPdfDocEncoding(String text) {
        if (text == null)
            return true;
        int len = text.length();
        for (int k = 0; k < len; ++k) {
            char char1 = text.charAt(k);
            if (char1 < 128 || char1 > 160 && char1 <= 255)
                continue;
            if (!pdfEncoding.containsKey(char1))
                return false;
        }
        return true;
    }


    public static final byte[] parseHexStream(PDFRawData src, ParsingContext context) throws EParseError {
        int ch, n, n1 = 0;
        boolean first = true;

        //src.pos++; // Skip the opening bracket '<'

        ByteBuffer out = context.tmpBuffer;
        out.reset();
        for (int i = src.pos; i < src.length; i++) {
            ch = src.src[i] & 0xFF;

            if (ch == 0x3E) { // '>' - EOD
                src.pos = i + 1;
                if (!first)
                    out.append((byte)(n1 << 4));
                return out.toByteArray();
            }
            // whitespace ?
            if ((ch == 0x00) || (ch == 0x09) || (ch == 0x0A) || (ch == 0x0C) || (ch == 0x0D) || (ch == 0x20))
                continue;

            if ((ch < 0x30) || (ch > 0x66))
                throw new EParseError("Illegal character in hex string");

            n = HEX2V[ch - 0x30];
            if (n < 0)
                throw new EParseError("Illegal character in hex string");

            if (first)
                n1 = n;
            else
                out.append((byte)((n1 << 4) + n));
            first = !first;
        }

        throw new EParseError("Unterminated hexadecimal string"); // ">"
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        if (obj instanceof COSString)
        {
            COSString strObj = (COSString) obj;
            return this.getValue().equals(strObj.getValue()) && this.forceHexForm == strObj.forceHexForm;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int result = getValue().hashCode();
        return result += forceHexForm ? 17 : 0;
    }

}
