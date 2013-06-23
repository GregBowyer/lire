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
 * ====================
 * (c) 2002-2013 by Mathias Lux (mathias@juggle.at)
 *  http://www.semanticmetadata.net/lire, http://www.lire-project.net
 *
 * Updated: 23.06.13 18:16
 */

package net.semanticmetadata.lire.benchmarking;

import junit.framework.TestCase;
import net.semanticmetadata.lire.*;
import net.semanticmetadata.lire.imageanalysis.*;
import net.semanticmetadata.lire.imageanalysis.spatialpyramid.SPACC;
import net.semanticmetadata.lire.imageanalysis.spatialpyramid.SPCEDD;
import net.semanticmetadata.lire.imageanalysis.spatialpyramid.SPFCTH;
import net.semanticmetadata.lire.imageanalysis.spatialpyramid.SPJCD;
import net.semanticmetadata.lire.impl.ChainedDocumentBuilder;
import net.semanticmetadata.lire.impl.GenericDocumentBuilder;
import net.semanticmetadata.lire.impl.GenericFastImageSearcher;
import net.semanticmetadata.lire.indexing.parallel.ParallelIndexer;
import net.semanticmetadata.lire.utils.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;

import javax.imageio.ImageIO;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: mlux
 * Date: 14.05.13
 * Time: 10:56
 */
public class TestUCID extends TestCase {
    private String indexPath = "ucid-index";
    // if you don't have the images you can get them here: http://homepages.lboro.ac.uk/~cogs/datasets/ucid/ucid.html
    // I converted all images to PNG (lossless) to save time, space & troubles with Java.
    private String testExtensive = "E:\\ucid.v2\\png";
    private final String groundTruth = "E:\\ucid.v2\\ucid.v2.groundtruth.txt";

    private ChainedDocumentBuilder builder;
    private HashMap<String, List<String>> queries;
    ParallelIndexer parallelIndexer;

