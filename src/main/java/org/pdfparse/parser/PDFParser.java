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


import org.pdfparse.PDFDefines;
import org.pdfparse.cos.*;
import org.pdfparse.exception.EParseError;
import org.pdfparse.filter.StreamDecoder;
import org.pdfparse.utils.IntObjHashtable;

import java.util.Arrays;

public class PDFParser implements ParsingGetObject {
    private static final byte[] OBJ = {0x6F, 0x62, 0x6A};
    private static final byte[] ENDOBJ = {0x65, 0x6E, 0x64, 0x6F, 0x62, 0x6A};

    private static final byte[] STREAM = {0x73, 0x74, 0x72, 0x65, 0x61, 0x6D};
    private static final byte[] ENDSTREAM = {0x65, 0x6E, 0x64, 0x73, 0x74, 0x72, 0x65, 0x61, 0x6D};

    private static final byte[] PDF_HEADER = {0x25, 0x50, 0x44, 0x46, 0x2D}; // "%PDF-";
    private static final byte[] FDF_HEADER = {0x25, 0x46, 0x44, 0x46, 0x2D}; // "%FDF-";

    private static final byte[] EOF = {0x25, 0x25, 0x45, 0x4F, 0x46}; // "%%EOF"
    private static final byte[] STARTXREF = {0x73, 0x74, 0x61, 0x72, 0x74, 0x78, 0x72, 0x65, 0x66}; // "startxref"

    private static final byte[] XREF = {0x78, 0x72, 0x65, 0x66};
    private static final byte[] TRAILER = {0x74, 0x72, 0x61, 0x69, 0x6C, 0x65, 0x72};

    private ParsingContext pContext;
    private PDFRawData pdfData;
    private ParsingEvent parsingEvent;

    private IntObjHashtable<XRefEntry> by_id;

    private int max_id = 0;
    private int max_gen = 0;
    private int max_offset = 0;

    private int compressed_max_stream_id = 0;
    private int compressed_max_stream_offs = 0;

    public PDFParser(PDFRawData pData, ParsingContext pContext, ParsingEvent evt) {
        this.pContext = pContext;
        this.pdfData = pData;
        this.pContext.objectCache = this;
        this.parsingEvent = evt;
        by_id = new IntObjHashtable<XRefEntry>();
        parse();
    }

    public void done() {
        pContext = null;
        by_id.clear();
    }

    public void clear() {
        by_id.clear();
        max_id = 0;
        max_gen = 0;
        max_offset = 0;
        compressed_max_stream_id = 0;
        compressed_max_stream_offs = 0;
    }

    /**
     * Parse a PDF file data
     * @param src PDF file data to parse
     * @param evt Listener to broadcast parsing events
     * @return Trailer dictionary
     */
    private COSDictionary parse() {
        PDFRawData src = pdfData;

        if (src.length < 10) {
            throw new EParseError("This is not a valid PDF file");
        }

        // Check the PDF header & version -----------------------
        src.pos = 0;
        if (  !( src.checkSignature(PDF_HEADER) || src.checkSignature(FDF_HEADER) ) ) {
            if (!pContext.allowScan)
                throw new EParseError("This is not a PDF file");

            while ( !(src.checkSignature(PDF_HEADER) || src.checkSignature(FDF_HEADER))
                    && (src.pos < pContext.headerLookupRange) && (src.pos < src.length) ) src.pos++;

            if (  !(src.checkSignature(PDF_HEADER) || src.checkSignature(FDF_HEADER)) )
                throw new EParseError("This is not a PDF file (PDF header not found)");
        }

        if (src.length - src.pos < 10)
            throw new EParseError("This is not a valid PDF file");


        if ((src.src[src.pos + 5] != '1') || (src.src[src.pos + 7] < '1') || (src.src[src.pos + 7] > '8')) {
            throw new EParseError("PDF version is not supported");
        }

        double documentVersion = (src.src[src.pos + 5] - '0') + (src.src[src.pos + 7] - '0') / 10.0;
        parsingEvent.onDocumentVersionFound((float)documentVersion);


        // Scan for EOF -----------------------------------------
        if (src.reverseScan(src.length, EOF, pContext.eofLookupRange) < 0)
            throw new EParseError("Missing end of file marker");

        // Scan for 'startxref' marker --------------------------
        if (src.reverseScan(src.pos, STARTXREF, 100) < 0)
            throw new EParseError("Missing 'startxref' marker");


        // Fetch XREF offset ------------------------------------
        src.pos += 10;
        src.skipWS();

        int xref_offset = COSNumber.readInteger(src);

        if ((xref_offset == 0) || (xref_offset >= src.length)) {
            throw new EParseError("Invalid xref offset");
        }

        src.pos = xref_offset;

        src.skipWS();
        if (src.checkSignature(XREF))
            return parseTableAndTrailer(src, parsingEvent);
        return parseXRefStream(src, false, 0, parsingEvent);
    }

