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

import org.pdfparse.cos.COSDictionary;
import org.pdfparse.cos.COSName;
import org.pdfparse.cos.COSReference;

/**
 * This represents a page node in a pdf document.
 * <P>
 * The Pages of a document are accessible through a tree of nodes known as the Pages tree.
 * This tree defines the ordering of the pages in the document.<BR>
 * This object is described in the 'Portable Document Format Reference Manual version 1.3'
 * section 6.3 (page 71-73)
 *
 * @see        org.pdfparse.model.PDFPage
 */

public class PDFPageNode {

    private COSDictionary dPageNode;

    /**
     * Creates a new instance of PDPage.
     */
    public PDFPageNode() {
        dPageNode = new COSDictionary();
        dPageNode.setName( COSName.TYPE, COSName.PAGES);
        //page.setName(COSName.KIDS, new COSArray());
        dPageNode.setInt(COSName.COUNT, 0);
    }

    /**
     * Creates a new instance of PDPage.
     *
     * @param pages The dictionary pages.
     */
    public PDFPageNode( COSDictionary pages ) {
        dPageNode = pages;
    }

    /**
     * Get the count of descendent page objects.
     *
     * @return The total number of descendent page objects.
     */
    public int getCount() {
        if(dPageNode == null)
            return 0;

        return dPageNode.getInt(COSName.COUNT, 0);
    }

    /**
     * This will get the underlying dictionary that this class acts on.
     *
     * @return The underlying dictionary for this class.
     */
    public COSDictionary getCOSDictionary() {
        return dPageNode;
    }

    /**
     * The parent page node.
     *
     * @return The parent to this page.
     */
    public COSReference getParent()  {
        return dPageNode.getReference(COSName.PARENT);
    }

    /**
     * Set the parent of this page.
     *
     * @param parent The parent to this page node.
     */
    public void setParent( COSReference parent ) {
        dPageNode.setReference( COSName.PARENT, parent );
    }
}
