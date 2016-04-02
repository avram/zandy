
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

package org.pdfparse.filter;


import org.pdfparse.exception.EDecoderException;
import org.pdfparse.parser.PDFParser;
import org.pdfparse.parser.PDFRawData;
import org.pdfparse.parser.ParsingContext;
import org.pdfparse.cos.*;
import org.pdfparse.exception.EParseError;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class StreamDecoder {

    public static interface FilterHandler{
        public byte[] decode(byte[] b, COSName filterName, COSObject decodeParams, final COSDictionary streamDictionary, ParsingContext context) throws EParseError;
    }

    private static final Map<COSName, FilterHandler> defaults;
    static {
        HashMap<COSName, FilterHandler> map = new HashMap<COSName, FilterHandler>();

        map.put(COSName.FLATEDECODE, new Filter_FLATEDECODE());
        map.put(COSName.FL, new Filter_FLATEDECODE());
        map.put(COSName.ASCIIHEXDECODE, new Filter_ASCIIHEXDECODE());
        map.put(COSName.AHX, new Filter_ASCIIHEXDECODE());
        map.put(COSName.ASCII85DECODE, new Filter_ASCII85DECODE());
        map.put(COSName.A85, new Filter_ASCII85DECODE());
        map.put(COSName.LZWDECODE, new Filter_LZWDECODE());
        //map.put(COSName.CCITTFAXDECODE, new Filter_CCITTFAXDECODE());
        map.put(COSName.CRYPT, new Filter_DoNothing());
        map.put(COSName.RUNLENGTHDECODE, new Filter_RUNLENGTHDECODE());

        // ignore this filters
        map.put(COSName.DCTDECODE, new Filter_DoNothing());
        map.put(COSName.JPXDECODE, new Filter_DoNothing());
        map.put(COSName.CCITTFAXDECODE, new Filter_DoNothing());
        map.put(COSName.JBIG2DECODE, new Filter_DoNothing());

        defaults = Collections.unmodifiableMap(map);
    }



    public static byte[] FLATEDecode(final byte[] src) {
        byte[] buf = new byte[1024];

        Inflater decompressor = new Inflater();
        decompressor.setInput(src);

        // Create an expandable byte array to hold the decompressed data
        ByteArrayOutputStream bos = new ByteArrayOutputStream(src.length);

        try {
            while (!decompressor.finished()) {
                int count = decompressor.inflate(buf);
                bos.write(buf, 0, count);
            }
        } catch (DataFormatException e) {
          decompressor.end();
          throw new EDecoderException("FlateDecode error", e);
        }
        decompressor.end();

        return bos.toByteArray();
    }

    /** Decodes a stream that has the LZWDecode filter.
     * @param in the input data
     * @return the decoded data
     */
    public static byte[] LZWDecode(final byte in[]) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LZWDecoder lzw = new LZWDecoder();
        lzw.decode(in, out);
        return out.toByteArray();
    }

    /** Decodes a stream that has the ASCIIHexDecode filter.
     * @param in the input data
     * @return the decoded data
     */
    public static byte[] ASCIIHexDecode(final byte in[], ParsingContext context) throws EParseError {
        PDFRawData data = new PDFRawData();
        data.src = in;
        data.length = in.length;
        data.pos = 0;

        return COSString.parseHexStream(data, context);
    }

    /** Decodes a stream that has the ASCII85Decode filter.
     * @param in the input data
     * @return the decoded data
     */
    public static byte[] ASCII85Decode(final byte in[]) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int state = 0;
        int chn[] = new int[5];
        for (int k = 0; k < in.length; ++k) {
            int ch = in[k] & 0xff;
            if (ch == '~')
                break;

            if (PDFRawData.isWhitespace(ch))
                continue;
            if (ch == 'z' && state == 0) {
                out.write(0);
                out.write(0);
                out.write(0);
                out.write(0);
                continue;
            }
            if (ch < '!' || ch > 'u')
                throw new EDecoderException("Illegal character in ascii85decode (#%d)", ch);
            chn[state] = ch - '!';
            ++state;
            if (state == 5) {
                state = 0;
                int r = 0;
                for (int j = 0; j < 5; ++j)
                    r = r * 85 + chn[j];
                out.write((byte)(r >> 24));
                out.write((byte)(r >> 16));
                out.write((byte)(r >> 8));
                out.write((byte)r);
            }
        }
        int r = 0;
        // We'll ignore the next two lines for the sake of perpetuating broken PDFs