    public XRefEntry getXRefEntry(int id, int gen) {
        return by_id.get(id);
    }

    public XRefEntry getXRefEntry(int id) {
        return by_id.get(id);
    }

//    public Set<Integer> getIdSet() {
//        return by_id.keySet();
//    }

    public COSObject getCOSObject(int id, int gen) throws EParseError {
        COSReference header;

        XRefEntry x = by_id.get(id);  // TODO: what with GEN ?

        if (x == null)
            return new COSNull();

        if (x.cachedObject != null)
            return x.cachedObject;

        if (x.gen != gen) {
            if (PDFDefines.DEBUG)
                System.out.printf("Object with generation %d not found. But there is %d generation number", gen, x.gen);
        }

        if (!x.isCompressed) {
            pdfData.pos = x.fileOffset;
            //-----

            header = this.tryFetchIndirectObjHeader(pdfData, pContext.tmpReference);
            if (header == null)
                throw new EParseError(String.format("Invalid indirect object header (expected '%d %d obj' @ %d)", id, gen, pdfData.pos));
            if ((header.id != id)||(header.gen != gen))
                throw new EParseError(String.format("Object header not correspond data specified in reference (expected '%d %d obj' @ %d)", id, gen, pdfData.pos));
            pdfData.skipWS();
            //-----
            x.cachedObject = this.parseObject(pdfData, pContext);
            return x.cachedObject;
        }

        // Compressed ----------------------------------------------------
        XRefEntry cx = by_id.get(x.containerObjId);
        if (cx == null)
            return new COSNull();

        if (cx.cachedObject == null) { // Extract compressed block (stream object)
            pdfData.pos = cx.fileOffset;
            //-----
            header = this.tryFetchIndirectObjHeader(pdfData, pContext.tmpReference);
            if (header == null)
                throw new EParseError("Invalid indirect object header");
            if ((header.id != x.containerObjId)||(header.gen != 0))
                throw new EParseError("Object header not correspond data specified in reference");
            pdfData.skipWS();
            //-----
            cx.cachedObject = this.parseObject(pdfData, pContext);

            if (! (cx.cachedObject instanceof COSStream))
                throw new EParseError("Referenced object-container is not stream object");
        }

        COSStream streamObject = (COSStream)cx.cachedObject;

        // --- Ok, received streamObject
        // next, decompress its data, and put in cache
        if (cx.decompressedStreamData == null) {
            cx.decompressedStreamData = StreamDecoder.decodeStream(streamObject.getData(), streamObject, pContext);
        }
        PDFRawData streamData = cx.decompressedStreamData;

        // -- OK, retrieved from cache decompressed data
        // Parse stream index & content

        int n = streamObject.getInt(COSName.N, 0);
        int first = streamObject.getInt(COSName.FIRST, 0);
        int idxId, idxOffset, savepos;
        XRefEntry idxXRefEntry;
        COSObject obj = null;
        for (int i=0; i<n; i++) { // Extract all objects within stream
            idxId = streamData.fetchUInt();
            idxOffset = streamData.fetchUInt();

            // check if it is free object
            idxXRefEntry = this.getXRefEntry(idxId);
            if (idxXRefEntry == null)
                continue; // this is a free object. skip it

            if (!idxXRefEntry.isCompressed)
                throw new EParseError(String.format("Something strange. Compressed object #%d marked as regular object in XRef", idxId));

            savepos = streamData.pos;

            streamData.pos = first + idxOffset;
            idxXRefEntry.cachedObject = this.parseObject(streamData, pContext);
            if (idxId == id)
                obj = idxXRefEntry.cachedObject; // found it

            streamData.pos = savepos;
        }

        return obj;
    }

