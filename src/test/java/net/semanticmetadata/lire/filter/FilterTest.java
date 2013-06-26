/*
 * This file is part of the LIRE project: http://www.semanticmetadata.net/lire
 * LIRE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * LIRE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LIRE; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * We kindly ask you to refer the any or one of the following publications in
 * any publication mentioning or employing Lire:
 *
 * Lux Mathias, Savvas A. Chatzichristofis. Lire: Lucene Image Retrieval –
 * An Extensible Java CBIR Library. In proceedings of the 16th ACM International
 * Conference on Multimedia, pp. 1085-1088, Vancouver, Canada, 2008
 * URL: http://doi.acm.org/10.1145/1459359.1459577
 *
 * Lux Mathias. Content Based Image Retrieval with LIRE. In proceedings of the
 * 19th ACM International Conference on Multimedia, pp. 735-738, Scottsdale,
 * Arizona, USA, 2011
 * URL: http://dl.acm.org/citation.cfm?id=2072432
 *
 * Mathias Lux, Oge Marques. Visual Information Retrieval using Java and LIRE
 * Morgan & Claypool, 2013
 * URL: http://www.morganclaypool.com/doi/abs/10.2200/S00468ED1V01Y201301ICR025
 *
 * Copyright statement:
 * --------------------
 * (c) 2002-2013 by Mathias Lux (mathias@juggle.at)
 *     http://www.semanticmetadata.net/lire, http://www.lire-project.net
 */

package net.semanticmetadata.lire.filter;

import junit.framework.TestCase;
import net.semanticmetadata.lire.*;
import net.semanticmetadata.lire.imageanalysis.CEDD;
import net.semanticmetadata.lire.imageanalysis.ColorLayout;
import net.semanticmetadata.lire.impl.ChainedDocumentBuilder;
import net.semanticmetadata.lire.utils.FileUtils;
import net.semanticmetadata.lire.utils.LuceneUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Created 03.08.11, 11:09 <br/>
 *
 * @author Mathias Lux, mathias@juggle.at
 * @author sangupta, sandy.pec@gmail.com (fixed deprecation)
 */
public class FilterTest extends TestCase {

    private String indexPath = "test-filters";

    private DocumentBuilder getDocumentBuilder() {
        ChainedDocumentBuilder builder = new ChainedDocumentBuilder();
        builder.addBuilder(DocumentBuilderFactory.getCEDDDocumentBuilder());
        builder.addBuilder(DocumentBuilderFactory.getAutoColorCorrelogramDocumentBuilder());
        builder.addBuilder(DocumentBuilderFactory.getColorLayoutBuilder());
        return builder;
    }

    public void testRerankFilter() throws IOException {
        // index images
        // indexFiles();
        // search
        System.out.println("---< searching >-------------------------");
        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
        Document document = reader.document(0);
        ImageSearcher searcher = ImageSearcherFactory.createCEDDImageSearcher(100);
        ImageSearchHits hits = searcher.search(document, reader);
        // rerank
        System.out.println("---< filtering >-------------------------");
        RerankFilter filter = new RerankFilter(ColorLayout.class, DocumentBuilder.FIELD_NAME_COLORLAYOUT);
        hits = filter.filter(hits, document);

        // output
        FileUtils.saveImageResultsToHtml("filtertest", hits, document.getField(DocumentBuilder.FIELD_NAME_IDENTIFIER).stringValue());
    }

    public void testLsaFilter() throws IOException {
        // index images
//        indexFiles();
        // search
        System.out.println("---< searching >-------------------------");
        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
        Document document = reader.document(0);
        ImageSearcher searcher = ImageSearcherFactory.createCEDDImageSearcher(100);
        ImageSearchHits hits = searcher.search(document, reader);
        // rerank
        System.out.println("---< filtering >-------------------------");
        LsaFilter filter = new LsaFilter(CEDD.class, DocumentBuilder.FIELD_NAME_CEDD);
        hits = filter.filter(hits, document);

        // output
        FileUtils.saveImageResultsToHtml("filtertest", hits, document.getField(DocumentBuilder.FIELD_NAME_IDENTIFIER).stringValue());
    }

    @SuppressWarnings("unused")
	private Document indexFiles() throws IOException {
        System.out.println("---< indexing >-------------------------");
        int count = 0;
        DocumentBuilder builder = getDocumentBuilder();
        ArrayList<String> allImages = FileUtils.getAllImages(new File("wang-1000"), true);
        IndexWriter iw = LuceneUtils.createIndexWriter(indexPath, true);
        Document document = null;
        for (Iterator<String> iterator = allImages.iterator(); iterator.hasNext(); ) {
            String filename = iterator.next();
            BufferedImage image = ImageIO.read(new FileInputStream(filename));
            document = builder.createDocument(image, filename);
            iw.addDocument(document);
            count++;
            if (count % 50 == 0)
                System.out.println("finished " + (count * 100) / allImages.size() + "% of the images.");
        }
        iw.close();
        return document;
    }
}
