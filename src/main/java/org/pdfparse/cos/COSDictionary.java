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

import org.pdfparse.*;
import org.pdfparse.cds.PDFRectangle;
import org.pdfparse.parser.PDFParser;
import org.pdfparse.parser.PDFRawData;
import org.pdfparse.parser.ParsingContext;
import org.pdfparse.parser.ParsingGetObject;
import org.pdfparse.utils.DateConverter;
import org.pdfparse.exception.EParseError;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.LinkedHashMap;

public class COSDictionary extends LinkedHashMap<COSName, COSObject> implements COSObject {
    //private LinkedHashMap<String, COSObject> map;
    private static final byte[] S_OPEN = {0x3C, 0x3C};
    private static final byte[] S_OPEN_PP = {0x3C, 0x3C, 0xA};
    private static final byte[] S_CLOSE = {0x3E, 0x3E};
    private static final byte[] S_CLOSE_PP = {0x3E, 0x3E, 0xA};
    private static final byte[] S_NULL = {0x6E, 0x75, 0x6C, 0x6C}; // "null"

    public COSDictionary() {
        super();
    }

    public COSDictionary(COSDictionary src, ParsingContext context) {
        super.putAll(src);
    }

    public COSDictionary(PDFRawData src, ParsingContext context) throws EParseError {
        parse(src, context);
    }

    @Override
    public void parse(PDFRawData src, ParsingContext context) throws EParseError {
        //throw new UnsupportedOperationException("Not supported yet.");
        src.pos +=2;

        while (src.pos < src.length) {
            src.skipWS();
            if ((src.src[src.pos] == 0x3E)&&(src.src[src.pos+1] == 0x3E)) { // '>'
                src.pos+=2; return;
            }

            src.skipWS();
            COSName name = new COSName(src, context);
            //if ((name.length == 0)||(name[0]!=0x2F)) throw new Error('This token is not a name: ' + name.toString()); // '/'
            COSObject obj = PDFParser.parseObject(src, context);
            this.put(name, obj);
        }
        throw new EParseError("Reach end of file while parsing dictionary");
    }

    @Override
    public void produce(OutputStream dst, ParsingContext context) throws IOException {
        COSObject obj;
        if (PDFDefines.PRETTY_PRINT)
            dst.write(S_OPEN_PP); // "<<\n"
        else dst.write(S_OPEN); // "<<"

        for(COSName key: this.keySet()) {
            key.produce(dst, context);
            dst.write(0x20);
            obj = this.get(key);
            if (obj == null)
                dst.write(S_NULL);
            else
                obj.produce(dst, context);
            if (PDFDefines.PRETTY_PRINT)
                dst.write(0xA);
        }

        dst.write(S_CLOSE);
    }

    @Override
    public String toString() {
        return String.format("<< %d >>", this.size());
    }

    private COSObject travel(COSObject obj, ParsingGetObject cache) throws EParseError {
        int counter = 5;
        while (obj instanceof COSReference) {
            obj = cache.getObject((COSReference)obj);
            if (counter-- == 0)
                throw new EParseError("Infinite or too deep loop for " + obj.toString());
        }
        return obj;
    }
    public static COSObject fetchValue(PDFRawData src) {
        return null;
    }

//    public void clear() {
//        map.clear();
//    }

//    public boolean containsKey(String key) {
//        return map.containsKey(key);
//    }

//    public COSObject get(String key) {
//        return map.get(key);
//    }

//    public void set(String key, COSObject value) {
//        map.put(key, value);
//    }

    public boolean getBool(COSName name, boolean def_value) {
        COSObject obj = this.get(name);
        if (obj == null) return def_value;
        if (obj instanceof COSBool) return ((COSBool)obj).value;
        else return def_value;
    }

    public boolean getBool(COSName name, ParsingGetObject cache, boolean def_value) throws EParseError {
        COSObject obj = this.get(name);
        if (obj == null) return def_value;
        if (obj instanceof COSReference)
            obj = travel(obj, cache);
        if (obj instanceof COSBool) return ((COSBool)obj).value;
        else return def_value;
    }

    public void setBool(COSName name, boolean value) {
        COSBool v = new COSBool(value);
        this.put(name, v);
    }

    public int getInt(COSName name, int def_value) {
        COSObject obj = this.get(name);
        if (obj == null) return def_value;
        if (obj instanceof COSNumber) return ((COSNumber)obj).intValue();
        else return def_value;
    }
    public int getInt(COSName name, ParsingGetObject cache, int def_value) throws EParseError {
        COSObject obj = this.get(name);
        if (obj == null) return def_value;
        if (obj instanceof COSReference)
            obj = travel(obj, cache);
        if (obj instanceof COSNumber) return ((COSNumber)obj).intValue();
        else return def_value;
    }
    public int getUInt(COSName name, int def_value) {
        return getInt(name, def_value);
    }
    public int getUInt(COSName name, ParsingGetObject cache, int def_value) throws EParseError {
        return getInt(name, cache, def_value);
    }