    @Override
    public COSObject getObject(COSReference ref) {
        try {
            int savepos = pdfData.pos;
            COSObject obj = getCOSObject(ref.id, ref.gen);
            pdfData.pos = savepos;
            return obj;
        } catch (EParseError ex) {
            return null;
        }
    }

    @Override
    public COSDictionary getDictionary(COSReference ref) {
        return getDictionary(ref.id, ref.gen, true);
    }

    @Override
    public COSStream getStream(COSReference ref) {
        return getStream(ref.id, ref.gen, true);
    }

    public COSDictionary getDictionary(int id, int gen, boolean strict) throws EParseError {
        COSObject obj = this.getCOSObject(id, gen);
        if (obj instanceof COSDictionary)
            return (COSDictionary)obj;

        if (strict)
            throw new EParseError("Dictionary expected for %d %d R. But retrieved object is %s", id, gen, obj.getClass().getName());
        else return null;
    }
    public COSDictionary getDictionary(COSReference ref, boolean strict) throws EParseError {
        return getDictionary(ref.id, ref.gen, strict);
    }

    public COSStream getStream(int id, int gen, boolean strict) throws EParseError {
        COSObject obj = this.getCOSObject(id, gen);
        if (obj instanceof COSStream)
            return (COSStream)obj;

        if (strict)
            throw new EParseError("Stream expected for %d %d R. But retrieved object is %s", id, gen, obj.getClass().getName());
        else return null;
    }
    public COSStream getStream(COSReference ref, boolean strict) throws EParseError {
        return getStream(ref.id, ref.gen, strict);
    }



    public static COSObject parseObject(PDFRawData src, ParsingContext context) throws EParseError {
        byte ch;

        while(true) {
            // skip spaces if any
            int dlen = src.length;
            ch = src.src[src.pos];
            while ((src.pos < dlen)&&((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D))) {
                src.pos++;
                ch = src.src[src.pos];
            }
            //--------------
            ch = src.src[src.pos];
            switch (ch) {
                case 0x25: // '%' - comment
                    src.skipLine();
                    break;
                case 0x2F: // '/' - name
                    return new COSName(src, context);
                case 0x74: // 't' - true
                    //assert(StrLComp(pCurr, 'true', 4) = 0, 'It is not a "true"');
                    src.pos += 4;
                    return new COSBool(true);
                case 0x66: // 'f' - false
                    // Assert(StrLComp(pCurr, 'false', 5) = 0, 'It is not a "false"');
                    src.pos += 5;
                    return new COSBool(false);
                case 0x6E: // 'n' - null
                    // Assert(StrLComp(pCurr, 'false', 5) = 0, 'It is not a "false"');
                    src.pos += 4;
                    return new COSNull();
                case 0x28: // '(' - raw string
                    return new COSString(src, context);
                case 0x3C: // '<' - hexadecimal string
                    if (src.src[src.pos+1] == 0x3C) { // '<'
                        COSDictionary dict = new COSDictionary(src, context);
                        // check for stream object
                        // TODO: Merge COSDictionary and COSStream into one object(class)
                        src.skipWS();
                        if (!src.checkSignature(STREAM))
                            return dict; // this is COSDictionary only
                        // this is stream object
                        COSStream stm = new COSStream(dict, src, context);
                        dict.clear();
                        dict = null;
                        return stm;
                    }
                    // this is only Hexadecimal string
                    return new COSString(src, context);
                case 0x5B: // '[' - array
                    return new COSArray(src, context);

                case 0x30: case 0x31: case 0x32: case 0x33: case 0x34: // 0..4
                case 0x35: case 0x36: case 0x37: case 0x38: case 0x39: // 5..9
                case 0x2B: case 0x2D: case 0x2E: // '+', '-', '.'
                    COSReference ref = tryFetchReference(src);
                    if (ref != null)
                        return ref; // this is a valid reference
                    return new COSNumber(src, context);
                default:
                    if (PDFDefines.DEBUG)
                        System.out.printf("Bytes before error occurs: %s\r\n", src.dbgPrintBytes());
                    throw new EParseError("Unknown value token at %d", src.pos);
            } // switch
        } // while
    }

