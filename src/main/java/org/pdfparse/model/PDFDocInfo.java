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

import org.pdfparse.parser.PDFParser;
import org.pdfparse.cos.*;
import org.pdfparse.exception.EParseError;
import java.util.Calendar;

/**
 * This is the document metadata.  Each getXXX method will return the entry if
 * it exists or null if it does not exist.  If you pass in null for the setXXX
 * method then it will clear the value.
 */

public class PDFDocInfo {
    private COSDictionary info;
    private PDFParser pdfParser;
    private boolean owned;


    /**
     * Constructor that is used for a preexisting dictionary.
     *
     * @param dic The underlying dictionary.
     * @param pdfParser Reference to the document parser object.
     */
    public PDFDocInfo( COSDictionary dic, PDFParser pdfParser )
    {
        if (dic == null) {
            dic = new COSDictionary();
            owned = true;
        } else owned = false;

        this.info = dic;
        this.pdfParser = pdfParser;
    }

    /**
     * This will get the underlying dictionary that this object wraps.
     *
     * @return The underlying info dictionary.
     */
    public COSDictionary getDictionary()
    {
        return info;
    }

    /**
     * This will get the title of the document.  This will return null if no title exists.
     *
     * @return The title of the document.
     * @throws EParseError If there is a problem retrieving the title
     */
    public String getTitle() throws EParseError
    {
        return info.getStr(COSName.TITLE, pdfParser, "");
    }

    /**
     * This will set the title of the document.
     *
     * @param title The new title for the document.
     */
    public void setTitle( String title )
    {
        info.setStr( COSName.TITLE, title );
    }

    /**
     * This will get the author of the document.  This will return null if no author exists.
     *
     * @return The author of the document.
     * @throws EParseError If there is a problem retrieving the author
     */
    public String getAuthor() throws EParseError
    {
        return info.getStr( COSName.AUTHOR, pdfParser, "" );
    }

    /**
     * This will set the author of the document.
     *
     * @param author The new author for the document.
     */
    public void setAuthor( String author )
    {
        info.setStr( COSName.AUTHOR, author );
    }

    /**
     * This will get the subject of the document.  This will return null if no subject exists.
     *
     * @return The subject of the document.
     *  @throws EParseError If there is a problem retrieving the subject
     */
    public String getSubject() throws EParseError
    {
        return info.getStr( COSName.SUBJECT, pdfParser, "" );
    }

    /**
     * This will set the subject of the document.
     *
     * @param subject The new subject for the document.
     */
    public void setSubject( String subject )
    {
        info.setStr( COSName.SUBJECT, subject );
    }

    /**
     * This will get the keywords of the document.  This will return null if no keywords exists.
     *
     * @return The keywords of the document.
     * @throws EParseError If there is a problem retrieving keywords
     */
    public String getKeywords() throws EParseError
    {
        return info.getStr( COSName.KEYWORDS, pdfParser, "" );
    }

    /**
     * This will set the keywords of the document.
     *
     * @param keywords The new keywords for the document.
     */
    public void setKeywords( String keywords )
    {
        info.setStr( COSName.KEYWORDS, keywords );
    }

    /**
     * This will get the creator of the document.  This will return null if no creator exists.
     *
     * @return The creator of the document.
     * @throws EParseError If there is a problem retrieving the creator
     */
    public String getCreator() throws EParseError
    {
        return info.getStr( COSName.CREATOR, pdfParser, "" );
    }

    /**
     * This will set the creator of the document.
     *
     * @param creator The new creator for the document.
     */
    public void setCreator( String creator )
    {
        info.setStr( COSName.CREATOR, creator );
    }

    /**
     * This will get the producer of the document.  This will return null if no producer exists.
     *
     * @return The producer of the document.
     * @throws EParseError If there is a problem retrieving the producer
     */
    public String getProducer() throws EParseError
    {
        return info.getStr( COSName.PRODUCER, pdfParser, "" );
    }

    /**
     * This will set the producer of the document.
     *
     * @param producer The new producer for the document.
     */
    public void setProducer( String producer )
    {
        info.setStr( COSName.PRODUCER, producer );
    }

    /**
     * This will get the creation date of the document.  This will return null if no creation date exists.
     *
     * @return The creation date of the document.
     *
     * @throws EParseError If there is an error creating the date.
     */
    public Calendar getCreationDate() throws EParseError
    {
        return info.getDate( COSName.CREATION_DATE, pdfParser, null );
    }

    /**
     * This will set the creation date of the document.
     *
     * @param date The new creation date for the document.
     */
    public void setCreationDate( Calendar date )
    {
        info.setDate( COSName.CREATION_DATE, date );
    }

    /**
     * This will get the modification date of the document.  This will return null if no modification date exists.
     *
     * @return The modification date of the document.
     *
     * @throws EParseError If there is an error creating the date.
     */
    public Calendar getModificationDate() throws EParseError
    {
        return info.getDate( COSName.MOD_DATE, pdfParser, null );
    }

    /**
     * This will set the modification date of the document.
     *
     * @param date The new modification date for the document.
     */
    public void setModificationDate( Calendar date )
    {
        info.setDate( COSName.MOD_DATE, date );
    }

    /**
     * This will get the trapped value for the document.
     * This will return COSName.UNKNOWN if one is not found.
     *
     * @return The trapped value for the document.
     */
    public COSName getTrapped()
    {
        return info.getName(COSName.TRAPPED, COSName.UNKNOWN);
    }

    /**
     * This will get the keys of all metadata information fields for the document.
     *
     * @return all metadata key strings.
     */
//    public Set<String> getMetadataKeys()
//    {
//        Set<String> keys = new TreeSet<String>();
//        for (COSName key : info.keySet()) {
//            keys.add(key.getName());
//        }
//        return keys;
//    }

    /**
     *  This will get the value of a custom metadata information field for the document.
     *  This will return null if one is not found.
     *
     * @param fieldName Name of custom metadata field from pdf document.
     *
     * @return String Value of metadata field
     *
     */
    public String getCustomMetadataValue(COSName fieldName)
    {
        return info.getStr( fieldName, "" );
    }

    /**
     * Set the custom metadata value.
     *
     * @param fieldName The name of the custom metadata field.
     * @param fieldValue The value to the custom metadata field.
     */
    public void setCustomMetadataValue( COSName fieldName, String fieldValue )
    {
        info.setStr( fieldName, fieldValue );
    }

    /**
     * This will set the trapped of the document.  This will be
     * 'True', 'False', or 'Unknown'.
     *
     * @param value The new trapped value for the document.
     */
    public void setTrapped( String value )
    {
        if( value != null &&
                !value.equals( "True" ) &&
                !value.equals( "False" ) &&
                !value.equals( "Unknown" ) )
        {
            throw new IllegalArgumentException( "Valid values for trapped are 'True', 'False', or 'Unknown'" );
        }

        info.setStr(COSName.TRAPPED, value);
    }
}
