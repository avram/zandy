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

package org.pdfparse.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;


/**
 * Acts like a <CODE>StringBuffer</CODE> but works with <CODE>byte</CODE> arrays.
 */

public class ByteBuffer extends OutputStream {
    /** The count of bytes in the buffer. */
    protected int count;

    /** The buffer where the bytes are stored. */
    protected byte buf[];

    /** Creates new ByteBuffer with capacity 128 */
    public ByteBuffer() {
        this(128);
    }

    /**
     * Creates a byte buffer with a certain capacity.
     * @param size the initial capacity
     */
    public ByteBuffer(int size) {
        if (size < 1)
            size = 128;
        buf = new byte[size];
    }


    /**
     * Appends an <CODE>int</CODE>. The size of the array will grow by one.
     * @param b the int to be appended
     * @return a reference to this <CODE>ByteBuffer</CODE> object
     */
    public ByteBuffer append(int b) {
        int newcount = count + 1;
        if (newcount > buf.length) {
            byte newbuf[] = new byte[Math.max(buf.length << 1, newcount)];
            System.arraycopy(buf, 0, newbuf, 0, count);
            buf = newbuf;
        }
        buf[count] = (byte)b;
        count = newcount;
        return this;
    }

    /**
     * Appends the subarray of the <CODE>byte</CODE> array. The buffer will grow by
     * <CODE>len</CODE> bytes.
     * @param b the array to be appended
     * @param off the offset to the start of the array
     * @param len the length of bytes to append
     * @return a reference to this <CODE>ByteBuffer</CODE> object
     */
    public ByteBuffer append(byte b[], int off, int len) {
        if ((off < 0) || (off > b.length) || (len < 0) ||
        ((off + len) > b.length) || ((off + len) < 0) || len == 0)
            return this;
        int newcount = count + len;
        if (newcount > buf.length) {
            byte newbuf[] = new byte[Math.max(buf.length << 1, newcount)];
            System.arraycopy(buf, 0, newbuf, 0, count);
            buf = newbuf;
        }
        System.arraycopy(b, off, buf, count, len);
        count = newcount;
        return this;
    }

    /**
     * Appends an array of bytes.
     * @param b the array to be appended
     * @return a reference to this <CODE>ByteBuffer</CODE> object
     */
    public ByteBuffer append(byte b[]) {
        return append(b, 0, b.length);
    }

    /**
     * Appends a <CODE>String</CODE> to the buffer. The <CODE>String</CODE> is
     * converted platform's default charset.
     * @param str the <CODE>String</CODE> to be appended
     * @return a reference to this <CODE>ByteBuffer</CODE> object
     */
    public ByteBuffer append(String str) {
        if (str != null)
            return append(str.getBytes());
        return this;
    }

    /**
     * Appends a <CODE>char</CODE> to the buffer. The <CODE>char</CODE> is
     * converted according to the encoding ISO-8859-1.
     * @param c the <CODE>char</CODE> to be appended
     * @return a reference to this <CODE>ByteBuffer</CODE> object
     */
    public ByteBuffer append(char c) {
        return append((int)c);
    }

    /**
     * Appends another <CODE>ByteBuffer</CODE> to this buffer.
     * @param buf the <CODE>ByteBuffer</CODE> to be appended
     * @return a reference to this <CODE>ByteBuffer</CODE> object
     */
    public ByteBuffer append(ByteBuffer buf) {
        return append(buf.buf, 0, buf.count);
    }

    /**
     * Sets the size to zero.
     */
    public void reset() {
        count = 0;
    }

    /**
    * Clear memory
    */
    public void clear() {
        count = 0;
        buf = new byte[128];
    }

    /**
     * Creates a newly allocated byte array. Its size is the current
     * size of this output stream and the valid contents of the buffer
     * have been copied into it.
     *
     * @return  the current contents of this output stream, as a byte array.
     */
    public byte[] toByteArray() {
        byte newbuf[] = new byte[count];
        System.arraycopy(buf, 0, newbuf, 0, count);
        return newbuf;
    }

    /**
     * Returns the current size of the buffer.
     *
     * @return the value of the <code>count</code> field, which is the number of valid bytes in this byte buffer.
     */
    public int size() {
        return count;
    }

    public void setSize(int size) {
        if (size > count || size < 0)
            throw new IndexOutOfBoundsException("The new size must be positive and less or equal of the current size");
        count = size;
    }

    /**
     * Converts the buffer's contents into a string, translating bytes into
     * characters according to the platform's default character encoding.
     *
     * @return String translated from the buffer's contents.
     */
    @Override
    public String toString() {
        return new String(buf, 0, count);
    }

    /**
     * Converts the buffer's contents into a string, translating bytes into
     * characters according to the specified character encoding.
     *
     * @param   enc  a character-encoding name.
     * @return String translated from the buffer's contents.
     * @throws UnsupportedEncodingException
     *         If the named encoding is not supported.
     */
    public String toString(String enc) throws UnsupportedEncodingException {
        return new String(buf, 0, count, enc);
    }

    /**
     * Writes the complete contents of this byte buffer output to
     * the specified output stream argument, as if by calling the output
     * stream's write method using <code>out.write(buf, 0, count)</code>.
     *
     * @param      out   the output stream to which to write the data.
     * @exception  IOException  if an I/O error occurs.
     */
    public void writeTo(OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }

    @Override
    public void write(int b) throws IOException {
        append((byte)b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        append(b, off, len);
    }

    public byte[] getBuffer() {
        return buf;
    }

}

