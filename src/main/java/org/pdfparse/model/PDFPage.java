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

import org.pdfparse.cds.PDFRectangle;
import org.pdfparse.cos.COSDictionary;
import org.pdfparse.cos.COSName;

/**
 * This represents a single page in a PDF document.
 *
 * <P>
 * A Page object is a dictionary whose keys describe a single page containing text,
 * graphics, and images. A Page object is a leaf of the Pages tree.<BR>
 * This object is described in the 'Portable Document Format Reference Manual version 1.3'
 * section 6.4 (page 73-81)
 *
 * @see        PDFPageNode
 */

public class PDFPage {
    COSDictionary dPage;

    /**
     * Creates a new instance of PDPage with a size of 8.5x11.
     */
    public PDFPage() {
        dPage = new COSDictionary();
        dPage.setName(COSName.TYPE, COSName.PAGE);
        //setMediaBox(PAGE_SIZE_LETTER);
    }

    /**
     * Creates a new instance of PDPage.
     *
     * @param size The MediaBox or the page.
     */
    public PDFPage(PDFRectangle size)
    {
        dPage = new COSDictionary();
        dPage.setName(COSName.TYPE, COSName.PAGE);
        //setMediaBox(size);
    }

    /**
     * Creates a new instance of PDPage.
     *
     * @param pageDic The existing page dictionary.
     */
    public PDFPage(COSDictionary pageDic)
    {
        dPage = pageDic;
    }


    /**
     * This will get the underlying dictionary that this class acts on.
     *
     * @return The underlying dictionary for this class.
     */
    public COSDictionary getCOSDictionary() {
        return dPage;
    }

    /**
     * A rectangle, expressed in default user space units, defining the boundaries of the physical medium on which the
     * page is intended to be displayed or printed
     *
     * This will get the MediaBox at this page and not look up the hierarchy. This attribute is inheritable, and
     * findMediaBox() should probably used. This will return null if no MediaBox are available at this level.
     *
     * @return The MediaBox at this level in the hierarchy.
     */
    public PDFRectangle getMediaBox() {
        return dPage.getRectangle(COSName.MEDIABOX);
    }

    /**
     * Set the mediaBox for this page.
     *
     * @param value The new mediaBox for this page.
     */
    public void setMediaBox(PDFRectangle value) {
        if (value == null) {
            dPage.remove(COSName.MEDIABOX);
        } else {
            dPage.setRectangle(COSName.MEDIABOX, value);
        }
    }

    /**
     * A rectangle, expressed in default user space units, defining the visible region of default user space. When the
     * page is displayed or printed, its contents are to be clipped (cropped) to this rectangle and then imposed on the
     * output medium in some implementation-defined manner
     *
     * This will get the CropBox at this page and not look up the hierarchy. This attribute is inheritable, and
     * findCropBox() should probably used. This will return null if no CropBox is available at this level.
     *
     * @return The CropBox at this level in the hierarchy.
     */
    public PDFRectangle getCropBox() {
        return dPage.getRectangle(COSName.CROPBOX);
    }

    /**
     * Set the CropBox for this page.
     *
     * @param value The new CropBox for this page.
     */
    public void setCropBox(PDFRectangle value) {
        if (value == null) {
            dPage.remove(COSName.CROPBOX);
        } else {
            dPage.setRectangle(COSName.CROPBOX, value);
        }
    }




}