    public void setInt(COSName name, int value) {
        COSNumber v = new COSNumber(value);
        this.put(name, v);
    }

    public void setUInt(COSName name, int value) {
        setInt(name, value);
    }

    public String getStr(COSName name, String def_value) {
        COSObject obj = this.get(name);
        if (obj == null) return def_value;
        if (obj instanceof COSString) return ((COSString)obj).getValue();
        else return def_value;

    }

    public String getStr(COSName name, ParsingGetObject cache, String def_value) throws EParseError {
        COSObject obj = this.get(name);
        if (obj == null) return def_value;
        if (obj instanceof COSReference)
            obj = travel(obj, cache);
        if (obj == null) return def_value;
        if (obj instanceof COSString) return ((COSString)obj).getValue();
        return def_value;
    }
    public void setStr(COSName name, String value) {
       this.put(name, new COSString(value));
    }

    public void setDate(COSName name, Calendar date) {
        setStr(name, DateConverter.toString(date));
    }

    public Calendar getDate(COSName name, Calendar def_value) throws EParseError {
        String date = getStr(name, "");
        if (date.equals("")) return null;
        return DateConverter.toCalendar( date );
    }

    public Calendar getDate(COSName name, ParsingGetObject cache, Calendar def_value) throws EParseError {
        String date = getStr(name, cache,  "");
        if (date.equals("")) return null;
        return DateConverter.toCalendar( date );
    }

    public COSName getName(COSName name, COSName def_value) {
        COSObject obj = this.get(name);
        if (obj == null) return def_value;
        if (obj instanceof COSName) return (COSName)obj;
        else return def_value;
    }

    public COSName getName(COSName name, ParsingGetObject cache, COSName def_value) throws EParseError {
        COSObject obj = this.get(name);
        if (obj == null) return def_value;
        if (obj instanceof COSReference)
            obj = travel(obj, cache);
        if (obj instanceof COSName) return (COSName)obj;
        else return def_value;
    }

    public String getNameAsStr(COSName name, ParsingGetObject cache, String def_value) throws EParseError {
        COSObject obj = this.get(name);
        if (obj == null) return def_value;
        if (obj instanceof COSReference)
            obj = travel(obj, cache);
        if (obj instanceof COSName) return ((COSName)obj).asString();
        else return def_value;
    }

    public void setName(COSName name, COSName value) {
       this.put(name, value);
    }

    public COSArray getArray(COSName name, COSArray def_value) {
        COSObject obj = this.get(name);
        if (obj == null) return def_value;
        if (obj instanceof COSArray) return (COSArray)obj;
        else return def_value;
    }
    public COSArray getArray(COSName name, ParsingGetObject cache, COSArray def_value) throws EParseError {
        COSObject obj = this.get(name);
        if (obj == null) return def_value;
        if (obj instanceof COSReference)
            obj = travel(obj, cache);
        if (obj instanceof COSArray) return (COSArray)obj;
        else return def_value;
    }

    public COSDictionary getDictionary(COSName name, COSDictionary def_value) {
        COSObject obj = this.get(name);
        if (obj == null) return def_value;
        if (obj instanceof COSDictionary) return (COSDictionary)obj;
        else return def_value;
    }
    public COSDictionary getDictionary(COSName name, ParsingGetObject cache, COSDictionary def_value) throws EParseError {
        COSObject obj = this.get(name);
        if (obj == null) return def_value;
        if (obj instanceof COSReference)
            obj = travel(obj, cache);
        if (obj instanceof COSDictionary) return (COSDictionary)obj;
        else return def_value;
    }

    public byte[] getBlob(COSName name, byte[] def_value) {
        throw new UnsupportedOperationException("Not supported yet.");
        //COSObject obj = this.get(name);
        //if (obj == null) return def_value;
        //if (obj instanceof COSDictionary) return (COSDictionary)obj;
        //else return def_value;
    }

    public void setReference(COSName name, COSReference value) {
        this.put(name, value);
    }

    public void setReference(COSName name, int id, int gen) {
        COSReference ref = new COSReference(id, gen);
        this.put(name, ref);
    }

    public COSReference getReference(COSName name) {
        COSObject obj = this.get(name);
        if (obj == null) return null;
        if (obj instanceof COSReference) return (COSReference)obj;
        else return null;
    }

    public PDFRectangle getRectangle(COSName name) {
        COSObject obj = this.get(name);
        if (obj == null) return null;
        if (obj instanceof COSArray) {
            PDFRectangle rect = new PDFRectangle((COSArray)obj);
            this.put(name, rect); // override existing COSArray with rectangle
            return rect;
        }
        if (obj instanceof PDFRectangle) return (PDFRectangle)obj;
        else return null;
    }

    public void setRectangle(COSName name, PDFRectangle value) {
        this.put(name, value);
    }

}