    // if next token is not a reference, function return null (without position changes)
    // else it fetches token and change stream position
    private static COSReference tryFetchReference(PDFRawData src) {
        int pos = src.pos;
        int len = src.length;
        int ch;
        int obj_id = 0, obj_gen = 0;

        if (pos >= len) return null;

        // parse int #1 --------------------------------------------
        ch = src.src[pos];
        while ((ch >= 0x30)&&(ch <= 0x39)) {
            obj_id = obj_id*10 + (ch - 0x30);
            pos++; // 0..9
            if (pos >= len) return null;
            ch = src.src[pos];
        }

        //check if not a whitespace or EOF
        if ((pos >= len)||(!((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D)||(ch==0x00))))
            return null;
        pos++; // skip this space
        if (pos >= len) return null;

        // skip succeeded spaces if any
        ch = src.src[pos];
        while ((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D)) {
            pos++;
            if (pos >= len) return null;
            ch = src.src[pos];
        }

        // parse int #2 --------------------------------------------
        while ((ch >= 0x30)&&(ch <= 0x39)) {
            obj_gen = obj_gen*10 + (ch - 0x30);
            pos++;
            if (pos >= len) return null;
            ch = src.src[pos];
        }

        //check if not a whitespace or EOF
        if (!((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D)||(ch==0x00)))
            return null;
        pos++; // skip space
        if (pos >= len) return null;

        // skip succeeded spaces if any
        ch = src.src[pos];
        while ((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D)) {
            pos++;
            if (pos >= len) return null;
            ch = src.src[pos];
        }

        // check if next char is R ---------------------------------
        if (src.src[pos] != 0x52) // 'R'
            return null;

        src.pos = ++pos; // beyond the 'R'

        return new COSReference(obj_id, obj_gen);
    }

    // if next token is not a object header, function return null (without position changes)
    // else it fetches token and change stream position
    private static COSReference tryFetchIndirectObjHeader(PDFRawData src, COSReference outHeader) {
        int pos = src.pos;
        int len = src.length;
        int ch;

        int obj_id = 0, obj_gen = 0;

        if (pos >= len) return null;

        // parse int #1 --------------------------------------------
        ch = src.src[pos];
        while ((ch >= 0x30)&&(ch <= 0x39)) {
            obj_id = obj_id*10 + (ch - 0x30);
            pos++; // 0..9
            if (pos >= len) return null;
            ch = src.src[pos];
        }

        //check if not a whitespace or EOF
        if ((!((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D)||(ch==0x00))))
            return null;
        pos++; // skip this space
        if (pos >= len) return null;

        // skip succeeded spaces if any
        ch = src.src[pos];
        while ((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D)) {
            pos++;
            if (pos >= len) return null;
            ch = src.src[pos];
        }

        // parse int #2 --------------------------------------------
        while ((ch >= 0x30)&&(ch <= 0x39)) {
            obj_gen = obj_gen*10 + (ch - 0x30);
            pos++;
            if (pos >= len) return null;
            ch = src.src[pos];
        }

        //check if not a whitespace or EOF
        if ((!((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D)||(ch==0x00))))
            return null;
        pos++; // skip space
        if (pos >= len) return null;

        // skip succeeded spaces if any
        ch = src.src[pos];
        while ((ch==0x20)||(ch==0x09)||(ch==0x0A)||(ch==0x0D)) {
            pos++;
            if (pos >= len) return null;
            ch = src.src[pos];
        }

        // check if next char is obj ---------------------------------
        if (!src.checkSignature(pos, OBJ)) // 'obj'
            return null;

        src.pos = pos + 3; // beyond the 'obj'

        outHeader.set(obj_id, obj_gen);

        return outHeader;
    }

