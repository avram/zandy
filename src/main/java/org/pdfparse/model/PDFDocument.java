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

package org.pdfparse.model;

import org.pdfparse.cos.*;
import org.pdfparse.exception.*;
import org.pdfparse.parser.*;
import org.pdfparse.parser.ParsingContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;


public class PDFDocument implements ParsingEvent {
    private String filename;
    private String filepath;
    private boolean loaded;

    private ParsingContext context;
    private PDFParser pdfParser;

    private COSReference rootID = null;
    private COSReference infoID = null;

    private COSDictionary encryption = null;

    private PDFDocInfo documentInfo = null;
    private PDFDocCatalog documentCatalog = null;
    private byte[][] documentId = {null,null};
    private boolean documentIsEncrypted = false;
    private float documentVersion = 0.0f;

    public PDFDocument() {
       context = new ParsingContext();
    }

    public void close() {
        pdfParser.done();
        loaded = false;
    }

    public PDFDocument(String filename) throws EParseError, IOException {
        this();
        File file = new File(filename);
        open(file);
    }

    public PDFDocument(File file) throws EParseError, IOException {
        this();
        open(file);
    }

    public PDFDocument(byte[] buffer) throws EParseError {
        this();

        this.filename = "internal";
        this.filepath = "internal";

        open(buffer);
    }

    private void open(File file) throws EParseError, IOException {
        this.filename = file.getName();
        this.filepath = file.getParent();

        FileInputStream fin = new FileInputStream(file);
        FileChannel channel = fin.getChannel();

        byte[] barray = new byte[(int) file.length()];
        ByteBuffer bb = ByteBuffer.wrap(barray);
        bb.order(ByteOrder.BIG_ENDIAN);
        channel.read(bb);

        open(barray);
    }

    public void open(byte[] buffer) throws EParseError {
        PDFRawData data = new PDFRawData(buffer);
        pdfParser = new PDFParser(data, context, this);
        loaded = true;
    }

    /**
     * Tell if this document is encrypted or not.
     *
     * @return true If this document is encrypted.
     */
    public boolean isEncrypted() {
        return documentIsEncrypted;
    }

    public byte[][] getDocumentId() {
        return documentId;
    }

    /**
     * Get the document info dictionary.  This is guaranteed to not return null.
     *
     * @return The documents /Info dictionary
     */
    public PDFDocInfo getDocumentInfo() throws EParseError {
        if (documentInfo != null)
            return documentInfo;

        COSDictionary dictInfo = null;
        if (infoID != null)
            dictInfo = pdfParser.getDictionary(infoID.id, infoID.gen, false);

        documentInfo = new PDFDocInfo(dictInfo, pdfParser);
        return documentInfo;
    }

    /**
     * This will get the document CATALOG. This is guaranteed to not return null.
     *
     * @return The documents /Root dictionary
     */
    public PDFDocCatalog getDocumentCatalog() throws EParseError {
        if (documentCatalog == null)
        {
            COSDictionary dictRoot;
            dictRoot = pdfParser.getDictionary(rootID, true);

            documentCatalog = new PDFDocCatalog(context, dictRoot);
        }
        return documentCatalog;
    }

    public float getDocumentVersion() {
        return documentVersion;
    }

    @Override
    public int onTrailerFound(COSDictionary trailer, int ordering) {
        if (ordering == 0) {
            rootID = trailer.getReference(COSName.ROOT);
            infoID = trailer.getReference(COSName.INFO);

            documentIsEncrypted = trailer.containsKey(COSName.ENCRYPT);

            COSArray Ids = trailer.getArray(COSName.ID, null);
            if (((Ids == null) || (Ids.size()!=2)) && documentIsEncrypted)
                throw new EParseError("Missing (required) file identifier for encrypted document");

            if (Ids != null) {
                if (Ids.size() != 2) {
                    if (documentIsEncrypted)
                        throw new EParseError("Invalid document ID array size (should be 2)");
                    context.softAssertSyntaxComliance(false, "Invalid document ID array size (should be 2)");

                    Ids = null;
                } else {
                    if ((Ids.get(0) instanceof COSString) && (Ids.get(1) instanceof COSString)) {
                        documentId[0] = ((COSString)Ids.get(0)).getBinaryValue();
                        documentId[1] = ((COSString)Ids.get(1)).getBinaryValue();
                    } else context.softAssertSyntaxComliance(false, "Invalid document ID");
                }
            } // Ids != null
        }
        return ParsingEvent.CONTINUE;
    }

    @Override
    public int onEncryptionDictFound(COSDictionary enc, int ordering) {
        if (ordering == 0)
            encryption = enc;
        return ParsingEvent.CONTINUE;
    }

    @Override
    public int onNotSupported(String msg) {
        //throw new UnsupportedOperationException("Not supported yet.");
        return ParsingEvent.CONTINUE;
    }

    @Override
    public void onDocumentVersionFound(float version) {
        this.documentVersion = version;
    }

    public void dbgDump() {
        //xref.dbgPrintAll();
        pdfParser.parseAndCacheAll();
        //cache.dbgSaveAllStreams(filepath + File.separator + "[" + filename + "]" );
        //cache.dbgSaveAllObjects(filepath + File.separator + "[" + filename + "]" );

    }
}
