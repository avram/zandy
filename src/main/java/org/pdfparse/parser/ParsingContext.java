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

import org.pdfparse.cos.COSReference;
import org.pdfparse.exception.EParseError;
import org.pdfparse.utils.ByteBuffer;

public class ParsingContext {
    private boolean checkSyntaxCompliance = false;
    private boolean ignoreErrors = false;
    private boolean ignoreBasicSyntaxErrors = false;


    public boolean allowScan = true;
    public int headerLookupRange = 100;
    public int eofLookupRange = 100;

    public ByteBuffer tmpBuffer = new ByteBuffer(1024);
    public COSReference tmpReference = new COSReference(0, 0);

    public ParsingGetObject objectCache;
    public boolean useEncryption;
    public byte[] encryptionKey;

    public ParsingContext() {

    }

    public void checkAndLog(boolean canContinue, String message) {
       if (canContinue)
           System.err.println(message);
       else
           throw new EParseError(message);
    }

    public boolean softAssertSyntaxComliance(boolean condition, String message) {
        if (!condition)
            checkAndLog(checkSyntaxCompliance, message);
        return condition;
    }

    public boolean softAssertFormatError(boolean condition, String message) {
        if (!condition)
            checkAndLog(ignoreBasicSyntaxErrors, message);
        return condition;
    }

    public boolean softAssertStructure(boolean condition, String message) {
        if (!condition)
            checkAndLog(ignoreErrors, message);
        return condition;
    }
}