    protected void setUp() throws Exception {
        super.setUp();
//        indexPath += "-" + System.currentTimeMillis() % (1000 * 60 * 60 * 24 * 7);
        // Setting up DocumentBuilder:
        parallelIndexer = new ParallelIndexer(8, indexPath, testExtensive, true) {
            @Override
            public void addBuilders(ChainedDocumentBuilder builder) {
//                builder.addBuilder(DocumentBuilderFactory.getCEDDDocumentBuilder());
//                builder.addBuilder(DocumentBuilderFactory.getAutoColorCorrelogramDocumentBuilder());
//                builder.addBuilder(DocumentBuilderFactory.getColorLayoutBuilder());
//                builder.addBuilder(DocumentBuilderFactory.getEdgeHistogramBuilder());
//                builder.addBuilder(DocumentBuilderFactory.getFCTHDocumentBuilder());
//                builder.addBuilder(DocumentBuilderFactory.getJCDDocumentBuilder());
//                builder.addBuilder(DocumentBuilderFactory.getJointHistogramDocumentBuilder());
                builder.addBuilder(DocumentBuilderFactory.getOpponentHistogramDocumentBuilder());
//                builder.addBuilder(DocumentBuilderFactory.getPHOGDocumentBuilder());
//                builder.addBuilder(DocumentBuilderFactory.getColorHistogramDocumentBuilder());
//                builder.addBuilder(DocumentBuilderFactory.getScalableColorBuilder());

//                builder.addBuilder(DocumentBuilderFactory.getTamuraDocumentBuilder());
//                builder.addBuilder(DocumentBuilderFactory.getGaborDocumentBuilder());
//                builder.addBuilder(DocumentBuilderFactory.getLuminanceLayoutDocumentBuilder());
//                builder.addBuilder(DocumentBuilderFactory.getJpegCoefficientHistogramDocumentBuilder());
//                builder.addBuilder(new GenericDocumentBuilder(JointOpponentHistogram.class, "jop"));
//                builder.addBuilder(new GenericFastDocumentBuilder(FuzzyOpponentHistogram.class, "opHist"));
//                builder.addBuilder(new SurfDocumentBuilder());
//                builder.addBuilder(new MSERDocumentBuilder());
//                builder.addBuilder(new SiftDocumentBuilder());
//                builder.addBuilder(new GenericDocumentBuilder(SPCEDD.class, "spcedd"));
//                builder.addBuilder(new GenericDocumentBuilder(SPJCD.class, "spjcd"));
//                builder.addBuilder(new GenericDocumentBuilder(SPFCTH.class, "spfcth"));
//                builder.addBuilder(new GenericDocumentBuilder(SPACC.class, "spacc"));
//                builder.addBuilder(new GenericDocumentBuilder(LocalBinaryPatterns.class, "lbp"));
//                builder.addBuilder(new GenericDocumentBuilder(RotationInvariantLocalBinaryPatterns.class, "rlbp"));
//                builder.addBuilder(new GenericDocumentBuilder(SPLBP.class, "splbp"));
            }
        };

        // Getting the queries:
        BufferedReader br = new BufferedReader(new FileReader(groundTruth));
        String line;
        queries = new HashMap<String, List<String>>(260);
        String currentQuery = null;
        LinkedList<String> results = null;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#") || line.length() < 4)
                continue;
            else {
                if (line.endsWith(":")) {
                    if (currentQuery != null) {
                        queries.put(currentQuery, results);
                    }
                    currentQuery = line.replace(':', ' ').trim();
                    results = new LinkedList<String>();
                } else {
                    results.add(line);
                }
            }
        }
        queries.put(currentQuery, results);
    }

    public void testMAP() throws IOException {
        // INDEXING ...
        parallelIndexer.run();
//        SurfFeatureHistogramBuilder sh = new SurfFeatureHistogramBuilder(DirectoryReader.open(FSDirectory.open(new File(indexPath))), 1400, 100);
//        sh.index();

        // SEARCHING
        IndexReader reader = DirectoryReader.open(new RAMDirectory(FSDirectory.open(new File(indexPath)), IOContext.READONCE));

        System.out.println("Feature\tMAP\tp@10\tER");
//        computeMAP(ImageSearcherFactory.createCEDDImageSearcher(1400), "CEDD", reader);
//        computeMAP(ImageSearcherFactory.createFCTHImageSearcher(1400), "FCTH", reader);
//        computeMAP(ImageSearcherFactory.createJCDImageSearcher(1400), "JCD", reader);
//        computeMAP(ImageSearcherFactory.createPHOGImageSearcher(1400), "PHOG", reader);
//        computeMAP(ImageSearcherFactory.createColorLayoutImageSearcher(1400), "Color Layout", reader);
//        computeMAP(ImageSearcherFactory.createEdgeHistogramImageSearcher(1400), "Edge Histogram", reader);
//        computeMAP(ImageSearcherFactory.createScalableColorImageSearcher(1400), "Scalable Color", reader);
//        computeMAP(ImageSearcherFactory.createJointHistogramImageSearcher(1400), "Joint Histogram", reader);
        computeMAP(ImageSearcherFactory.createOpponentHistogramSearcher(1400), "Opponent Histogram", reader);
//        computeMAP(ImageSearcherFactory.createColorHistogramImageSearcher(1400), "RGB Color Histogram", reader);
//        computeMAP(ImageSearcherFactory.createAutoColorCorrelogramImageSearcher(1400), "Color Correlation", reader);

//        computeMAP(new GenericFastImageSearcher(1400, SPCEDD.class, "spcedd"), "SPCEDD", reader);
//        computeMAP(new GenericFastImageSearcher(1400, SPJCD.class, "spjcd"), "SPJCD", reader);
//        computeMAP(new GenericFastImageSearcher(1400, SPFCTH.class, "spfcth"), "SPFCTH", reader);
//        computeMAP(new GenericFastImageSearcher(1400, SPACC.class, "spacc"), "SPACC ", reader);
//        computeMAP(new GenericFastImageSearcher(1400, LocalBinaryPatterns.class, "lbp"), "LBP ", reader);
//        computeMAP(new GenericFastImageSearcher(1400, RotationInvariantLocalBinaryPatterns.class, "rlbp"), "RILBP ", reader);
//        computeMAP(new GenericFastImageSearcher(1400, SPLBP.class, "splbp"), "SPLBP ", reader);
//        computeMAP(ImageSearcherFactory.createTamuraImageSearcher(1400), "Tamura", reader);
//        computeMAP(ImageSearcherFactory.createTamuraImageSearcher(1400), "Tamura", reader);
//        computeMAP(new VisualWordsImageSearcher(1400, DocumentBuilder.FIELD_NAME_SURF_VISUAL_WORDS), "Surf BoVW", reader);
    }

    private void computeMAP(ImageSearcher searcher, String prefix, IndexReader reader) throws IOException {
        double queryCount = 0d;
        double errorRate = 0;
        double map = 0;
        double p10 = 0;
        // Needed for check whether the document is deleted.
        Bits liveDocs = MultiFields.getLiveDocs(reader);

        for (int i = 0; i < reader.maxDoc(); i++) {
            if (reader.hasDeletions() && !liveDocs.get(i)) continue; // if it is deleted, just ignore it.
            String fileName = getIDfromFileName(reader.document(i).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0]);
            if (queries.keySet().contains(fileName)) {
                queryCount += 1d;
                // ok, we've got a query here for a document ...
                Document queryDoc = reader.document(i);
                ImageSearchHits hits = searcher.search(queryDoc, reader);
                int rank = 0;
                double avgPrecision = 0;
                double found = 0;
                double tmpP10 = 0;
                for (int y = 0; y < hits.length(); y++) {
                    String hitFile = getIDfromFileName(hits.doc(y).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0]);
                    if (!hitFile.equals(fileName)) {
                        rank++;
                        if (queries.get(fileName).contains(hitFile)) { // it's a hit.
                            found++;
                            // TODO: Compute error rate, etc. here.
                            avgPrecision += found / rank;
//                            if (rank<=60) System.out.print('X');
                            if (rank <= 10) tmpP10++;
                        } else {
                            if (rank == 1) errorRate += 1d;
//                            if (rank<=60) System.out.print('-');
                        }
                    }
                }
//                System.out.println();
                avgPrecision /= (double) queries.get(fileName).size();
                if (found - queries.get(fileName).size() != 0)
                    System.err.println("huhu");
                assertTrue(found - queries.get(fileName).size() == 0);
                map += avgPrecision;
                p10 += tmpP10 / 3d;
            }
        }
        errorRate = errorRate / queryCount;
        map /= queryCount;
        p10 /= queryCount;
        System.out.print(prefix);
        System.out.format("\t%.5f\t%.5f\t%.5f\n", map, p10, errorRate);
    }

    private String getIDfromFileName(String path) {
        // That's the one for Windows. Change for Linux ...
        return path.substring(path.lastIndexOf('\\') + 1).replace(".png", ".tif");
    }

    public void testIndexingSpeed() throws IOException {
        ArrayList<String> images = FileUtils.getAllImages(new File(testExtensive), false);
//        testFeatureSpeed(images, new AutoColorCorrelogram());
//        testFeatureSpeed(images, new CEDD());
//        testFeatureSpeed(images, new FCTH());
//        testFeatureSpeed(images, new JCD());
        testFeatureSpeed(images, new SPACC());
        testFeatureSpeed(images, new SPCEDD());
        testFeatureSpeed(images, new SPFCTH());
        testFeatureSpeed(images, new SPJCD());
    }

    public void testSearchSpeed() throws IOException {
        ArrayList<String> images = FileUtils.getAllImages(new File(testExtensive), false);
        testSearchSpeed(images, AutoColorCorrelogram.class);
        testSearchSpeed(images, CEDD.class);
        testSearchSpeed(images, FCTH.class);
        testSearchSpeed(images, JCD.class);
        testSearchSpeed(images, SPACC.class);
        testSearchSpeed(images, SPCEDD.class);
        testSearchSpeed(images, SPFCTH.class);
        testSearchSpeed(images, SPJCD.class);
    }

    private void testSearchSpeed(ArrayList<String> images, final Class featureClass) throws IOException {
        parallelIndexer = new ParallelIndexer(8, indexPath, testExtensive, true) {
            @Override
            public void addBuilders(ChainedDocumentBuilder builder) {
                builder.addBuilder(new GenericDocumentBuilder(featureClass, "feature"));
            }
        };
        parallelIndexer.run();
        IndexReader reader = DirectoryReader.open(new RAMDirectory(FSDirectory.open(new File(indexPath)), IOContext.READONCE));
        Bits liveDocs = MultiFields.getLiveDocs(reader);
        double queryCount = 0d;
        ImageSearcher searcher = new GenericFastImageSearcher(100, featureClass, "feature");
        long ms = System.currentTimeMillis();
        for (int i = 0; i < reader.maxDoc(); i++) {
            if (reader.hasDeletions() && !liveDocs.get(i)) continue; // if it is deleted, just ignore it.
            String fileName = getIDfromFileName(reader.document(i).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0]);
            if (queries.keySet().contains(fileName)) {
                queryCount += 1d;
                // ok, we've got a query here for a document ...
                Document queryDoc = reader.document(i);
                ImageSearchHits hits = searcher.search(queryDoc, reader);
            }
        }
        ms = System.currentTimeMillis() - ms;
        System.out.printf("%s \t %3.1f \n", featureClass.getName().substring(featureClass.getName().lastIndexOf('.')+1), (double) ms / queryCount);
    }

    private void testFeatureSpeed(ArrayList<String> images, LireFeature feature) throws IOException {
        long ms = System.currentTimeMillis();
        for (Iterator<String> iterator = images.iterator(); iterator.hasNext(); ) {
            String s = iterator.next();
            feature.extract(ImageIO.read(new File(s)));
        }
        ms = System.currentTimeMillis() - ms;
        System.out.printf("%s \t %3.1f \n", feature.getClass().getName().substring(feature.getClass().getName().lastIndexOf('.')+1), (double) ms / (double) images.size());
    }


}