    public static final byte[] fetchStream(PDFRawData src, int stream_len, boolean movePosBeyoundEndObj) throws EParseError {
        src.skipWS();
        if (!src.checkSignature(STREAM))
            throw new EParseError("'stream' keyword not found");
        src.pos += STREAM.length;
        src.skipCRLForLF();
        if (src.pos + stream_len > src.length)
            throw new EParseError("Unexpected end of file (stream object too large)");

        // TODO: Lazy parse (reference + start + len)
        byte[] res = Arrays.copyOfRange(src.src, src.pos, src.pos + stream_len);
        src.pos += stream_len;

        if (movePosBeyoundEndObj) {
            byte firstbyte = ENDOBJ[0];
            int max_pos = src.length - ENDOBJ.length;
            if (max_pos - src.pos > PDFDefines.MAX_SCAN_RANGE)
                max_pos = src.pos + PDFDefines.MAX_SCAN_RANGE;
            for (int i = src.pos; i < max_pos; i++)
                if ((src.src[i] == firstbyte)&&src.checkSignature(i, ENDOBJ)) {
                    src.pos = i + ENDOBJ.length;
                    return res;
                }

            throw new EParseError("'endobj' tag not found");
        }

        return res;
    }

    private void addXref(int id, int gen, int offs) throws EParseError {
        // Skip invalid or not-used objects (assumed that they are free objects)
        if (offs == 0) {
            if (PDFDefines.DEBUG)
                System.out.printf("XREF: Got object with zero offset. Assumed that this was a free object(%d %d R)\r\n", id, gen);
            return;
        }
        if (offs < 0)
            throw new EParseError("Negative offset for object id=%d", id);

        XRefEntry obj = new XRefEntry();
        obj.id = id;
        obj.gen = gen;
        obj.fileOffset = offs;
        obj.isCompressed = false;

        XRefEntry old_obj = by_id.get(id);

        if (old_obj == null) {
            by_id.put(id, obj);
        } else if (old_obj.gen < gen) {
            by_id.put(id, obj);
        }

        if (max_id < id) max_id = id;
        if (max_offset < offs) max_offset = offs;
        if (max_gen < gen) max_gen = gen;
    }

    private void addXrefCompressed(int id, int containerId, int indexWithinContainer) throws EParseError {
        // Skip invalid or not-used objects (assumed that they are free objects)
        if (containerId == 0) {
            if (PDFDefines.DEBUG)
                System.out.printf("XREF: Got containerId which is zero. Assumed that this was a free object (%d 0 R)\r\n", id);
            return;
        }
        if (indexWithinContainer < 0)
            throw new EParseError(String.format("Negative indexWithinContainer for compressed object id=%d in stream #%d", id, containerId));

        XRefEntry obj = new XRefEntry();
        obj.id = id;
        obj.gen = 0;
        obj.fileOffset = 0;
        obj.isCompressed = true;
        obj.containerObjId = containerId;
        obj.indexWithinContainer = indexWithinContainer;

        by_id.put(id, obj);

        if (compressed_max_stream_id<containerId) compressed_max_stream_id = containerId;
        if (compressed_max_stream_offs<indexWithinContainer) compressed_max_stream_offs = indexWithinContainer;
    }

    private void parseTableOnly(PDFRawData src, boolean override) throws EParseError {
        src.skipWS();
        int start;
        int count;
        int i, p;
        int obj_off;
        int obj_gen;
        boolean obj_use;

        while (true) {
        start = src.fetchUInt(); src.skipWS();
        count = src.fetchUInt(); src.skipWS();

        if (start == 1) { // fix incorrect start number
            p = src.pos;
            obj_off = src.fetchUInt();
            obj_gen = src.fetchUInt();
            if (obj_off == 0 && obj_gen == 65535)
                start--;
            src.pos = p;
        }

        for (i = 0; i < count; i++) {
            obj_off = src.fetchUInt();
            obj_gen = src.fetchUInt();
            src.skipWS();
            obj_use = (src.src[src.pos] == 0x6E);   // 'n'
            src.pos++; // skip flag
            if (!obj_use) continue;

            if (!override) {
              if (by_id.containsKey(start+i)) continue; // TODO: Optimize this
            }
            addXref(start+i, obj_gen, obj_off);
        }
        src.skipWS();
        byte b = src.src[src.pos];
        if ((b < 0x30)||(b > 0x39)) break; // not in [0..9] range
        }// while(1)...
    }

