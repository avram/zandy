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
import org.pdfparse.parser.ParsingContext;
import java.util.ArrayList;

public class PDFDocCatalog {
    private COSDictionary dRoot;
    private COSDictionary dPages;
    private ParsingContext context;
    private ArrayList<PDFPage> pages;

    public PDFDocCatalog(ParsingContext context, COSDictionary dic) {
        dRoot = dic;
        this.context = context;

        context.softAssertSyntaxComliance(COSName.CATALOG.equals(dic.getName(COSName.TYPE, null)), "Document catalog should be /Catalog type");
    }


    public COSDictionary getCOSDictionary() {
        return dRoot;
    }

    /**
     * Return the total page count of the PDF document.
     *
     * @return The total number of pages in the PDF document.
     */
    public int getPagesCount() {
        if (pages == null) {
            COSReference refRootPages = dRoot.getReference(COSName.PAGES);
            dPages = context.objectCache.getDictionary(refRootPages);
            return dPages.getUInt(COSName.COUNT, context.objectCache, -1);
        };

        return pages.size();
    }
    private void loadPage(COSReference cosReference) {
        COSDictionary dict = context.objectCache.getDictionary(cosReference);
        if (dict.getName(COSName.TYPE, COSName.EMPTY).equals(COSName.PAGES)) {
            loadPages(dict); // This is a page node
            return;
        }

        this.pages.add( new PDFPage(dict) );
    }

    private void loadPages(COSDictionary pages) {
        context.softAssertStructure(
                pages.getName(COSName.TYPE, COSName.EMPTY).equals(COSName.PAGES),
                "This dictionary should be /Type = /Pages");


        COSArray kids = pages.getArray(COSName.KIDS, context.objectCache, null);
        if (!context.softAssertStructure(kids != null, "Required entry '/Kids' not found")) {
            return; // will be zero pages
        }

        for (int i=0; i<kids.size(); i++) {
            COSObject ref = kids.get(i);

            if (context.softAssertStructure(ref instanceof COSReference, "/Kids element should be a reference"))
                loadPage((COSReference)ref);
        }
    }

    public ArrayList<PDFPage> getPages() {
        if (pages != null)
            return pages;

        getPagesCount();
        loadPages(dPages);

        if (pages == null) {

        }

        return pages;
    }

    /**
     * Returns the PDF specification version this document conforms to.
     *
     * @return The PDF version.
     */
    public String getVersion() {
       return dRoot.getNameAsStr(COSName.VERSION, context.objectCache, "");
    }

    /** Sets the PDF specification version this document conforms to.
    *
            * @param version the PDF version (ex. "1.4")
    */
    public void setVersion(String version) {
        dRoot.setName(COSName.VERSION, new COSName(version));
    }

    /**
     * Get the metadata that is part of the document catalog.  This will
     * return null if there is no meta data for this object.
     *
     * @return The metadata for this object.
     */
    public byte[] getXMLMetadata() {
        COSReference refMetadata = dRoot.getReference(COSName.METADATA);
        if (refMetadata == null)
            return null;
        COSStream dMetadata = context.objectCache.getStream(refMetadata);
        if (dMetadata == null)
            return null;
        return dMetadata.getData();
    }

    /**
     * The language for the document.
     *
     * @return The language for the document.
     */
    public String getLanguage() {
        return dRoot.getStr(COSName.LANG, context.objectCache, "");
    }

    /**
     * Set the Language for the document.
     *
     * @param language The new document language.
     */
    public void setLanguage( String language ) {
        dRoot.setStr( COSName.LANG, language );
    }


    /**
     * Get the page layout, see the PL_XXX constants.
     * @return A COSName representing the page layout.
     */
    public COSName getPageLayout() {
        return dRoot.getName( COSName.PAGELAYOUT, COSName.PL_SINGLE_PAGE );
    }

    /**
     * Set the page layout, see the PL_XXX constants for valid values.
     * @param layout The new page layout.
     */
    public void setPageLayout( COSName layout ) {
        dRoot.setName( COSName.PAGELAYOUT, layout );
    }

    /**
     * Get the page display mode, see the PM_XXX constants.
     * @return A COSName representing the page mode.
     */
    public COSName getPageMode() {
        return dRoot.getName( COSName.PAGEMODE, COSName.PM_NONE );
    }

    /**
     * Set the page mode.  See the PM_XXX constants for valid values.
     * @param mode The new page mode.
     */
    public void setPageMode( COSName mode ) {
        dRoot.setName( COSName.PAGEMODE, mode );
    }





}
