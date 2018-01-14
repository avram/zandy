package com.gimranov.zandy.app.api;

import com.gimranov.zandy.app.model.Collection;
import com.gimranov.zandy.app.model.Item;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.BufferedSource;
import okio.Okio;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static junit.framework.Assert.*;

/*
 * This file is part of Zandy.
 *
 * Zandy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Zandy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Zandy.  If not, see <http://www.gnu.org/licenses/>.
 */
@RunWith(JUnit4.class)
public class ZoteroServiceTest {
    private ZoteroService mZoteroService;
    private MockWebServer mMockWebServer;
    private Item mItemforSingle;
    private Item mItemforArray;
    private String mJson;
    private String mJsonArray;

    @Before
    public void setUp() throws Exception {
        mMockWebServer = new MockWebServer();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Item.class, new ZoteroDeserializer())
                .registerTypeAdapter(Collection.class, new ZoteroDeserializer())
                .create();

        mZoteroService = new Retrofit.Builder()
                .baseUrl(mMockWebServer.url("/"))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ZoteroService.class);

        mItemforSingle = new Gson().fromJson("{\"key\":\"X42A7DEE\",\"version\":1,\"itemType\":\"book\",\"title\":\"Electron Microscopy and Analysis 1993: Proceedings of the Institute of Physics Electron Microscopy and Analysis Group Conference, University of Liverpool, 14-17 September1993\",\"creators\":[{\"creatorType\":\"author\",\"name\":\"Institute of Physics (Great Britain)\"},{\"creatorType\":\"contributor\",\"firstName\":\"A. J\",\"lastName\":\"Craven\"},{\"creatorType\":\"contributor\",\"name\":\"Institute of Physics (Great Britain)\"},{\"creatorType\":\"contributor\",\"name\":\"Institute of Physics (Great Britain)\"},{\"creatorType\":\"contributor\",\"name\":\"Institute of Materials (Great Britain)\"},{\"creatorType\":\"contributor\",\"name\":\"Royal Microscopical Society (Great Britain)\"},{\"creatorType\":\"contributor\",\"name\":\"University of Liverpool\"}],\"abstractNote\":\"\",\"series\":\"Institute of Physics conference series\",\"seriesNumber\":\"no. 138\",\"volume\":\"\",\"numberOfVolumes\":\"\",\"edition\":\"\",\"place\":\"Bristol, UK\",\"publisher\":\"Institute of Physics Pub\",\"date\":\"1993\",\"numPages\":\"546\",\"language\":\"\",\"ISBN\":\"0750303212\",\"shortTitle\":\"Electron Microscopy and Analysis 1993\",\"url\":\"\",\"accessDate\":\"\",\"archive\":\"\",\"archiveLocation\":\"\",\"libraryCatalog\":\"cat.cisti-icist.nrc-cnrc.gc.ca Library Catalog\",\"callNumber\":\"QC1 I584 v. 138\",\"rights\":\"\",\"extra\":\"\",\"tags\":[{\"tag\":\"Analysis\",\"type\":1},{\"tag\":\"Congresses\",\"type\":1},{\"tag\":\"Electron microscopy\",\"type\":1},{\"tag\":\"Materials\",\"type\":1},{\"tag\":\"Microscopy\",\"type\":1}],\"collections\":[\"BX9965IJ\",\"9KH9TNSJ\"],\"relations\":{\"owl:sameAs\":\"http://zotero.org/groups/36222/items/E6IGUT5Z\"},\"dateAdded\":\"2011-01-13T03:37:29Z\",\"dateModified\":\"2011-01-13T03:37:29Z\"}", Item.class);
        mItemforArray = new Gson().fromJson("{\"key\":\"FHCSUTJ7\",\"version\":1,\"parentItem\":\"TWZWDCXM\",\"itemType\":\"attachment\",\"linkMode\":\"imported_url\",\"title\":\"JavaScript: The Good Parts: Proquest Tech & Business Books\",\"accessDate\":\"2011-04-10T19:42:57Z\",\"url\":\"http://proquestcombo.safaribooksonline.com.proxy2.library.illinois.edu/book/programming/javascript/9780596517748\",\"note\":\"\",\"contentType\":\"text/html\",\"charset\":\"utf-8\",\"filename\":\"9780596517748.html\",\"md5\":null,\"mtime\":1302464576000,\"tags\":[],\"relations\":{\"owl:sameAs\":\"http://zotero.org/groups/36222/items/PIQTI4QF\"},\"dateAdded\":\"2011-04-10T19:42:57Z\",\"dateModified\":\"2011-04-10T19:45:55Z\"}", Item.class);
    }

    @After
    public void tearDown() throws Exception {
        mMockWebServer.shutdown();
    }

    @Test
    public void getItemForUser() throws Exception {
        enqueueResponse("item.json");
        Response<Item> response = mZoteroService.getItemForUser("475425", "X42A7DEE").execute();
        Item item = response.body();
        RecordedRequest request = mMockWebServer.takeRequest();
        assertEquals("/475425/items/X42A7DEE", request.getPath() );
        assertNotNull(item);
        assertEquals("Bristol, UK", item.place);
    }


    private void enqueueResponse(String fileName) throws IOException {
        enqueueResponse(fileName, Collections.<String, String>emptyMap());
    }

    private void enqueueResponse(String fileName, Map<String, String> headers) throws IOException {
        InputStream inputStream = getClass().getClassLoader()
                                            .getResourceAsStream("api-response/" + fileName);
        BufferedSource source = Okio.buffer(Okio.source(inputStream));
        MockResponse mockResponse = new MockResponse();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            mockResponse.addHeader(header.getKey(), header.getValue());
        }
        mMockWebServer.enqueue(mockResponse
                .setBody(source.readString(StandardCharsets.UTF_8)));
    }

}