    private COSDictionary parseTableAndTrailer(PDFRawData src, ParsingEvent evt) throws EParseError {
        int prev = src.pos;
        int xrefstrm = 0;
        int res, trailer_ordering = 0;
        COSDictionary curr_trailer = null;
        COSDictionary dic_trailer = null;

        while (prev != 0) {
            src.pos = prev;
            // Parse XREF ---------------------
            if (!src.checkSignature(XREF))
                throw new EParseError("This is not an 'xref' table");
            src.pos += XREF.length;

            parseTableOnly(src, false);
            // Parse Trailer ------------------
            src.skipWS();
            if (!src.checkSignature(TRAILER))
                throw new EParseError("Cannot find 'trailer' tag");
            src.pos += TRAILER.length;
            src.skipWS();

            curr_trailer = new COSDictionary(src, pContext);
            prev = curr_trailer.getInt(COSName.PREV, 0);
            if (trailer_ordering == 0)
                dic_trailer = curr_trailer;

            res = evt.onTrailerFound(curr_trailer, trailer_ordering);
            if ((res & ParsingEvent.ABORT_PARSING) != 0)
                return dic_trailer;

            // TODO: mark encrypted objects for removing
            //-----------------------
            if (trailer_ordering == 0) {
                xrefstrm = curr_trailer.getInt(COSName.XREFSTM, 0);
                if (xrefstrm != 0) { // This is an a hybrid PDF-file
                    //res = parsingEvent.onNotSupported("Hybrid PDF-files not supported");
                    //if ((res&ParsingEvent.CONTINUE) == 0)
                    //    return dic_trailer;

                    src.pos = xrefstrm;
                    parseXRefStream(src, true, trailer_ordering+1, evt);
                }
            }
            trailer_ordering++;
        } // while
        return dic_trailer;
    }

    private COSDictionary parseXRefStream(PDFRawData src, boolean override, int trailer_ordering, ParsingEvent evt) throws EParseError {
        COSDictionary curr_trailer, dic_trailer = null;
        int res, prev;
        while (true) {
            src.skipWS();

            COSReference x = PDFParser.tryFetchIndirectObjHeader(src, pContext.tmpReference);
            if (x == null)
                throw new EParseError("Invalid indirect object header");

            src.skipWS();


            //addXRef(65530, 0, trailerOffset);
            curr_trailer = new COSDictionary(src, pContext);
            if (trailer_ordering == 0)
                dic_trailer = curr_trailer;

            res = evt.onTrailerFound(curr_trailer, trailer_ordering);
            if ((res & ParsingEvent.ABORT_PARSING) != 0)
                return dic_trailer;

            // TODO: Mark 'encrypt' objects for removing

            if (!curr_trailer.getName(COSName.TYPE, null).equals(COSName.XREF))
                throw new EParseError("This is not a XRef stream");


            COSArray oW = curr_trailer.getArray(COSName.W, null);
            if ((oW == null) || (oW.size() != 3))
                throw new EParseError("Invalid PDF file");
            int[] w = {oW.getInt(0), oW.getInt(1), oW.getInt(2)};

            int size = curr_trailer.getUInt(COSName.SIZE, 0);
            COSArray index = curr_trailer.getArray(COSName.INDEX, null);
            if (index == null) {
                index = new COSArray();
                index.add(new COSNumber(0));
                index.add(new COSNumber(size));
            }

            //int row_len = w[0] + w[1] + w[2];

            //byte[] bstream =  // TODO: implement max verbosity mode
            //    src.fetchStream(curr_trailer.getUInt(COSName.LENGTH, 0), false);

            PDFRawData bstream;
            bstream = StreamDecoder.decodeStream(src, curr_trailer, pContext);

            int start;
            int count;
            int index_idx = 0;

            int itype, i2, i3;

            while (index_idx < index.size()) {
                start = index.getInt(index_idx++);
                count = index.getInt(index_idx++);

                int i = 0;
                while (i < count) {
                    if (w[0] != 0) itype = bstream.fetchBinaryUInt(w[0]); else itype = 1; // default value (see specs)
                    if (w[1] != 0) i2 = bstream.fetchBinaryUInt(w[1]); else i2 = 0;
                    if (w[2] != 0) i3 = bstream.fetchBinaryUInt(w[2]); else i3 = 0;

                    switch(itype) {
                    case 0:  // linked list of free objects (corresponding to f entries in a cross-reference table).
                        i++; //TODO: mark as free (delete if exist)
                        continue;
                    case 1: // objects that are in use but are not compressed (corresponding to n entries in a cross-reference table).
                        addXref((start+i), i3, i2);
                        i++;
                        continue;
                    case 2: // compressed objects.
                        addXrefCompressed(start+i, i2, i3);
                        i++;
                        continue;
                    default:
                        //throw new EParseError("Invalid iType entry in xref stream");
                        if (PDFDefines.DEBUG)
                            System.out.printf("Invalid iType entry in xref stream: %d\r\n", itype );
                        continue;
                    }// switch
                }// for
            } // while

            prev = curr_trailer.getInt(COSName.PREV, 0);
            if (prev != 0) {
                if ((prev < 0) || (prev > src.length))
                    throw new EParseError("Invalid trailer offset (%d)", prev);
                src.pos = prev;
                trailer_ordering++;
                continue;
            } else break;
        } // while (true)

        return dic_trailer;

    }