//        if (state == 1)
//            throw new RuntimeException("illegal.length.in.ascii85decode");
        if (state == 2) {
            r = chn[0] * 85 * 85 * 85 * 85 + chn[1] * 85 * 85 * 85 + 85 * 85 * 85  + 85 * 85 + 85;
            out.write((byte)(r >> 24));
        }
        else if (state == 3) {
            r = chn[0] * 85 * 85 * 85 * 85 + chn[1] * 85 * 85 * 85  + chn[2] * 85 * 85 + 85 * 85 + 85;
            out.write((byte)(r >> 24));
            out.write((byte)(r >> 16));
        }
        else if (state == 4) {
            r = chn[0] * 85 * 85 * 85 * 85 + chn[1] * 85 * 85 * 85  + chn[2] * 85 * 85  + chn[3] * 85 + 85;
            out.write((byte)(r >> 24));
            out.write((byte)(r >> 16));
            out.write((byte)(r >> 8));
        }
        return out.toByteArray();
    }

    public static PDFRawData decodeStream(byte[] src, COSDictionary dic, ParsingContext context) throws EParseError {
        // Decompress stream
        COSObject objFilter = dic.get(COSName.FILTER);
        if (objFilter != null) {
            COSArray filters = new COSArray();
            if (objFilter instanceof COSName)
                filters.add((COSName)objFilter);
            else if (objFilter instanceof COSArray)
                filters.addAll((COSArray)objFilter);

            byte[] bytes = src;
            for (int i=0; i<filters.size(); i++) {
                COSName currFilterName = (COSName)filters.get(i);
                FilterHandler fhandler = defaults.get(currFilterName);
                if (fhandler == null)
                    throw new EParseError("Stream filter not supported: " + currFilterName.toString());

                bytes = fhandler.decode(bytes, currFilterName, dic.get(COSName.DECODEPARMS), dic, context);
            }
            PDFRawData pd = new PDFRawData();
            pd.length = bytes.length;
            pd.pos = 0;
            pd.src = bytes;
            return pd;
        }
        PDFRawData pd = new PDFRawData();
        pd.length = src.length;
        pd.pos = 0;
        pd.src = src;
        return pd;

//            if (filter.equals(COSName.FLATEDECODE)) {
//               src = FLATEDecode(src);
//            } else if (filter.equals(COSName.DCTDECODE)) {
//                // do nothing
//            } else
//            throw new EParseError("Stream filter not supported: " + filter.toString());
//        }
//        // ------------
//        COSDictionary decodeParams = dic.getDictionary(COSName.DECODEPARMS, null);
//        if (decodeParams != null)
//          src = unpredictStream(src, decodeParams.getInt(COSName.PREDICTOR, 1), decodeParams.getInt(COSName.COLUMNS, 0));
//
//        PDFRawData pd = new PDFRawData();
//        pd.length = src.length;
//        pd.pos = 0;
//        pd.src = src;
//        return pd;
    }

    public static PDFRawData decodeStream(PDFRawData src, COSDictionary dic, ParsingContext context) throws EParseError {
        byte[] bstream =  // TODO: implement max verbosity mode
            PDFParser.fetchStream(src, dic.getUInt(COSName.LENGTH, 0), false);
        return decodeStream(bstream, dic, context);

    }


    /**
     * @param in_out
     * @param dic
     * @return a new length
     */
    public static byte[] decodePredictor (byte in_out[], final COSDictionary dic, ParsingContext context) {
        int predictor = dic.getInt(COSName.PREDICTOR, -1);
        if (predictor < 0)
            return in_out;

        if (predictor < 10 && predictor != 2)
            return in_out;

        int width = dic.getInt(COSName.COLUMNS, 1);
        int colors = dic.getInt(COSName.COLORS, 1);
        int bpc = dic.getInt(COSName.BITSPERCOMPONENT, 8);

        int bytesPerPixel = colors * bpc / 8;
        int bytesPerRow = (colors*width*bpc + 7)/8;

        if (predictor == 2) {
            if (bpc == 8) {
                int numRows = in_out.length / bytesPerRow;
                for (int row = 0; row < numRows; row++) {
                    int rowStart = row * bytesPerRow;
                    for (int col = 0 + bytesPerPixel; col < bytesPerRow; col++) {       // TODO: Check documentation (BUG ?)
                        int idx = rowStart + col;
                        in_out[idx] = (byte)(in_out[idx] + in_out[idx - bytesPerPixel]);
                    }
                }
            }
            return in_out;
        }

        if (!context.softAssertFormatError(in_out.length > bytesPerPixel, "Data to small for decoding PNG prediction") ) {
            return in_out;
        }


        int filter = 0;
        int curr_in_idx = 0, curr_out_idx = 0, prior_idx = 0;
        // Decode the first line -------------------
        filter = in_out[curr_in_idx++];

        switch (filter) {
            case 0: //PNG_FILTER_NONE
            case 2: //PNG_FILTER_UP
                //curr[i] += prior[i];
                for (int i = 0; i < bytesPerRow; i++)
                    in_out[curr_out_idx + i] = in_out[curr_in_idx + i];
                break;
            case 1: //PNG_FILTER_SUB
            case 4: //PNG_FILTER_PAETH
                //curr[i] += curr[i - bytesPerPixel];
                for (int i = 0; i < bytesPerPixel; i++)
                    in_out[curr_out_idx + i] = in_out[curr_in_idx + i];

                for (int i = bytesPerPixel; i < bytesPerRow; i++)
                    in_out[curr_out_idx + i] = (byte)((in_out[curr_out_idx + i - bytesPerPixel]&0xff + in_out[curr_in_idx + i]&0xff)&0xff);
                break;
            case 3: //PNG_FILTER_AVERAGE
                for (int i = 0; i < bytesPerPixel; i++)
                    in_out[curr_out_idx + i] = in_out[curr_in_idx + i];

                for (int i = bytesPerPixel; i < bytesPerRow; i++)
                    in_out[curr_out_idx + i] = (byte) ((in_out[curr_in_idx + i - bytesPerPixel] & 0xff)/2);
                break;
            default:
                // Error -- unknown filter type
                throw new EDecoderException("PNG filter unknown (%d)", filter);
        }
        curr_in_idx += bytesPerRow;
        curr_out_idx += bytesPerRow;

        //-------------------------


        // Decode the (sub)image row-by-row
        while (true) {
             if (curr_in_idx >= in_out.length)
                 break;

             filter = in_out[curr_in_idx++];

            switch (filter) {
                case 0: //PNG_FILTER_NONE
                    for (int i = 0; i < bytesPerPixel; i++)
                        in_out[curr_out_idx + i] = in_out[curr_in_idx + i];
                    break;
                case 1: //PNG_FILTER_SUB
                    //curr[i] += curr[i - bytesPerPixel];
                    for (int i = 0; i < bytesPerPixel; i++)
                        in_out[curr_out_idx + i] = in_out[curr_in_idx + i];

                    for (int i = bytesPerPixel; i < bytesPerRow; i++)
                        in_out[curr_out_idx + i] = (byte)(((in_out[curr_out_idx + i - bytesPerPixel]&0xff) + (in_out[curr_in_idx + i]&0xff))&0xff);
                    break;
                case 2: //PNG_FILTER_UP
                    for (int i = 0; i < bytesPerRow; i++) {
                        //curr[i] += prior[i];
                        in_out[curr_out_idx + i] = (byte) ((in_out[curr_in_idx + i]&0xff) + (in_out[prior_idx + i]&0xff)&0xff);
                    }
                    break;
                case 3: //PNG_FILTER_AVERAGE
                    for (int i = 0; i < bytesPerPixel; i++) {
                        //curr[i] += prior[i] / 2;
                        in_out[curr_out_idx + i] += (byte) (((in_out[curr_in_idx + i]&0xff) + (in_out[prior_idx + i]&0xff) / 2)&0xff);
                    }
                    for (int i = bytesPerPixel; i < bytesPerRow; i++) {
                        //curr[i] += ((curr[i - bytesPerPixel] & 0xff) + (prior[i] & 0xff))/2;
                        in_out[curr_out_idx + i] += (byte) ((
                                (in_out[curr_out_idx + i - bytesPerPixel] & 0xff)+(in_out[prior_idx + i] & 0xff))/2);
                    }
                    break;
                case 4: //PNG_FILTER_PAETH
                    for (int i = 0; i < bytesPerPixel; i++) {
                        //curr[i] += prior[i];
                        in_out[curr_out_idx + i] = (byte) (((in_out[curr_in_idx + i]&0xff) + (in_out[prior_idx + i]&0xff)&0xff));
                    }

                    for (int i = bytesPerPixel; i < bytesPerRow; i++) {
                        //int a = curr[i - bytesPerPixel] & 0xff;
                        //int b = prior[i] & 0xff;
                        //int c = prior[i - bytesPerPixel] & 0xff;
                        int a = in_out[curr_out_idx + i - bytesPerPixel] & 0xFF;
                        int b = in_out[prior_idx + i] & 0xFF;
                        int c = in_out[prior_idx + i - bytesPerPixel] & 0xFF;

                        int p = a + b - c;
                        int pa = Math.abs(p - a);
                        int pb = Math.abs(p - b);
                        int pc = Math.abs(p - c);

                        int ret;

                        if (pa <= pb && pa <= pc) {
                            ret = a;
                        } else if (pb <= pc) {
                            ret = b;
                        } else {
                            ret = c;
                        }
                        //curr[i] += (byte)ret;
                        in_out[curr_out_idx + i] += (byte)ret;
                    }
                    break;
                default:
                    // Error -- unknown filter type
                    throw new EDecoderException("PNG filter unknown (%d)", filter);
            }

            // Swap curr and prior
            prior_idx = curr_out_idx;
            curr_in_idx += bytesPerRow;
            curr_out_idx += bytesPerRow;
        } // while (true) ...

        byte[] res = new byte[curr_out_idx];
        System.arraycopy(in_out,0, res, 0, res.length);
        return res;
    }

    /**
     * Handles FLATEDECODE filter
     */
    private static class Filter_FLATEDECODE implements FilterHandler{
        public byte[] decode(byte[] b, COSName filterName, COSObject decodeParams, COSDictionary streamDictionary, ParsingContext context) throws EParseError {
            b = StreamDecoder.FLATEDecode(b);
            if (decodeParams != null)
                b = StreamDecoder.decodePredictor(b, (COSDictionary)decodeParams, context);
            return b;
        }
    }

    /**
     * Handles ASCIIHEXDECODE filter
     */
    private static class Filter_ASCIIHEXDECODE implements FilterHandler{
        public byte[] decode(byte[] b, COSName filterName, COSObject decodeParams, COSDictionary streamDictionary, ParsingContext context) throws EParseError {
            b = StreamDecoder.ASCIIHexDecode(b, context);
            return b;
        }
    }

    /**
     * Handles ASCIIHEXDECODE filter
     */
    private static class Filter_ASCII85DECODE implements FilterHandler{
        public byte[] decode(byte[] b, COSName filterName, COSObject decodeParams, COSDictionary streamDictionary, ParsingContext context) throws EParseError {
            b = StreamDecoder.ASCII85Decode(b);
            return b;
        }
    }

    /**
     * Handles LZWDECODE filter
     */
    private static class Filter_LZWDECODE implements FilterHandler{
        public byte[] decode(byte[] b, COSName filterName, COSObject decodeParams, COSDictionary streamDictionary, ParsingContext context) throws EParseError {
            b = StreamDecoder.LZWDecode(b);
            if (decodeParams != null)
                b = StreamDecoder.decodePredictor(b, (COSDictionary)decodeParams, context);
            return b;
        }
    }


    /**
     * A filter that doesn't modify the stream at all
     */
    private static class Filter_DoNothing implements FilterHandler{
        public byte[] decode(byte[] b, COSName filterName, COSObject decodeParams, COSDictionary streamDictionary, ParsingContext context) throws EParseError {
            return b;
        }
    }

    /**
     * Handles RUNLENGTHDECODE filter
     */
    private static class Filter_RUNLENGTHDECODE implements FilterHandler{

        public byte[] decode(byte[] b, COSName filterName, COSObject decodeParams, COSDictionary streamDictionary, ParsingContext context) throws EParseError {
         // allocate the output buffer
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte dupCount = -1;
            for(int i = 0; i < b.length; i++){
                dupCount = b[i];
                if (dupCount == -128) break; // this is implicit end of data

                if (dupCount >= 0 && dupCount <= 127){
                    int bytesToCopy = dupCount+1;
                    baos.write(b, i, bytesToCopy);
                    i+=bytesToCopy;
                } else {
                    // make dupcount copies of the next byte
                    i++;
                    for(int j = 0; j < 1-(int)(dupCount);j++){
                        baos.write(b[i]);
                    }
                }
            }

            return baos.toByteArray();
        }
    }

}