    public void dbgPrintAll() {
        System.out.printf("Max id: %d\r\n", max_id);
        System.out.printf("Max gen: %d\r\n", max_gen);
        System.out.printf("Max offset: %d\r\n", max_offset);
        System.out.printf("Compressed max stream id: %d\r\n", compressed_max_stream_id);
        System.out.printf("Compressed max stream offs: %d\r\n", compressed_max_stream_offs);

        XRefEntry xref;
        int[] keys = by_id.getKeys();
        for (int i = 0; i<keys.length; i++) {
            xref = by_id.get(keys[i]);
            System.out.printf("%d %s\r\n", keys[i], xref.toString());
        }
//        for (Integer id : by_id.keySet()) {
//           xref = by_id.get(id);
//           System.out.printf("%d %s\r\n", id.intValue(), xref.toString());
//        }

    }

//    public void dbgSaveAllStreams(String dir) {
//        File path = new File (dir);
//        path.mkdirs();
//        path = null;
//
//        FileOutputStream f;
//        for (Integer stmId  : decompressedStreams.keySet()) {
//            try {
//                f = new FileOutputStream(dir + File.separator + "stream" + stmId.toString() + ".bin");
//                f.write(decompressedStreams.get(stmId).src);
//                f.flush();
//                f.close();
//            } catch ( IOException ex) {
//                Logger.getLogger(XRef.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
//
//    public void dbgSaveAllObjects(String dir) {
//        final byte[] SEPARATOR = {0xD, 0xA, 0x2D, 0x2D, 0x2D, 0x2D, 0x2D, 0x2D, 0x2D, 0x2D, 0x2D, 0xD, 0xA};
//        File path = new File (dir);
//        path.mkdirs();
//        path = null;
//
//        COSObject obj;
//        FileOutputStream f;
//        ByteArrayOutputStream out;
//        PDFRawData decompressedStm;
//        for (Integer objId  : by_id.keySet()) {
//            try {
//                f = new FileOutputStream(dir + File.separator + "obj" + objId.toString() + ".bin");
//                obj = by_id.get(objId);
//                out = new ByteArrayOutputStream();
//                obj.produce(out, pContext);
//
//                if (obj instanceof COSStream) {
//                    decompressedStm = decompressedStreams.get(objId);
//
//                    if (decompressedStm == null) { // is not in cache?
//                        decompressedStm = StreamDecoder.decodeStream(((COSStream)obj).getData(), (COSStream)obj, pContext);
//                        decompressedStreams.put(objId.intValue(), decompressedStm); // put in cache
//                    }
//
//                    if (decompressedStm != null) {
//                        out.write(SEPARATOR);
//                        out.write(decompressedStm.src);
//                    }
//                }
//                f.write(out.toByteArray());
//                f.flush();
//                f.close();
//                out = null;
//            } catch ( EParseError ex) {
//                Logger.getLogger(XRef.class.getName()).log(Level.SEVERE, null, ex);
//            } catch ( IOException io) {
//                Logger.getLogger(XRef.class.getName()).log(Level.SEVERE, null, io);
//            }
//
//        }
//    }

    public void parseAndCacheAll() {
        XRefEntry xre;

        int[] keys = by_id.getKeys();
        for (int i=0; i<keys.length; i++) {
            xre = this.getXRefEntry(keys[i]);
            this.getCOSObject(xre.id, xre.gen);

        }
//        for (Integer id : this.getIdSet()) {
//            xre = this.getXRefEntry(id);
//            this.getCOSObject(id, xre.gen);
//        }
    }

}
