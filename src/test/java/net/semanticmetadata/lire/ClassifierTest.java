package net.semanticmetadata.lire;/*
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

import net.semanticmetadata.lire.LireTestCase;
import net.semanticmetadata.lire.imageanalysis.*;
import net.semanticmetadata.lire.impl.BitSamplingImageSearcher;
import net.semanticmetadata.lire.impl.GenericFastImageSearcher;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.MMapDirectory;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: mlux, mriegler
 * Date: 01.08.2013
 * Time: 13:53
 */
public class ClassifierTest extends LireTestCase {


    public void testClassify() throws IOException {
        boolean weightByRank = true;
        String[] classes = {"2012", "beach", "food", "london", "music", "nature", "people", "sky", "travel", "wedding"};
        int k = 50;
        //String indexLocation = "D:\\Datasets\\FashionTestItemDataSet\\idx\\index";
        String photosLocation = "D:\\Datasets\\FashionTestItemDataSet\\";
        String fieldName = DocumentBuilder.FIELD_NAME_COLORLAYOUT;
        LireFeature feature = new ColorLayout();
        String indexPath = "E:\\acmgc-cl-idx";
        System.out.println("Tests for feature " + fieldName + " with k=" + k + " - weighting by rank sum: " + weightByRank);
        System.out.println("========================================");
        HashMap<String, Integer> tag2count = new HashMap<String, Integer>(k);
        HashMap<String, Double> tag2weight = new HashMap<String, Double>(k);
        int c = 9;   // used for just one class ...
//        for (int c = 0; c < 10; c++) {
        String classIdentifier = classes[c];
        String listFiles = "D:\\DataSets\\Yahoo-GC\\test\\" + classIdentifier + ".txt";

        // INIT
        int[] confusion = new int[10];
        Arrays.fill(confusion, 0);
        HashMap<String, Integer> class2id = new HashMap<String, Integer>(10);
        for (int i = 0; i < classes.length; i++)
            class2id.put(classes[i], i);

        BufferedReader br = new BufferedReader(new FileReader(listFiles));
        String line;
        IndexReader ir = DirectoryReader.open(MMapDirectory.open(new File(indexPath)));
        // in-memory linear search
//            ImageSearcher bis = new GenericFastImageSearcher(k, feature.getClass(), fieldName, true, ir);
        // hashing based searcher
        BitSamplingImageSearcher bis = new BitSamplingImageSearcher(k, fieldName, fieldName + "_hash", feature, 1000);
        ImageSearchHits hits;
        int count = 0, countCorrect = 0;
        long ms = System.currentTimeMillis();
        while ((line = br.readLine()) != null) {
            try {
                tag2count.clear();
                tag2weight.clear();
                hits = bis.search(ImageIO.read(new File(line)), ir);
                // set tag weights and counts.
                for (int l = 0; l < k; l++) {
                    String tag = getTag(hits.doc(l), photosLocation);
                    if (tag2count.get(tag) == null) tag2count.put(tag, 1);
                    else tag2count.put(tag, tag2count.get(tag) + 1);
                    if (weightByRank) {
                        if (tag2weight.get(tag) == null) tag2weight.put(tag, (double) l);
                        else tag2weight.put(tag, (double) l + tag2weight.get(tag));
                    } else {
                        if (tag2weight.get(tag) == null) tag2weight.put(tag, Double.valueOf(hits.score(l)));
                        else tag2weight.put(tag, (double) l + hits.score(l));
                    }
                }
                // find class:
                int maxCount = 0, maxima = 0;
                String classifiedAs = null;
                for (Iterator<String> tagIterator = tag2count.keySet().iterator(); tagIterator.hasNext(); ) {
                    String tag = tagIterator.next();
                    if (tag2count.get(tag) > maxCount) {
                        maxCount = tag2count.get(tag);
                        maxima = 1;
                        classifiedAs = tag;
                    } else if (tag2count.get(tag) == maxCount) {
                        maxima++;
                    }
                }
                // if there are two or more classes with the same number of results, then we take a look at the weights.
                // else the class is alread given in classifiedAs.
                if (maxima > 1) {
                    double minWeight = Double.MAX_VALUE;
                    for (Iterator<String> tagIterator = tag2count.keySet().iterator(); tagIterator.hasNext(); ) {
                        String tag = tagIterator.next();
                        if (tag2weight.get(tag) < minWeight) {
                            minWeight = tag2weight.get(tag);
                            classifiedAs = tag;
                        }
                    }
                }
//                    if (tag2.equals(tag3)) tag1 = tag2;
                count++;
                if (classifiedAs.equals(classIdentifier)) countCorrect++;
                // confusion:
                confusion[class2id.get(classifiedAs)]++;
//                    System.out.printf("%10s (%4.3f, %10d, %4d)\n", classifiedAs, ((double) countCorrect / (double) count), count, (System.currentTimeMillis() - ms) / count);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
//            System.out.println("Results for class " + classIdentifier);
        System.out.printf("Class\tAvg. Precision\tCount Test Images\tms per test\n");
        System.out.printf("%s\t%4.5f\t%10d\t%4d\n", classIdentifier, ((double) countCorrect / (double) count), count, (System.currentTimeMillis() - ms) / count);
        System.out.printf("Confusion\t");
//            for (int i = 0; i < classes.length; i++) {
//                System.out.printf("%s\t", classes[i]);
//            }
//            System.out.println();
        for (int i = 0; i < classes.length; i++) {
            System.out.printf("%d\t", confusion[i]);
        }
        System.out.println();
//        }
    }

    //Classifie Fashion(or every other frameing category) in yes or no.
    public void testClassifyFashion() throws IOException {

        PrintWriter print_line = new PrintWriter(new BufferedWriter(new FileWriter("D:\\resultsSingleFeatureItem1.txt")));

        String[] fieldsArray = {"CEDD", "EdgeHistogram", "FCTH", "ColorLayout", "PHOG", "JCD", "Gabor", "JpegCoeffs", "Tamura", "Luminance_Layout", "Opponent_Histogram", "ScalableColor"};
        String[] classArray = {"CEDD", "EdgeHistogram", "FCTH", "ColorLayout", "PHOG", "JCD", "Gabor", "JpegCoefficientHistogram", "Tamura", "LuminanceLayout", "OpponentHistogram", "ScalableColor"};

        // System.out.printf("Feature1;Feature2;Feature3;K=;Weight Rank=;Class;Precision;Recall;True Negative Rate;Accuracy;F-Measure;Count Test Images;Count Corret;ms per test;");

        print_line.print("Feature1;Feature2;Feature3;K=;Weight Rank=;Class;Precision;Recall;True Negative Rate;Accuracy; False Positive Rate;F-Measure;Count Test Images;Count Correct;ms per test;TP;FP;TN;FN");
        print_line.println();
        print_line.flush();

        for (int y = 0; y < fieldsArray.length; y++) {

            String fieldName1String = "FIELD_NAME_" + fieldsArray[y].toUpperCase();
            boolean weightByRank = true;
            boolean createHTML = false;
            String[] classes = {"yes", "no"};
            int k = 1;
            String indexLocation = "D:\\Datasets\\FashionTestItemDataSet\\idx\\index";
            String photosLocation = "D:\\Datasets\\FashionTestItemDataSet\\";
          //  String listFiles = "D:\\Datasets\\FashionTestItemDataSet\\itemtest.txt";
            String listFiles = "D:\\bug.txt";

            // CONFIG
            String fieldName = null;
            try {
                fieldName = (String) DocumentBuilder.class.getField(fieldName1String).get(null);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            LireFeature feature = null;
            try {
                feature = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + classArray[y]).newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (ClassNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            String indexPath = "D:\\Datasets\\FashionTestItemDataSet\\idx\\index" + classArray[y];


            //  for (int ik = 0;ik<k;ik++)       {

            //   System.out.println("Tests for feature " + fieldName + " with k=" + k + " - weighting by rank sum: " + weightByRank);
            //   System.out.println("========================================");
            HashMap<String, Integer> tag2count = new HashMap<String, Integer>(k);
            HashMap<String, Double> tag2weight = new HashMap<String, Double>(k);
            int c = 0;   // used for just one class ...
//        for (int c = 0; c < 10; c++) {
            String classIdentifier = classes[c];

            //"D:\\Datasets\\FashionTest\\fashion10000Test\\" + classIdentifier + ".txt";

            // INIT
            ArrayList<String> classesHTML = new ArrayList<String>();
            ArrayList<String> filesHTML = new ArrayList<String>();

            int[] confusion = new int[2];
            Arrays.fill(confusion, 0);
            HashMap<String, Integer> class2id = new HashMap<String, Integer>(2);
            for (int i = 0; i < classes.length; i++)
                class2id.put(classes[i], i);

            BufferedReader br = new BufferedReader(new FileReader(listFiles));
            String line;
            IndexReader ir = DirectoryReader.open(MMapDirectory.open(new File(indexPath)));
            // in-memory linear search
            ImageSearcher bis = new GenericFastImageSearcher(k, feature.getClass(), fieldName, true, ir);
            // hashing based searcher
            //BitSamplingImageSearcher bis = new BitSamplingImageSearcher(k, fieldName, fieldName + "_hash", feature, 3000);
            ImageSearchHits hits;
            int count = 0, countCorrect = 0;
            double countTp = 0, countFp = 0, countTn = 0, countFn = 0;      //F1 Metric
            long ms = System.currentTimeMillis();
            while ((line = br.readLine()) != null) {
                try {
                    tag2count.clear();
                    tag2weight.clear();
                    hits = bis.search(ImageIO.read(new File(line)), ir);
                    // set tag weights and counts.
                    for (int l = 0; l < k; l++) {
                        String tag = getTag(hits.doc(l), photosLocation);
                        if (tag2count.get(tag) == null) tag2count.put(tag, 1);
                        else tag2count.put(tag, tag2count.get(tag) + 1);
                        if (weightByRank) {
                            if (tag2weight.get(tag) == null) tag2weight.put(tag, (double) l);
                            else tag2weight.put(tag, (double) l + tag2weight.get(tag));
                        } else {
                            if (tag2weight.get(tag) == null) tag2weight.put(tag, Double.valueOf(hits.score(l)));
                            else tag2weight.put(tag, (double) l + hits.score(l));
                        }
                    }
                    // find class:
                    int maxCount = 0, maxima = 0;
                    String classifiedAs = null;
                    for (Iterator<String> tagIterator = tag2count.keySet().iterator(); tagIterator.hasNext(); ) {
                        String tag = tagIterator.next();
                        if (tag2count.get(tag) > maxCount) {
                            maxCount = tag2count.get(tag);
                            maxima = 1;
                            classifiedAs = tag;
                        } else if (tag2count.get(tag) == maxCount) {
                            maxima++;
                        }
                    }
                    // if there are two or more classes with the same number of results, then we take a look at the weights.
                    // else the class is alread given in classifiedAs.
                    if (maxima > 1) {
                        double minWeight = Double.MAX_VALUE;
                        for (Iterator<String> tagIterator = tag2count.keySet().iterator(); tagIterator.hasNext(); ) {
                            String tag = tagIterator.next();
                            if (tag2weight.get(tag) < minWeight) {
                                minWeight = tag2weight.get(tag);
                                classifiedAs = tag;
                            }
                        }
                    }
//                    if (tag2.equals(tag3)) tag1 = tag2;
                    count++;
                    //SHOW THE CLASSIFICATION
                    //     System.out.println(classifiedAs+";"+line);
                    classesHTML.add(classifiedAs);
                    filesHTML.add(line);
                    //F1 Metric
                    if (classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("yes")) {
                        countCorrect++;
                        countTp++;
                    } else if (!classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("yes"))
                        countFp++;

                    if (classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("no")) {
                        countCorrect++;
                        countTn++;
                    } else if (!classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("no"))
                        countFn++;
                    //if (classifiedAs.equals(classIdentifier)) countCorrect++;
                    //if (classifiedAs.equals(classIdentifier)) countCorrect++;
                    // confusion:
                    confusion[class2id.get(classifiedAs)]++;
//                    System.out.printf("%10s (%4.3f, %10d, %4d)\n", classifiedAs, ((double) countCorrect / (double) count), count, (System.currentTimeMillis() - ms) / count);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }

            double precisicon = getPrecision(countTp, countFp);
            double recall = getRecall(countTp, countFn);
            double trueNegativeRate = getTrueNegativeRate(countTn, countFp);
            double accuracy = getAccuracy(countTp, countFp, countTn, countFn);
            double fMeasure = getFmeasure(precisicon, recall);
            double falsePositiveRate = getFalsePositiveRate(countFp, countTn);
//            System.out.println("Results for class " + classIdentifier);

            // System.out.printf("%s;%s;%s;%s;%s;%s;%4.5f;%4.5f;%4.5f;%4.5f;%4.5f;%10d;%10d;%4d\n",classArray[y],classArray[y],classArray[y],k,weightByRank, classIdentifier, precisicon, recall, trueNegativeRate,accuracy, fMeasure,  count, countCorrect, (System.currentTimeMillis() - ms) / count);
            System.out.println(y + 1 + " of " + classArray.length + " finished. " + (System.currentTimeMillis() - ms) / 1000 + " seconds per round. " + "Feature: " + classArray[y] + " Current y " + y);

            print_line.printf("%s;%s;%s;%s;%s;%s;%4.5f;%4.5f;%4.5f;%4.5f;%4.5f;%4.5f;%10d;%10d;%4d;%4.5f;%4.5f;%4.5f;%4.5f\n", classArray[y], classArray[y], classArray[y], k, weightByRank, classIdentifier, precisicon, recall, trueNegativeRate, accuracy, falsePositiveRate, fMeasure, count, countCorrect, (System.currentTimeMillis() - ms) / count, countTp, countFp, countTn, countFn);
            print_line.flush();

//        System.out.printf("Confusion\t");
//            for (int i = 0; i < classes.length; i++) {
//                System.out.printf("%s\t", classes[i]);
//            }
//            System.out.println();
//        for (int i = 0; i < classes.length; i++) {
            //           System.out.printf("%d\t", confusion[i]);
//        }
//        System.out.println();

            if (createHTML == true) {
                //Create HTML

                String fileName = "classifieresults-" + System.currentTimeMillis() / 1000 + ".html";
                BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
                bw.write("<html>\n" +
                        "<head><title>Classification Results</title></head>\n" +
                        "<body bgcolor=\"#FFFFFF\">\n");
                bw.write("<table>");

                // int elems = Math.min(filesHTML.size(),50);
                int elems = filesHTML.size();

                for (int i = 0; i < elems; i++) {
                    if (i % 3 == 0) bw.write("<tr>");

                    String s = filesHTML.get(i);
                    String colorF = "rgb(0, 255, 0)";

                    if (classesHTML.get(i).equals("no"))
                        colorF = "rgb(255, 0, 0)";
                    //  String s = reader.document(topDocs.scoreDocs[i].doc).get("descriptorImageIdentifier");
                    //  System.out.println(reader.document(topDocs.scoreDocs[i].doc).get("featLumLay"));
                    //  s = new File(s).getAbsolutePath();
                    // System.out.println(s);
                    bw.write("<td><a href=\"" + s + "\"><img style=\"max-width:220px;border:medium solid " + colorF + ";\"src=\"" + s + "\" border=\"" + 5 + "\" style=\"border: 3px\n" +
                            "black solid;\"></a></td>\n");
                    if (i % 3 == 2) bw.write("</tr>");
                }
                if (elems % 3 != 0) {
                    if (elems % 3 == 2) {
                        bw.write("<td>-</td with exit code 0\nd>\n");
                        bw.write("<td>-</td>\n");
                    } else if (elems % 3 == 2) {
                        bw.write("<td>-</td>\n");
                    }
                    bw.write("</tr>");
                }

                bw.write("</table></body>\n" +
                        "</html>");
                bw.close();
            }
            //   } // kfor
//        }
        }

        print_line.close();


    }

    public void testClassifyFashionCombinedFeatures() throws IOException {

        PrintWriter print_line = new PrintWriter(new BufferedWriter(new FileWriter("D:\\resultsitemDoubleFeatureK1.txt")));
        String[] fieldsArray = {"CEDD", "EdgeHistogram", "FCTH", "ColorLayout", "PHOG", "JCD", "Gabor", "JpegCoeffs", "Tamura", "Luminance_Layout", "Opponent_Histogram", "ScalableColor"};
        String[] classArray = {"CEDD", "EdgeHistogram", "FCTH", "ColorLayout", "PHOG", "JCD", "Gabor", "JpegCoefficientHistogram", "Tamura", "LuminanceLayout", "OpponentHistogram", "ScalableColor"};

        print_line.print("Feature1;Feature2;Feature3;K=;Weight Rank=;Class;Precision;Recall;True Negative Rate;Accuracy;False Positive Rate;F-Measure;Count Test Images;Count Correct;ms per test;TP;FP;TN;FN");
        print_line.println();
        print_line.flush();

        ArrayList<String> fields1List = new ArrayList<String>();
        ArrayList<String> fields2List = new ArrayList<String>();
        ArrayList<String> class1List = new ArrayList<String>();
        ArrayList<String> class2List = new ArrayList<String>();


        for (int g = 0; g < fieldsArray.length; g++) {
            //   System.out.println(fieldsArray[g]);
            for (int h = g + 1; h < fieldsArray.length; h++) {
                fields1List.add(fieldsArray[g]);
                fields2List.add(fieldsArray[h]);
                class1List.add(classArray[g]);
                class2List.add(classArray[h]);
            }

        }

        for (int y = 0; y < fields1List.size(); y++) {

            String fS1 = class1List.get(y);
            String fS2 = class2List.get(y);

            String fN1 = "FIELD_NAME_" + fields1List.get(y).toUpperCase();
            String fN2 = "FIELD_NAME_" + fields2List.get(y).toUpperCase();

            boolean weightByRank = true;
            //create HTML file or not
            boolean createHTML = false;
            //binary classes
            String[] classes = {"yes", "no"};
            //number of neighbours
            int k = 1;
            //location of the index and the images, indexname has to be index+classname (indexPHOG)
            String indexLocation = "D:\\Datasets\\FashionTestItemDataSet\\idx\\index";
            String photosLocation = "D:\\Datasets\\FashionTestItemDataSet\\";
            //Testset
            String listFiles = "D:\\Datasets\\FashionTestItemDataSet\\itemtest.txt";

            String f1 = null;
            String f2 = null;
            try {
                f1 = (String) DocumentBuilder.class.getField(fN1).get(null);
                f2 = (String) DocumentBuilder.class.getField(fN2).get(null);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            LireFeature lf1 = null;
            LireFeature lf2 = null;
            try {
                lf1 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + fS1).newInstance();
                lf2 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + fS2).newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (ClassNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            String i1 = indexLocation + fS1;
            String i2 = indexLocation + fS2;

            //  for (int ik = 0;ik<k;ik++)       {

            //  System.out.println("Tests for lf1 " + f1 + " with k=" + k + " combined with " + f2 + " - weighting by rank sum: " + weightByRank);
            //  System.out.println("========================================");
            HashMap<String, Integer> tag2count = new HashMap<String, Integer>(k);
            HashMap<String, Double> tag2weight = new HashMap<String, Double>(k);
            int c = 0;   // used for just one class ...
            //        for (int c = 0; c < 10; c++) {
            String classIdentifier = classes[c];

            //"D:\\Datasets\\FashionTest\\fashion10000Test\\" + classIdentifier + ".txt";

            // INIT
            ArrayList<String> classesHTML = new ArrayList<String>();
            ArrayList<String> filesHTML = new ArrayList<String>();

           // int[] confusion = new int[2];
           // Arrays.fill(confusion, 0);
            HashMap<String, Integer> class2id = new HashMap<String, Integer>(2);
            for (int i = 0; i < classes.length; i++)
                class2id.put(classes[i], i);

            BufferedReader br = new BufferedReader(new FileReader(listFiles));
            String line;

            IndexReader ir1 = DirectoryReader.open(MMapDirectory.open(new File(i1)));
            IndexReader ir2 = DirectoryReader.open(MMapDirectory.open(new File(i2)));
            // in-memory linear search
            ImageSearcher bis1 = new GenericFastImageSearcher(k, lf1.getClass(), f1, true, ir1);
            ImageSearcher bis2 = new GenericFastImageSearcher(k, lf2.getClass(), f2, true, ir2);
            // hashing based searcher
            //BitSamplingImageSearcher bis1 = new BitSamplingImageSearcher(k, f1, f1 + "_hash", lf1, 3000);
            ImageSearchHits hits1;
            ImageSearchHits hits2;

            int count = 0, countCorrect = 0;
            double countTp = 0, countFp = 0, countTn = 0, countFn = 0;      //F1 Metric
            long ms = System.currentTimeMillis();
            while ((line = br.readLine()) != null) {
                try {
                    tag2count.clear();
                    tag2weight.clear();
                    hits1 = bis1.search(ImageIO.read(new File(line)), ir1);
                    hits2 = bis2.search(ImageIO.read(new File(line)), ir2);
                    //Print the tag of both searches
                    //System.out.println(getTag(hits1.doc(0)) + "\n" + getTag(hits2.doc(0)));

                    // set tag weights and counts.
                    for (int l = 0; l < k; l++) {
                        String tag1 = getTag(hits1.doc(l), photosLocation);
                        String tag2 = getTag(hits2.doc(l), photosLocation);

                        //  if (tag2count.get(tag1) == null) tag2count.put(tag1, 1);
                        //  else tag2count.put(tag1, tag2count.get(tag1) + 1);

                        //Simple combination
                        if (tag2count.get(tag1) == null && tag2count.get(tag2) == null) {
                            tag2count.put(tag1, 1);
                            tag2count.put(tag2, 1);
                        } else if (tag2count.get(tag1) != null && tag2count.get(tag2) != null) {
                            tag2count.put(tag1, tag2count.get(tag1) + 1);
                            tag2count.put(tag2, tag2count.get(tag2) + 1);
                        } else if (tag2count.get(tag1) != null && tag2count.get(tag2) == null) {
                            tag2count.put(tag1, tag2count.get(tag1) + 1);
                            tag2count.put(tag2, 1);
                        } else if (tag2count.get(tag1) == null && tag2count.get(tag2) != null) {
                            tag2count.put(tag1, 1);
                            tag2count.put(tag2, tag2count.get(tag2) + 1);
                        }


                        if (weightByRank) {
                            //only if rank weight used
                            if (tag2weight.get(tag1) == null && tag2weight.get(tag2) == null) {
                                tag2weight.put(tag1, (double) l);
                                tag2weight.put(tag2, (double) l);

                            } else if (tag2weight.get(tag1) != null && tag2weight.get(tag2) != null) {
                                tag2weight.put(tag1, (double) l + tag2weight.get(tag1));
                                tag2weight.put(tag2, (double) l + tag2weight.get(tag2));
                            } else if (tag2weight.get(tag1) != null && tag2weight.get(tag2) == null) {
                                tag2weight.put(tag1, (double) l + tag2weight.get(tag1));
                                tag2weight.put(tag2, (double) l);
                            } else if (tag2count.get(tag1) == null && tag2count.get(tag2) != null) {
                                tag2weight.put(tag1, (double) l);
                                tag2weight.put(tag2, (double) l + tag2weight.get(tag2));
                            }
                            //  else {
                            //      tag2weight.put(tag1, (double) l + tag2weight.get(tag1));
                            //}
                        } else {
                            // System.out.println(hits1.score(l));
                            //  System.out.println(hits2.score(l));
                            if (tag2weight.get(tag1) == null) {
                                if (Double.valueOf(hits1.score(l)) > Double.valueOf(hits2.score(l)))
                                    tag2weight.put(tag1, Double.valueOf(hits1.score(l)));
                                else
                                    tag2weight.put(tag2, Double.valueOf(hits2.score(l)));
                            } else {

                                if (Double.valueOf(hits1.score(l)) > Double.valueOf(hits2.score(l)))
                                    tag2weight.put(tag1, (double) l + hits1.score(l));
                                else
                                    tag2weight.put(tag2, (double) l + hits2.score(l));

                            }
                        }
                    }
                    // find class, iterate over the tags (classes):
                    int maxCount = 0, maxima = 0;
                    String classifiedAs = null;
                    for (Iterator<String> tagIterator = tag2count.keySet().iterator(); tagIterator.hasNext(); ) {
                        String tag = tagIterator.next();
                        if (tag2count.get(tag) > maxCount) {
                            maxCount = tag2count.get(tag);
                            maxima = 1;
                            classifiedAs = tag;
                        } else if (tag2count.get(tag) == maxCount) {
                            maxima++;
                        }
                    }
                    // if there are two or more classes with the same number of results, then we take a look at the weights.
                    // else the class is alread given in classifiedAs.
                    if (maxima > 1) {
                        double minWeight = Double.MAX_VALUE;
                        for (Iterator<String> tagIterator = tag2count.keySet().iterator(); tagIterator.hasNext(); ) {
                            String tag = tagIterator.next();
                            if (tag2weight.get(tag) < minWeight) {
                                minWeight = tag2weight.get(tag);
                                classifiedAs = tag;
                            }
                        }
                    }
//                    if (tag2.equals(tag3)) tag1 = tag2;
                    count++;
                    //SHOW THE CLASSIFICATION
                    //     System.out.println(classifiedAs+";"+line);
                    classesHTML.add(classifiedAs);
                    filesHTML.add(line);

                    //F1 Metric calculation
                    if (classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("yes")) {
                        countCorrect++;
                        countTp++;
                    } else if (!classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("yes"))
                        countFp++;

                    if (classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("no")) {
                        countCorrect++;
                        countTn++;
                    } else if (!classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("no"))
                        countFn++;
                    //if (classifiedAs.equals(getTagLine(line)))countCorrect++;
                    //if (classifiedAs.equals(classIdentifier)) countCorrect++;
                    //if (classifiedAs.equals(classIdentifier)) countCorrect++;
                    // confusion:
                    //confusion[class2id.get(classifiedAs)]++;
//                    System.out.printf("%10s (%4.3f, %10d, %4d)\n", classifiedAs, ((double) countCorrect / (double) count), count, (System.currentTimeMillis() - ms) / count);
                } catch (Exception e) {
                    System.err.println(">>> ERR:" + e.getMessage() + e);
                    //   throw (NullPointerException) e;
                }
            }

            //get all the evaluation values
            double precisicon = getPrecision(countTp, countFp);
            double recall = getRecall(countTp, countFn);
            double trueNegativeRate = getTrueNegativeRate(countTn, countFp);
            double accuracy = getAccuracy(countTp, countFp, countTn, countFn);
            double fMeasure = getFmeasure(precisicon, recall);
            double falsePositiveRate = getFalsePositiveRate(countFp, countTn);
//            System.out.println("Results for class " + classIdentifier);
//        System.out.printf("Class\tPrecision\tRecall\tTrue Negative Rate\tAccuracy\tF-Measure\tCount Test Images\tCount Corret\tms per test\n");
//        System.out.printf("%s\t%4.5f\t%4.5f\t%4.5f\t%4.5f\t%4.5f\t%10d\t%10d\t%4d\n", classIdentifier, precisicon, recall, trueNegativeRate,accuracy, fMeasure,  count, countCorrect, (System.currentTimeMillis() - ms) / count);


            System.out.println(y + 1 + " of " + class1List.size() + " finished. " + (System.currentTimeMillis() - ms) / 1000 + " seconds per round. " + "Feature: " + class1List.get(y) + " " + class2List.get(y) + " Current y: " + y);

            print_line.printf("%s;%s;%s;%s;%s;%s;%4.5f;%4.5f;%4.5f;%4.5f;%4.5f;%4.5f;%10d;%10d;%4d;%4.5f;%4.5f;%4.5f;%4.5f\n", class1List.get(y), class2List.get(y), "no", k, weightByRank, classIdentifier, precisicon, recall, trueNegativeRate, accuracy, falsePositiveRate, fMeasure, count, countCorrect, (System.currentTimeMillis() - ms) / count, countTp, countFp, countTn, countFn);
            print_line.flush();

//        System.out.printf("Confusion\t");
//            for (int i = 0; i < classes.length; i++) {
//                System.out.printf("%s\t", classes[i]);
//            }
//            System.out.println();
//        for (int i = 0; i < classes.length; i++) {
            //           System.out.printf("%d\t", confusion[i]);
//        }
            //  System.out.println();

            //Create HTML

            if (createHTML == true) {

                String fileName = "classifieresults-" + System.currentTimeMillis() / 1000 + ".html";
                BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
                bw.write("<html>\n" +
                        "<head><title>Classification Results</title></head>\n" +
                        "<body bgcolor=\"#FFFFFF\">\n");
                bw.write("<table>");

                // int elems = Math.min(filesHTML.size(),50);
                int elems = filesHTML.size();

                for (int i = 0; i < elems; i++) {
                    if (i % 3 == 0) bw.write("<tr>");

                    String s = filesHTML.get(i);
                    String colorF = "rgb(0, 255, 0)";

                    if (classesHTML.get(i).equals("no"))
                        colorF = "rgb(255, 0, 0)";
                    //  String s = reader.document(topDocs.scoreDocs[i].doc).get("descriptorImageIdentifier");
                    //  System.out.println(reader.document(topDocs.scoreDocs[i].doc).get("featLumLay"));
                    //  s = new File(s).getAbsolutePath();
                    // System.out.println(s);
                    bw.write("<td><a href=\"" + s + "\"><img style=\"max-width:220px;border:medium solid " + colorF + ";\"src=\"" + s + "\" border=\"" + 5 + "\" style=\"border: 3px\n" +
                            "black solid;\"></a></td>\n");
                    if (i % 3 == 2) bw.write("</tr>");
                }
                if (elems % 3 != 0) {
                    if (elems % 3 == 2) {
                        bw.write("<td>-</td with exit code 0\nd>\n");
                        bw.write("<td>-</td>\n");
                    } else if (elems % 3 == 2) {
                        bw.write("<td>-</td>\n");
                    }
                    bw.write("</tr>");
                }

                bw.write("</table></body>\n" +
                        "</html>");
                bw.close();
            }
            //   } // kfor
//        }
        }

        print_line.close();

    }

    public void testClassifyFashionThreeCombinedFeatures() throws IOException {

        PrintWriter print_line = new PrintWriter(new BufferedWriter(new FileWriter("D:\\resultsTripleFeature.txt")));

        String[] fieldsArray = {"CEDD", "EdgeHistogram", "FCTH", "ColorLayout", "PHOG", "JCD", "Gabor", "JpegCoeffs", "Tamura", "Luminance_Layout", "Opponent_Histogram", "ScalableColor"};
        String[] classArray = {"CEDD", "EdgeHistogram", "FCTH", "ColorLayout", "PHOG", "JCD", "Gabor", "JpegCoefficientHistogram", "Tamura", "LuminanceLayout", "OpponentHistogram", "ScalableColor"};

        print_line.print("Feature1;Feature2;Feature3;K=;Weight Rank=;Class;Precision;Recall;True Negative Rate;Accuracy;False Positive Rate;F-Measure;Count Test Images;Count Correct;ms per test;TP;FP;TN;FN");
        print_line.println();
        print_line.flush();

        ArrayList<String> fields1List = new ArrayList<String>();
        ArrayList<String> fields2List = new ArrayList<String>();
        ArrayList<String> fields3List = new ArrayList<String>();
        ArrayList<String> class1List = new ArrayList<String>();
        ArrayList<String> class2List = new ArrayList<String>();
        ArrayList<String> class3List = new ArrayList<String>();


        for (int g = 0; g < fieldsArray.length; g++) {
            //   System.out.println(fieldsArray[g]);
            for (int h = g + 1; h < fieldsArray.length; h++) {
                for (int f = h + 1; f < fieldsArray.length; f++) {

                    fields1List.add(fieldsArray[g]);
                    fields2List.add(fieldsArray[h]);
                    fields3List.add(fieldsArray[f]);
                    class1List.add(classArray[g]);
                    class2List.add(classArray[h]);
                    class3List.add(classArray[f]);
                }
            }

        }

        for (int y = 0; y < fields1List.size(); y++) {

            String fS1 = class1List.get(y);
            String fS2 = class2List.get(y);
            String fS3 = class3List.get(y);

            String fN1 = "FIELD_NAME_" + fields1List.get(y).toUpperCase();
            String fN2 = "FIELD_NAME_" + fields2List.get(y).toUpperCase();
            String fN3 = "FIELD_NAME_" + fields3List.get(y).toUpperCase();

            boolean weightByRank = true;
            boolean createHTML = false;
            String[] classes = {"yes", "no"};
            int k = 50;
            String indexLocation = "D:\\Datasets\\FashionTestItemDataSet\\idx\\index";
            String photosLocation = "D:\\Datasets\\FashionTestItemDataSet\\";
            //Testset
            String testFiles = "D:\\Datasets\\FashionTestFashionDataSet\\test.txt";

            String f1 = null;
            String f2 = null;
            String f3 = null;
               try{
                f1 = (String) DocumentBuilder.class.getField(fN1).get(null);
                f2 = (String) DocumentBuilder.class.getField(fN2).get(null);
                f3 = (String) DocumentBuilder.class.getField(fN3).get(null);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            LireFeature feature = null;
            LireFeature featureSecond = null;
            LireFeature featureThird = null;
            try {
                feature = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + fS1).newInstance();
                featureSecond = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + fS2).newInstance();
                featureThird = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + fS3).newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (ClassNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            String i1 = indexLocation + fS1;
            String i2 = indexLocation + fS2;
            String i3 = indexLocation + fS3;

            //boolean weightByRank = true;
            //String[] classes = {"yes", "no"};
            //int k = 70;
            // CONFIG
            //String f1 = DocumentBuilder.FIELD_NAME_EDGEHISTOGRAM;
            //String f2 = DocumentBuilder.FIELD_NAME_CEDD;
            //String f3 = DocumentBuilder.FIELD_NAME_PHOG;
            //LireFeature feature = new EdgeHistogram();
            //LireFeature featureSecond = new CEDD();
            //LireFeature featureThird = new PHOG();
            //String i1 = "D:\\Datasets\\FashionTestFashionDataSet\\idx\\indexEdgeHistogram";
            //String i2 = "D:\\Datasets\\FashionTestFashionDataSet\\idx\\indexCEDD";
            //String i3 = "D:\\Datasets\\FashionTestFashionDataSet\\idx\\indexPHOG";

            //  for (int ik = 0;ik<k;ik++)       {

            //System.out.println("Tests for feature " + f1 + " with k=" + k + " combined with " + f2 + " - weighting by rank sum: " + weightByRank);
            //System.out.println("========================================");
            HashMap<String, Integer> tag2count = new HashMap<String, Integer>(k);
            HashMap<String, Double> tag2weight = new HashMap<String, Double>(k);
            int c = 0;   // used for just one class ...
            //        for (int c = 0; c < 10; c++) {
            String classIdentifier = classes[c];

            //"D:\\Datasets\\FashionTest\\fashion10000Test\\" + classIdentifier + ".txt";

            // INIT
            ArrayList<String> classesHTML = new ArrayList<String>();
            ArrayList<String> filesHTML = new ArrayList<String>();


            int[] confusion = new int[2];
            Arrays.fill(confusion, 0);
            HashMap<String, Integer> class2id = new HashMap<String, Integer>(2);
            for (int i = 0; i < classes.length; i++)
                class2id.put(classes[i], i);

            BufferedReader br = new BufferedReader(new FileReader(testFiles));
            String line;

            IndexReader ir1 = DirectoryReader.open(MMapDirectory.open(new File(i1)));
            IndexReader ir2 = DirectoryReader.open(MMapDirectory.open(new File(i2)));
            IndexReader ir3 = DirectoryReader.open(MMapDirectory.open(new File(i3)));
            // in-memory linear search
            ImageSearcher bis1 = new GenericFastImageSearcher(k, feature.getClass(), f1, true, ir1);
            ImageSearcher bis2 = new GenericFastImageSearcher(k, featureSecond.getClass(), f2, true, ir2);
            ImageSearcher bis3 = new GenericFastImageSearcher(k, featureThird.getClass(), f3, true, ir3);
            // hashing based searcher
            //BitSamplingImageSearcher bis1 = new BitSamplingImageSearcher(k, f1, f1 + "_hash", feature, 3000);
            ImageSearchHits hits1;
            ImageSearchHits hits2;
            ImageSearchHits hits3;

            int count = 0, countCorrect = 0;
            double countTp = 0, countFp = 0, countTn = 0, countFn = 0;      //F1 Metric
            long ms = System.currentTimeMillis();
            while ((line = br.readLine()) != null) {
                try {
                    tag2count.clear();
                    tag2weight.clear();
                    hits1 = bis1.search(ImageIO.read(new File(line)), ir1);
                    hits2 = bis2.search(ImageIO.read(new File(line)), ir2);
                    hits3 = bis3.search(ImageIO.read(new File(line)), ir3);
                    //Print the tag of both searches
                    //System.out.println(getTag(hits1.doc(0)) + "\n" + getTag(hits2.doc(0)));

                    // set tag weights and counts.
                    for (int l = 0; l < k; l++) {
                        String tag1 = getTag(hits1.doc(l), photosLocation);
                        String tag2 = getTag(hits2.doc(l), photosLocation);
                        String tag3 = getTag(hits3.doc(l), photosLocation);

                        //  if (tag2count.get(tag1) == null) tag2count.put(tag1, 1);
                        //  else tag2count.put(tag1, tag2count.get(tag1) + 1);

                        //Simple combination
                        if (tag2count.get(tag1) == null && tag2count.get(tag2) == null && tag2count.get(tag3) == null) {
                            tag2count.put(tag1, 1);
                            tag2count.put(tag2, 1);
                            tag2count.put(tag3, 1);
                        } else if (tag2count.get(tag1) != null && tag2count.get(tag2) != null && tag2count.get(tag3) != null) {
                            tag2count.put(tag1, tag2count.get(tag1) + 1);
                            tag2count.put(tag2, tag2count.get(tag2) + 1);
                            tag2count.put(tag3, tag2count.get(tag3) + 1);
                        } else if (tag2count.get(tag1) != null && tag2count.get(tag2) != null && tag2count.get(tag3) == null) {
                            tag2count.put(tag1, tag2count.get(tag1) + 1);
                            tag2count.put(tag2, tag2count.get(tag2) + 1);
                            tag2count.put(tag3, 1);
                        } else if (tag2count.get(tag1) != null && tag2count.get(tag2) == null && tag2count.get(tag3) != null) {
                            tag2count.put(tag1, tag2count.get(tag1) + 1);
                            tag2count.put(tag2, 1);
                            tag2count.put(tag3, tag2count.get(tag3) + 1);
                        } else if (tag2count.get(tag1) == null && tag2count.get(tag2) != null && tag2count.get(tag3) != null) {
                            tag2count.put(tag1, 1);
                            tag2count.put(tag2, tag2count.get(tag2) + 1);
                            tag2count.put(tag3, tag2count.get(tag3) + 1);
                        } else if (tag2count.get(tag1) == null && tag2count.get(tag2) == null && tag2count.get(tag3) != null) {
                            tag2count.put(tag1, 1);
                            tag2count.put(tag2, 1);
                            tag2count.put(tag3, tag2count.get(tag3) + 1);
                        } else if (tag2count.get(tag1) == null && tag2count.get(tag2) != null && tag2count.get(tag3) == null) {
                            tag2count.put(tag1, 1);
                            tag2count.put(tag2, tag2count.get(tag2) + 1);
                            tag2count.put(tag3, 1);
                        } else if (tag2count.get(tag1) != null && tag2count.get(tag2) == null && tag2count.get(tag3) == null) {
                            tag2count.put(tag1, tag2count.get(tag1) + 1);
                            tag2count.put(tag2, 1);
                            tag2count.put(tag3, 1);
                        }


                        if (weightByRank) {
                            //only if rank weight used
                            if (tag2weight.get(tag1) == null && tag2weight.get(tag2) == null && tag2weight.get(tag3) == null) {
                                tag2weight.put(tag1, (double) l);
                                tag2weight.put(tag2, (double) l);
                                tag2weight.put(tag3, (double) l);
                            } else if (tag2weight.get(tag1) != null && tag2weight.get(tag2) != null && tag2weight.get(tag3) != null) {
                                tag2weight.put(tag1, (double) l + tag2weight.get(tag1));
                                tag2weight.put(tag2, (double) l + tag2weight.get(tag2));
                                tag2weight.put(tag3, (double) l + tag2weight.get(tag3));
                            } else if (tag2weight.get(tag1) != null && tag2weight.get(tag2) != null && tag2weight.get(tag3) == null) {
                                tag2weight.put(tag1, (double) l + tag2weight.get(tag1));
                                tag2weight.put(tag2, (double) l + tag2weight.get(tag2));
                                tag2weight.put(tag3, (double) l);
                            } else if (tag2count.get(tag1) != null && tag2count.get(tag2) == null && tag2weight.get(tag3) != null) {
                                tag2weight.put(tag1, (double) l + tag2weight.get(tag1));
                                tag2weight.put(tag2, (double) l);
                                tag2weight.put(tag3, (double) l + tag2weight.get(tag3));
                            } else if (tag2count.get(tag1) == null && tag2count.get(tag2) != null && tag2weight.get(tag3) != null) {
                                tag2weight.put(tag1, (double) l);
                                tag2weight.put(tag2, (double) l + tag2weight.get(tag2));
                                tag2weight.put(tag3, (double) l + tag2weight.get(tag3));
                            }
                            //  else {
                            //      tag2weight.put(tag1, (double) l + tag2weight.get(tag1));
                            //}
                        } else {
                            // System.out.println(hits1.score(l));
                            //  System.out.println(hits2.score(l));
                            if (tag2weight.get(tag1) == null) {
                                if (Double.valueOf(hits1.score(l)) > Double.valueOf(hits2.score(l)) && Double.valueOf(hits1.score(l)) > Double.valueOf(hits3.score(l)))
                                    tag2weight.put(tag1, Double.valueOf(hits1.score(l)));
                                else if (Double.valueOf(hits2.score(l)) > Double.valueOf(hits1.score(l)) && Double.valueOf(hits2.score(l)) > Double.valueOf(hits3.score(l)))
                                    tag2weight.put(tag2, Double.valueOf(hits2.score(l)));
                                else if (Double.valueOf(hits3.score(l)) > Double.valueOf(hits1.score(l)) && Double.valueOf(hits3.score(l)) > Double.valueOf(hits2.score(l)))
                                    tag2weight.put(tag3, Double.valueOf(hits3.score(l)));
                            } else {

                                if (Double.valueOf(hits1.score(l)) > Double.valueOf(hits2.score(l)) && Double.valueOf(hits1.score(l)) > Double.valueOf(hits3.score(l)))
                                    tag2weight.put(tag1, (double) l + hits1.score(l));
                                else if (Double.valueOf(hits2.score(l)) > Double.valueOf(hits1.score(l)) && Double.valueOf(hits2.score(l)) > Double.valueOf(hits3.score(l)))
                                    tag2weight.put(tag2, (double) l + hits2.score(l));
                                else if (Double.valueOf(hits3.score(l)) > Double.valueOf(hits1.score(l)) && Double.valueOf(hits3.score(l)) > Double.valueOf(hits2.score(l)))
                                    tag2weight.put(tag3, Double.valueOf(hits3.score(l)));

                            }
                        }
                    }
                    // find class, iterate over the tags (classes):
                    int maxCount = 0, maxima = 0;
                    String classifiedAs = null;
                    for (Iterator<String> tagIterator = tag2count.keySet().iterator(); tagIterator.hasNext(); ) {
                        String tag = tagIterator.next();
                        if (tag2count.get(tag) > maxCount) {
                            maxCount = tag2count.get(tag);
                            maxima = 1;
                            classifiedAs = tag;
                        } else if (tag2count.get(tag) == maxCount) {
                            maxima++;
                        }
                    }
                    // if there are two or more classes with the same number of results, then we take a look at the weights.
                    // else the class is alread given in classifiedAs.
                    if (maxima > 1) {
                        double minWeight = Double.MAX_VALUE;
                        for (Iterator<String> tagIterator = tag2count.keySet().iterator(); tagIterator.hasNext(); ) {
                            String tag = tagIterator.next();
                            if (tag2weight.get(tag) < minWeight) {
                                minWeight = tag2weight.get(tag);
                                classifiedAs = tag;
                            }
                        }
                    }
//                    if (tag2.equals(tag3)) tag1 = tag2;
                    count++;
                    //SHOW THE CLASSIFICATION
                    //     System.out.println(classifiedAs+";"+line);
                    classesHTML.add(classifiedAs);
                    filesHTML.add(line);

                    //F1 Metric
                    if (classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("yes")) {
                        countCorrect++;
                        countTp++;
                    } else if (!classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("yes"))
                        countFp++;

                    if (classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("no")) {
                        countCorrect++;
                        countTn++;
                    } else if (!classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("no"))
                        countFn++;
                    //if (classifiedAs.equals(getTagLine(line)))countCorrect++;
                    //if (classifiedAs.equals(classIdentifier)) countCorrect++;
                    //if (classifiedAs.equals(classIdentifier)) countCorrect++;
                    // confusion:
                    confusion[class2id.get(classifiedAs)]++;
//                    System.out.printf("%10s (%4.3f, %10d, %4d)\n", classifiedAs, ((double) countCorrect / (double) count), count, (System.currentTimeMillis() - ms) / count);
                } catch (Exception e) {
                    System.err.println(">>> ERR:" + e.getMessage() + e);
                    //   throw (NullPointerException) e;
                }
            }

            double precisicon = getPrecision(countTp, countFp);
            double recall = getRecall(countTp, countFn);
            double trueNegativeRate = getTrueNegativeRate(countTn, countFp);
            double accuracy = getAccuracy(countTp, countFp, countTn, countFn);
            double fMeasure = getFmeasure(precisicon, recall);
            double falsePositiveRate = getFalsePositiveRate(countFp, countTn);
//            System.out.println("Results for class " + classIdentifier);
            // System.out.printf("Class\tPrecision\tRecall\tTrue Negative Rate\tAccuracy\tF-Measure\tCount Test Images\tCount Corret\tms per test\n");
            // System.out.printf("%s\t%4.5f\t%4.5f\t%4.5f\t%4.5f\t%4.5f\t%10d\t%10d\t%4d\n", classIdentifier, precisicon, recall, trueNegativeRate,accuracy, fMeasure,  count, countCorrect, (System.currentTimeMillis() - ms) / count);

            System.out.println(y + 1 + " of " + class1List.size() + " finished. " + (System.currentTimeMillis() - ms) / 1000 + " seconds per round. " + "Feature: " + class1List.get(y) + " " + class2List.get(y) + " " + class3List.get(y) + " Current y: " + y);

            print_line.printf("%s;%s;%s;%s;%s;%s;%4.5f;%4.5f;%4.5f;%4.5f;%4.5f;%4.5f;%10d;%10d;%4d;%4.5f;%4.5f;%4.5f;%4.5f\n", class1List.get(y), class2List.get(y), class3List.get(y), k, weightByRank, classIdentifier, precisicon, recall, trueNegativeRate, accuracy, falsePositiveRate, fMeasure, count, countCorrect, (System.currentTimeMillis() - ms) / count, countTp, countFp, countTn, countFn);
            print_line.flush();

//        System.out.printf("Confusion\t");
//            for (int i = 0; i < classes.length; i++) {
//                System.out.printf("%s\t", classes[i]);
//            }
//            System.out.println();
//        for (int i = 0; i < classes.length; i++) {
            //           System.out.printf("%d\t", confusion[i]);
//        }
            //   System.out.println();

            //Create HTML
            if (createHTML == true) {

                String fileName = "classifieresults-" + System.currentTimeMillis() / 1000 + ".html";
                BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
                bw.write("<html>\n" +
                        "<head><title>Classification Results</title></head>\n" +
                        "<body bgcolor=\"#FFFFFF\">\n");
                bw.write("<table>");

                // int elems = Math.min(filesHTML.size(),50);
                int elems = filesHTML.size();

                for (int i = 0; i < elems; i++) {
                    if (i % 3 == 0) bw.write("<tr>");

                    String s = filesHTML.get(i);
                    String colorF = "rgb(0, 255, 0)";

                    if (classesHTML.get(i).equals("no"))
                        colorF = "rgb(255, 0, 0)";
                    //  String s = reader.document(topDocs.scoreDocs[i].doc).get("descriptorImageIdentifier");
                    //  System.out.println(reader.document(topDocs.scoreDocs[i].doc).get("featLumLay"));
                    //  s = new File(s).getAbsolutePath();
                    // System.out.println(s);
                    bw.write("<td><a href=\"" + s + "\"><img style=\"max-width:220px;border:medium solid " + colorF + ";\"src=\"" + s + "\" border=\"" + 5 + "\" style=\"border: 3px\n" +
                            "black solid;\"></a></td>\n");
                    if (i % 3 == 2) bw.write("</tr>");
                }
                if (elems % 3 != 0) {
                    if (elems % 3 == 2) {
                        bw.write("<td>-</td with exit code 0\nd>\n");
                        bw.write("<td>-</td>\n");
                    } else if (elems % 3 == 2) {
                        bw.write("<td>-</td>\n");
                    }
                    bw.write("</tr>");
                }

                bw.write("</table></body>\n" +
                        "</html>");
                bw.close();
            }
            //   } // kfor
//        }
        }
        print_line.close();
    }

    public static boolean testClassifyFashionThreeCombinedFeaturesMulti(int start, int end, String storeToFile) throws IOException {

        PrintWriter print_line = new PrintWriter(new BufferedWriter(new FileWriter(storeToFile)));

        String[] fieldsArray = {"CEDD", "EdgeHistogram", "FCTH", "ColorLayout", "PHOG", "JCD", "Gabor", "JpegCoeffs", "Tamura", "Luminance_Layout", "Opponent_Histogram", "ScalableColor"};
        String[] classArray = {"CEDD", "EdgeHistogram", "FCTH", "ColorLayout", "PHOG", "JCD", "Gabor", "JpegCoefficientHistogram", "Tamura", "LuminanceLayout", "OpponentHistogram", "ScalableColor"};

        print_line.print("Feature1;Feature2;Feature3;K=;Weight Rank=;Class;Precision;Recall;True Negative Rate;Accuracy;False Positive Rate;F-Measure;Count Test Images;Count Correct;ms per test;TP;FP;TN;FN");
        print_line.println();
        print_line.flush();

        ArrayList<String> fields1List = new ArrayList<String>();
        ArrayList<String> fields2List = new ArrayList<String>();
        ArrayList<String> fields3List = new ArrayList<String>();
        ArrayList<String> class1List = new ArrayList<String>();
        ArrayList<String> class2List = new ArrayList<String>();
        ArrayList<String> class3List = new ArrayList<String>();


        for (int g = 0; g < fieldsArray.length; g++) {
            //   System.out.println(fieldsArray[g]);
            for (int h = g + 1; h < fieldsArray.length; h++) {
                for (int f = h + 1; f < fieldsArray.length; f++) {

                    fields1List.add(fieldsArray[g]);
                    fields2List.add(fieldsArray[h]);
                    fields3List.add(fieldsArray[f]);
                    class1List.add(classArray[g]);
                    class2List.add(classArray[h]);
                    class3List.add(classArray[f]);
                }
            }

        }

        for (int y = start; y < end; y++) {

            String fS1 = class1List.get(y);
            String fS2 = class2List.get(y);
            String fS3 = class3List.get(y);

            String fN1 = "FIELD_NAME_" + fields1List.get(y).toUpperCase();
            String fN2 = "FIELD_NAME_" + fields2List.get(y).toUpperCase();
            String fN3 = "FIELD_NAME_" + fields3List.get(y).toUpperCase();

            boolean weightByRank = true;
            boolean createHTML = false;
            String[] classes = {"yes", "no"};
            int k = 3;
            String indexLocation = "D:\\Datasets\\FashionTestItemDataSet\\idx\\index";
            String photosLocation = "D:\\Datasets\\FashionTestItemDataSet\\";
            //Testset
            String listFiles = "D:\\Datasets\\FashionTestItemDataSet\\itemtest.txt";
            // CONFIG

            String f1 = null;
            String f2 = null;
            String f3 = null;
            try {
                f1 = (String) DocumentBuilder.class.getField(fN1).get(null);
                f2 = (String) DocumentBuilder.class.getField(fN2).get(null);
                f3 = (String) DocumentBuilder.class.getField(fN3).get(null);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            LireFeature lf1 = null;
            LireFeature lf2 = null;
            LireFeature lf3 = null;
            try {
                lf1 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + fS1).newInstance();
                lf2 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + fS2).newInstance();
                lf3 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + fS3).newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (ClassNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            String i1 = indexLocation + fS1;
            String i2 = indexLocation + fS2;
            String i3 = indexLocation + fS3;

            //boolean weightByRank = true;
            //String[] classes = {"yes", "no"};
            //int k = 70;
            // CONFIG
            //String f1 = DocumentBuilder.FIELD_NAME_EDGEHISTOGRAM;
            //String f2 = DocumentBuilder.FIELD_NAME_CEDD;
            //String f3 = DocumentBuilder.FIELD_NAME_PHOG;
            //LireFeature lf1 = new EdgeHistogram();
            //LireFeature lf2 = new CEDD();
            //LireFeature lf3 = new PHOG();
            //String i1 = "D:\\Datasets\\FashionTestFashionDataSet\\idx\\indexEdgeHistogram";
            //String i2 = "D:\\Datasets\\FashionTestFashionDataSet\\idx\\indexCEDD";
            //String i3 = "D:\\Datasets\\FashionTestFashionDataSet\\idx\\indexPHOG";

            //  for (int ik = 0;ik<k;ik++)       {

            //System.out.println("Tests for lf1 " + f1 + " with k=" + k + " combined with " + f2 + " - weighting by rank sum: " + weightByRank);
            //System.out.println("========================================");
            HashMap<String, Integer> tag2count = new HashMap<String, Integer>(k);
            HashMap<String, Double> tag2weight = new HashMap<String, Double>(k);
            int c = 0;   // used for just one class ...
            //        for (int c = 0; c < 10; c++) {
            String classIdentifier = classes[c];

            //"D:\\Datasets\\FashionTest\\fashion10000Test\\" + classIdentifier + ".txt";

            // INIT
            ArrayList<String> classesHTML = new ArrayList<String>();
            ArrayList<String> filesHTML = new ArrayList<String>();


            int[] confusion = new int[2];
            Arrays.fill(confusion, 0);
            HashMap<String, Integer> class2id = new HashMap<String, Integer>(2);
            for (int i = 0; i < classes.length; i++)
                class2id.put(classes[i], i);

            BufferedReader br = new BufferedReader(new FileReader(listFiles));
            String line;

            IndexReader ir1 = DirectoryReader.open(MMapDirectory.open(new File(i1)));
            IndexReader ir2 = DirectoryReader.open(MMapDirectory.open(new File(i2)));
            IndexReader ir3 = DirectoryReader.open(MMapDirectory.open(new File(i3)));
            // in-memory linear search
            ImageSearcher bis1 = new GenericFastImageSearcher(k, lf1.getClass(), f1, true, ir1);
            ImageSearcher bis2 = new GenericFastImageSearcher(k, lf2.getClass(), f2, true, ir2);
            ImageSearcher bis3 = new GenericFastImageSearcher(k, lf3.getClass(), f3, true, ir3);
            // hashing based searcher
            //BitSamplingImageSearcher bis1 = new BitSamplingImageSearcher(k, f1, f1 + "_hash", lf1, 3000);
            ImageSearchHits hits1;
            ImageSearchHits hits2;
            ImageSearchHits hits3;

            int count = 0, countCorrect = 0;
            double countTp = 0, countFp = 0, countTn = 0, countFn = 0;      //F1 Metric
            long ms = System.currentTimeMillis();
            while ((line = br.readLine()) != null) {
                try {
                    tag2count.clear();
                    tag2weight.clear();
                    tag2count.put("yes",1);
                    tag2count.put("no",1);
                    tag2weight.put("yes",1.0);
                    tag2weight.put("no",1.0);

                    hits1 = bis1.search(ImageIO.read(new File(line)), ir1);
                    hits2 = bis2.search(ImageIO.read(new File(line)), ir2);
                    hits3 = bis3.search(ImageIO.read(new File(line)), ir3);
                    //Print the tag of both searches
                    //System.out.println(getTag(hits1.doc(0)) + "\n" + getTag(hits2.doc(0)));

                    // set tag weights and counts.
                    for (int l = 0; l < k; l++) {
                        String tag = getTag(hits1.doc(l), photosLocation);
                        String tagSecond = getTag(hits2.doc(l), photosLocation);
                        String tagThird = getTag(hits3.doc(l), photosLocation);

                        //Simple combination

                        tag2count.put(tag, tag2count.get(tag) + 1);
                        tag2count.put(tagSecond, tag2count.get(tagSecond) + 1);
                        tag2count.put(tagThird, tag2count.get(tagThird) + 1);

                       if (weightByRank) {
                            //only if rank weight used
                            if (tag2weight.get(tag) == null && tag2weight.get(tagSecond) == null && tag2weight.get(tagThird) == null) {
                                tag2weight.put(tag, (double) l);
                                tag2weight.put(tagSecond, (double) l);
                                tag2weight.put(tagThird, (double) l);
                            } else if (tag2weight.get(tag) != null && tag2weight.get(tagSecond) != null && tag2weight.get(tagThird) != null) {
                                tag2weight.put(tag, (double) l + tag2weight.get(tag));
                                tag2weight.put(tagSecond, (double) l + tag2weight.get(tagSecond));
                                tag2weight.put(tagThird, (double) l + tag2weight.get(tagThird));
                            } else if (tag2weight.get(tag) != null && tag2weight.get(tagSecond) != null && tag2weight.get(tagThird) == null) {
                                tag2weight.put(tag, (double) l + tag2weight.get(tag));
                                tag2weight.put(tagSecond, (double) l + tag2weight.get(tagSecond));
                                tag2weight.put(tagThird, (double) l);
                            } else if (tag2count.get(tag) != null && tag2count.get(tagSecond) == null && tag2weight.get(tagThird) != null) {
                                tag2weight.put(tag, (double) l + tag2weight.get(tag));
                                tag2weight.put(tagSecond, (double) l);
                                tag2weight.put(tagThird, (double) l + tag2weight.get(tagThird));
                            } else if (tag2count.get(tag) == null && tag2count.get(tagSecond) != null && tag2weight.get(tagThird) != null) {
                                tag2weight.put(tag, (double) l);
                                tag2weight.put(tagSecond, (double) l + tag2weight.get(tagSecond));
                                tag2weight.put(tagThird, (double) l + tag2weight.get(tagThird));
                            }
                            //  else {
                            //      tag2weight.put(tag, (double) l + tag2weight.get(tag));
                            //}
                        } else {
                            // System.out.println(hits1.score(l));
                            //  System.out.println(hits2.score(l));
                            if (tag2weight.get(tag) == null) {
                                if (Double.valueOf(hits1.score(l)) > Double.valueOf(hits2.score(l)) && Double.valueOf(hits1.score(l)) > Double.valueOf(hits3.score(l)))
                                    tag2weight.put(tag, Double.valueOf(hits1.score(l)));
                                else if (Double.valueOf(hits2.score(l)) > Double.valueOf(hits1.score(l)) && Double.valueOf(hits2.score(l)) > Double.valueOf(hits3.score(l)))
                                    tag2weight.put(tagSecond, Double.valueOf(hits2.score(l)));
                                else if (Double.valueOf(hits3.score(l)) > Double.valueOf(hits1.score(l)) && Double.valueOf(hits3.score(l)) > Double.valueOf(hits2.score(l)))
                                    tag2weight.put(tagThird, Double.valueOf(hits3.score(l)));
                            } else {

                                if (Double.valueOf(hits1.score(l)) > Double.valueOf(hits2.score(l)) && Double.valueOf(hits1.score(l)) > Double.valueOf(hits3.score(l)))
                                    tag2weight.put(tag, (double) l + hits1.score(l));
                                else if (Double.valueOf(hits2.score(l)) > Double.valueOf(hits1.score(l)) && Double.valueOf(hits2.score(l)) > Double.valueOf(hits3.score(l)))
                                    tag2weight.put(tagSecond, (double) l + hits2.score(l));
                                else if (Double.valueOf(hits3.score(l)) > Double.valueOf(hits1.score(l)) && Double.valueOf(hits3.score(l)) > Double.valueOf(hits2.score(l)))
                                    tag2weight.put(tagThird, Double.valueOf(hits3.score(l)));

                            }
                        }
                    }
                    // find class, iterate over the tags (classes):
                    int maxCount = 0, maxima = 0;
                    String classifiedAs = null;
                    for (Iterator<String> tagIterator = tag2count.keySet().iterator(); tagIterator.hasNext(); ) {
                        String tag = tagIterator.next();
                        if (tag2count.get(tag) > maxCount) {
                            maxCount = tag2count.get(tag);
                            maxima = 1;
                            classifiedAs = tag;
                        } else if (tag2count.get(tag) == maxCount) {
                            maxima++;
                        }
                    }
                    // if there are two or more classes with the same number of results, then we take a look at the weights.
                    // else the class is alread given in classifiedAs.
                    if (maxima > 1) {
                        double minWeight = Double.MAX_VALUE;
                        for (Iterator<String> tagIterator = tag2count.keySet().iterator(); tagIterator.hasNext(); ) {
                            String tag = tagIterator.next();
                            if (tag2weight.get(tag) < minWeight) {
                                minWeight = tag2weight.get(tag);
                                classifiedAs = tag;
                            }
                        }
                    }
//                    if (tag2.equals(tag3)) tag1 = tag2;
                    count++;
                    //SHOW THE CLASSIFICATION
                    //     System.out.println(classifiedAs+";"+line);
                    classesHTML.add(classifiedAs);
                    filesHTML.add(line);

                    //F1 Metric
                    if (classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("yes")) {
                        countCorrect++;
                        countTp++;
                    } else if (!classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("yes"))
                        countFp++;

                    if (classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("no")) {
                        countCorrect++;
                        countTn++;
                    } else if (!classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("no"))
                        countFn++;
                    //if (classifiedAs.equals(getTagLine(line)))countCorrect++;
                    //if (classifiedAs.equals(classIdentifier)) countCorrect++;
                    //if (classifiedAs.equals(classIdentifier)) countCorrect++;
                    // confusion:
                    confusion[class2id.get(classifiedAs)]++;
//                    System.out.printf("%10s (%4.3f, %10d, %4d)\n", classifiedAs, ((double) countCorrect / (double) count), count, (System.currentTimeMillis() - ms) / count);
                } catch (Exception e) {
                    System.err.println(">>> ERR:" + e.getMessage() + e);
                    //   throw (NullPointerException) e;
                }
            }

            double precisicon = getPrecision(countTp, countFp);
            double recall = getRecall(countTp, countFn);
            double trueNegativeRate = getTrueNegativeRate(countTn, countFp);
            double accuracy = getAccuracy(countTp, countFp, countTn, countFn);
            double fMeasure = getFmeasure(precisicon, recall);
            double falsePositiveRate = getFalsePositiveRate(countFp, countTn);
//            System.out.println("Results for class " + classIdentifier);
            // System.out.printf("Class\tPrecision\tRecall\tTrue Negative Rate\tAccuracy\tF-Measure\tCount Test Images\tCount Corret\tms per test\n");
            // System.out.printf("%s\t%4.5f\t%4.5f\t%4.5f\t%4.5f\t%4.5f\t%10d\t%10d\t%4d\n", classIdentifier, precisicon, recall, trueNegativeRate,accuracy, fMeasure,  count, countCorrect, (System.currentTimeMillis() - ms) / count);

            System.out.println(y + 1 + " of " + class1List.size() + " finished. " + (System.currentTimeMillis() - ms) / 1000 + " seconds per round. " + "Feature: " + class1List.get(y) + " " + class2List.get(y) + " " + class3List.get(y) + " Current y: " + y);

            print_line.printf("%s;%s;%s;%s;%s;%s;%4.5f;%4.5f;%4.5f;%4.5f;%4.5f;%4.5f;%10d;%10d;%4d;%4.5f;%4.5f;%4.5f;%4.5f\n", class1List.get(y), class2List.get(y), class3List.get(y), k, weightByRank, classIdentifier, precisicon, recall, trueNegativeRate, accuracy, falsePositiveRate, fMeasure, count, countCorrect, (System.currentTimeMillis() - ms) / count, countTp, countFp, countTn, countFn);
            print_line.flush();

//        System.out.printf("Confusion\t");
//            for (int i = 0; i < classes.length; i++) {
//                System.out.printf("%s\t", classes[i]);
//            }
//            System.out.println();
//        for (int i = 0; i < classes.length; i++) {
            //           System.out.printf("%d\t", confusion[i]);
//        }
            //   System.out.println();

            //Create HTML
            if (createHTML == true) {

                String fileName = "classifieresults-" + System.currentTimeMillis() / 1000 + ".html";
                BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
                bw.write("<html>\n" +
                        "<head><title>Classification Results</title></head>\n" +
                        "<body bgcolor=\"#FFFFFF\">\n");
                bw.write("<table>");

                // int elems = Math.min(filesHTML.size(),50);
                int elems = filesHTML.size();

                for (int i = 0; i < elems; i++) {
                    if (i % 3 == 0) bw.write("<tr>");

                    String s = filesHTML.get(i);
                    String colorF = "rgb(0, 255, 0)";

                    if (classesHTML.get(i).equals("no"))
                        colorF = "rgb(255, 0, 0)";
                    //  String s = reader.document(topDocs.scoreDocs[i].doc).get("descriptorImageIdentifier");
                    //  System.out.println(reader.document(topDocs.scoreDocs[i].doc).get("featLumLay"));
                    //  s = new File(s).getAbsolutePath();
                    // System.out.println(s);
                    bw.write("<td><a href=\"" + s + "\"><img style=\"max-width:220px;border:medium solid " + colorF + ";\"src=\"" + s + "\" border=\"" + 5 + "\" style=\"border: 3px\n" +
                            "black solid;\"></a></td>\n");
                    if (i % 3 == 2) bw.write("</tr>");
                }
                if (elems % 3 != 0) {
                    if (elems % 3 == 2) {
                        bw.write("<td>-</td with exit code 0\nd>\n");
                        bw.write("<td>-</td>\n");
                    } else if (elems % 3 == 2) {
                        bw.write("<td>-</td>\n");
                    }
                    bw.write("</tr>");
                }

                bw.write("</table></body>\n" +
                        "</html>");
                bw.close();
            }
            //   } // kfor
//        }
        }
        print_line.close();
        return true;
    }

    public void testClassifyFashionAllCombinedFeatures() throws IOException {

        PrintWriter print_line = new PrintWriter(new BufferedWriter(new FileWriter("D:\\resultsallFeatureK7.txt")));

        String[] fieldsArray = {"CEDD", "EdgeHistogram", "FCTH", "ColorLayout", "PHOG", "JCD", "Gabor", "JpegCoeffs", "Tamura", "Luminance_Layout", "Opponent_Histogram", "ScalableColor"};
        String[] classArray = {"CEDD", "EdgeHistogram", "FCTH", "ColorLayout", "PHOG", "JCD", "Gabor", "JpegCoefficientHistogram", "Tamura", "LuminanceLayout", "OpponentHistogram", "ScalableColor"};

        print_line.print("Feature1;Feature2;Feature3;K=;Weight Rank=;Class;Precision;Recall;True Negative Rate;Accuracy;False Positive Rate;F-Measure;Count Test Images;Count Correct;ms per test;TP;FP;TN;FN");
        print_line.println();
        print_line.flush();


        boolean weightByRank = true;
        boolean createHTML = false;
        String[] classes = {"yes", "no"};
        int k = 50;
        String indexLocation = "D:\\Datasets\\FashionTestItemDataSet\\idx\\index";
        String photosLocation = "D:\\Datasets\\FashionTestItemDataSet\\";
        //Testset
        String listFiles = "D:\\Datasets\\FashionTestItemDataSet\\itemtest.txt";
        //  String indexLocation = "D:\\Datasets\\FashionTestFashionDataSet\\idx\\index";
        // CONFIG


        String f1 = null;
        String f2 = null;
        String f3 = null;
        String f4 = null;
        String f5 = null;
        String f6 = null;
        String f7 = null;
        String f8 = null;
        String f9 = null;
        String f10 = null;
        String f11 = null;
        String f12 = null;

        try {
            f1 = (String) DocumentBuilder.class.getField("FIELD_NAME_" + fieldsArray[0].toUpperCase()).get(null);
            f2 = (String) DocumentBuilder.class.getField("FIELD_NAME_" + fieldsArray[1].toUpperCase()).get(null);
            f3 = (String) DocumentBuilder.class.getField("FIELD_NAME_" + fieldsArray[2].toUpperCase()).get(null);
            f4 = (String) DocumentBuilder.class.getField("FIELD_NAME_" + fieldsArray[3].toUpperCase()).get(null);
            f5 = (String) DocumentBuilder.class.getField("FIELD_NAME_" + fieldsArray[4].toUpperCase()).get(null);
            f6 = (String) DocumentBuilder.class.getField("FIELD_NAME_" + fieldsArray[5].toUpperCase()).get(null);
            f7 = (String) DocumentBuilder.class.getField("FIELD_NAME_" + fieldsArray[6].toUpperCase()).get(null);
            f8 = (String) DocumentBuilder.class.getField("FIELD_NAME_" + fieldsArray[7].toUpperCase()).get(null);
            f9 = (String) DocumentBuilder.class.getField("FIELD_NAME_" + fieldsArray[8].toUpperCase()).get(null);
            f10 = (String) DocumentBuilder.class.getField("FIELD_NAME_" + fieldsArray[9].toUpperCase()).get(null);
            f11 = (String) DocumentBuilder.class.getField("FIELD_NAME_" + fieldsArray[10].toUpperCase()).get(null);
            f12 = (String) DocumentBuilder.class.getField("FIELD_NAME_" + fieldsArray[11].toUpperCase()).get(null);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        LireFeature lf1 = null;
        LireFeature lf2 = null;
        LireFeature lf3 = null;
        LireFeature lf4 = null;
        LireFeature lf5 = null;
        LireFeature lf6 = null;
        LireFeature lf7 = null;
        LireFeature lf8 = null;
        LireFeature lf9 = null;
        LireFeature lf10 = null;
        LireFeature lf11 = null;
        LireFeature lf12 = null;
        try {
            lf1 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + classArray[0]).newInstance();
            lf2 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + classArray[1]).newInstance();
            lf3 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + classArray[2]).newInstance();
            lf4 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + classArray[3]).newInstance();
            lf5 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + classArray[4]).newInstance();
            lf6 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + classArray[5]).newInstance();
            lf7 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + classArray[6]).newInstance();
            lf8 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + classArray[7]).newInstance();
            lf9 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + classArray[8]).newInstance();
            lf10 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + classArray[9]).newInstance();
            lf11 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + classArray[10]).newInstance();
            lf12 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + classArray[11]).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        String i1 = indexLocation + classArray[0];
        String i2 = indexLocation + classArray[1];
        String i3 = indexLocation + classArray[2];
        String i4 = indexLocation + classArray[3];
        String i5 = indexLocation + classArray[4];
        String i6 = indexLocation + classArray[5];
        String i7 = indexLocation + classArray[6];
        String i8 = indexLocation + classArray[7];
        String i9 = indexLocation + classArray[8];
        String i10 = indexLocation + classArray[9];
        String i11 = indexLocation + classArray[10];
        String i12 = indexLocation + classArray[11];


        //  for (int ik = 0;ik<k;ik++)       {

        //System.out.println("Tests for feature " + fieldName + " with k=" + k + " combined with " + fieldNameSecond + " - weighting by rank sum: " + weightByRank);
        //System.out.println("========================================");
        HashMap<String, Integer> tag2count = new HashMap<String, Integer>(k);
        HashMap<String, Double> tag2weight = new HashMap<String, Double>(k);

        //   tag2count.put("yes",1);
        //   tag2count.put("no",1);

        int c = 0;   // used for just one class ...
        //        for (int c = 0; c < 10; c++) {
        String classIdentifier = classes[c];

        //"D:\\Datasets\\FashionTest\\fashion10000Test\\" + classIdentifier + ".txt";

        // INIT
        ArrayList<String> classesHTML = new ArrayList<String>();
        ArrayList<String> filesHTML = new ArrayList<String>();


        //   int[] confusion = new int[2];
        //   Arrays.fill(confusion, 0);
        HashMap<String, Integer> class2id = new HashMap<String, Integer>(2);
        for (int i = 0; i < classes.length; i++)
            class2id.put(classes[i], i);

        BufferedReader br = new BufferedReader(new FileReader(listFiles));
        String line;

        IndexReader ir1 = DirectoryReader.open(MMapDirectory.open(new File(i1)));
        IndexReader ir2 = DirectoryReader.open(MMapDirectory.open(new File(i2)));
        IndexReader ir3 = DirectoryReader.open(MMapDirectory.open(new File(i3)));
        IndexReader ir4 = DirectoryReader.open(MMapDirectory.open(new File(i4)));
        IndexReader ir5 = DirectoryReader.open(MMapDirectory.open(new File(i5)));
        IndexReader ir6 = DirectoryReader.open(MMapDirectory.open(new File(i6)));
        IndexReader ir7 = DirectoryReader.open(MMapDirectory.open(new File(i7)));
        IndexReader ir8 = DirectoryReader.open(MMapDirectory.open(new File(i8)));
        IndexReader ir9 = DirectoryReader.open(MMapDirectory.open(new File(i9)));
        IndexReader ir10 = DirectoryReader.open(MMapDirectory.open(new File(i10)));
        IndexReader ir11 = DirectoryReader.open(MMapDirectory.open(new File(i11)));
        IndexReader ir12 = DirectoryReader.open(MMapDirectory.open(new File(i12)));

        // in-memory linear search
        ImageSearcher bis1 = new GenericFastImageSearcher(k, lf1.getClass(), f1, true, ir1);
        ImageSearcher bis2 = new GenericFastImageSearcher(k, lf2.getClass(), f2, true, ir2);
        ImageSearcher bis3 = new GenericFastImageSearcher(k, lf3.getClass(), f3, true, ir3);
        ImageSearcher bis4 = new GenericFastImageSearcher(k, lf4.getClass(), f4, true, ir4);
        ImageSearcher bis5 = new GenericFastImageSearcher(k, lf5.getClass(), f5, true, ir5);
        ImageSearcher bis6 = new GenericFastImageSearcher(k, lf6.getClass(), f6, true, ir6);
        ImageSearcher bis7 = new GenericFastImageSearcher(k, lf7.getClass(), f7, true, ir7);
        ImageSearcher bis8 = new GenericFastImageSearcher(k, lf8.getClass(), f8, true, ir8);
        ImageSearcher bis9 = new GenericFastImageSearcher(k, lf9.getClass(), f9, true, ir9);
        ImageSearcher bis10 = new GenericFastImageSearcher(k, lf10.getClass(), f10, true, ir10);
        ImageSearcher bis11 = new GenericFastImageSearcher(k, lf11.getClass(), f11, true, ir11);
        ImageSearcher bis12 = new GenericFastImageSearcher(k, lf12.getClass(), f12, true, ir12);
        // hashing based searcher
        //BitSamplingImageSearcher bis = new BitSamplingImageSearcher(k, fieldName, fieldName + "_hash", feature, 3000);

        ImageSearchHits hits1;
        ImageSearchHits hits2;
        ImageSearchHits hits3;
        ImageSearchHits hits4;
        ImageSearchHits hits5;
        ImageSearchHits hits6;
        ImageSearchHits hits7;
        ImageSearchHits hits8;
        ImageSearchHits hits9;
        ImageSearchHits hits10;
        ImageSearchHits hits11;
        ImageSearchHits hits12;

        int count = 0, countCorrect = 0;
        double countTp = 0, countFp = 0, countTn = 0, countFn = 0;      //F1 Metric
        long ms = System.currentTimeMillis();
        while ((line = br.readLine()) != null) {

            System.out.println(line);
            System.out.println(count);

            try {
                tag2count.clear();
                tag2count.put("yes", 1);
                tag2count.put("no", 1);
                tag2weight.clear();
                tag2weight.put("yes", 1.0);
                tag2weight.put("no", 1.0);


                hits1 = bis1.search(ImageIO.read(new File(line)), ir1);
                hits2 = bis2.search(ImageIO.read(new File(line)), ir2);
                hits3 = bis3.search(ImageIO.read(new File(line)), ir3);
                hits4 = bis4.search(ImageIO.read(new File(line)), ir4);
                hits5 = bis5.search(ImageIO.read(new File(line)), ir5);
                hits6 = bis6.search(ImageIO.read(new File(line)), ir6);
                hits7 = bis7.search(ImageIO.read(new File(line)), ir7);
                hits8 = bis8.search(ImageIO.read(new File(line)), ir8);
                hits9 = bis9.search(ImageIO.read(new File(line)), ir9);
                hits10 = bis10.search(ImageIO.read(new File(line)), ir10);
                hits11 = bis11.search(ImageIO.read(new File(line)), ir11);
                hits12 = bis12.search(ImageIO.read(new File(line)), ir12);
                //Print the tag of both searches
                //System.out.println(getTag(hits.doc(0)) + "\n" + getTag(hitsSecond.doc(0)));

                // set tag weights and counts.
                for (int l = 0; l < k; l++) {
                    String tag1 = getTag(hits1.doc(l), photosLocation);
                    String tag2 = getTag(hits2.doc(l), photosLocation);
                    String tag3 = getTag(hits3.doc(l), photosLocation);
                    String tag4 = getTag(hits4.doc(l), photosLocation);
                    String tag5 = getTag(hits5.doc(l), photosLocation);
                    String tag6 = getTag(hits6.doc(l), photosLocation);
                    String tag7 = getTag(hits7.doc(l), photosLocation);
                    String tag8 = getTag(hits8.doc(l), photosLocation);
                    String tag9 = getTag(hits9.doc(l), photosLocation);
                    String tag10 = getTag(hits10.doc(l), photosLocation);
                    String tag11 = getTag(hits11.doc(l), photosLocation);
                    String tag12 = getTag(hits12.doc(l), photosLocation);

                    //  System.out.println(tag1);


                    //  if (tag2count.get(tag) == null) tag2count.put(tag, 1);
                    //  else tag2count.put(tag, tag2count.get(tag) + 1);

                    //Simple combination

                    tag2count.put(tag1, tag2count.get(tag1) + 1);
                    tag2count.put(tag2, tag2count.get(tag2) + 1);
                    tag2count.put(tag3, tag2count.get(tag3) + 1);
                    tag2count.put(tag4, tag2count.get(tag4) + 1);
                    tag2count.put(tag5, tag2count.get(tag5) + 1);
                    tag2count.put(tag6, tag2count.get(tag6) + 1);
                    tag2count.put(tag7, tag2count.get(tag7) + 1);
                    tag2count.put(tag8, tag2count.get(tag8) + 1);
                    tag2count.put(tag9, tag2count.get(tag9) + 1);
                    tag2count.put(tag10, tag2count.get(tag10) + 1);
                    tag2count.put(tag11, tag2count.get(tag11) + 1);
                    tag2count.put(tag12, tag2count.get(tag12) + 1);


                    if (weightByRank) {
                        //only if rank weight used
                        tag2weight.put(tag1, (double) l);
                        tag2weight.put(tag2, (double) l);
                        tag2weight.put(tag3, (double) l);
                        tag2weight.put(tag4, (double) l);
                        tag2weight.put(tag5, (double) l);
                        tag2weight.put(tag6, (double) l);
                        tag2weight.put(tag7, (double) l);
                        tag2weight.put(tag8, (double) l);
                        tag2weight.put(tag9, (double) l);
                        tag2weight.put(tag10, (double) l);
                        tag2weight.put(tag11, (double) l);
                        tag2weight.put(tag12, (double) l);

                    }
                    //  else {
                    //      tag2weight.put(tag, (double) l + tag2weight.get(tag));
                    //}
                    //SCORE
                    //     else {
                    // System.out.println(hits.score(l));
                    //  System.out.println(hitsSecond.score(l));
                    //         if (tag2weight.get(tag) == null) {
                    //           if (Double.valueOf(hits.score(l)) > Double.valueOf(hitsSecond.score(l)) && Double.valueOf(hits.score(l)) > Double.valueOf(hitsThird.score(l)))
                    //              tag2weight.put(tag, Double.valueOf(hits.score(l)));
                    //          else if (Double.valueOf(hitsSecond.score(l)) > Double.valueOf(hits.score(l)) && Double.valueOf(hitsSecond.score(l)) > Double.valueOf(hitsThird.score(l)))
                    //             tag2weight.put(tagSecond, Double.valueOf(hitsSecond.score(l)));
                    //              else if (Double.valueOf(hitsThird.score(l)) > Double.valueOf(hits.score(l)) && Double.valueOf(hitsThird.score(l)) > Double.valueOf(hitsSecond.score(l)))
                    //                   tag2weight.put(tagThird, Double.valueOf(hitsThird.score(l)));
                    //          } else {

                    //             if (Double.valueOf(hits.score(l)) > Double.valueOf(hitsSecond.score(l)) && Double.valueOf(hits.score(l)) > Double.valueOf(hitsThird.score(l)))
                    //                 tag2weight.put(tag, (double) l + hits.score(l));
                    //            else if (Double.valueOf(hitsSecond.score(l)) > Double.valueOf(hits.score(l)) && Double.valueOf(hitsSecond.score(l)) > Double.valueOf(hitsThird.score(l)))
                    //               tag2weight.put(tagSecond, (double) l + hitsSecond.score(l));
                    //          else if (Double.valueOf(hitsThird.score(l)) > Double.valueOf(hits.score(l)) && Double.valueOf(hitsThird.score(l)) > Double.valueOf(hitsSecond.score(l)))
                    //            tag2weight.put(tagThird, Double.valueOf(hitsThird.score(l)));

                    //      }
                    // }
                }
                // find class, iterate over the tags (classes):
                int maxCount = 0, maxima = 0;
                String classifiedAs = null;
                for (Iterator<String> tagIterator = tag2count.keySet().iterator(); tagIterator.hasNext(); ) {
                    String tag = tagIterator.next();
                    if (tag2count.get(tag) > maxCount) {
                        maxCount = tag2count.get(tag);
                        maxima = 1;
                        classifiedAs = tag;
                    } else if (tag2count.get(tag) == maxCount) {
                        maxima++;
                    }
                }
                // if there are two or more classes with the same number of results, then we take a look at the weights.
                // else the class is alread given in classifiedAs.
                if (maxima > 1) {
                    double minWeight = Double.MAX_VALUE;
                    for (Iterator<String> tagIterator = tag2count.keySet().iterator(); tagIterator.hasNext(); ) {
                        String tag = tagIterator.next();
                        if (tag2weight.get(tag) < minWeight) {
                            minWeight = tag2weight.get(tag);
                            classifiedAs = tag;
                        }
                    }
                }
//                    if (tag2.equals(tag3)) tag1 = tag2;
                count++;
                //SHOW THE CLASSIFICATION
                //     System.out.println(classifiedAs+";"+line);
                classesHTML.add(classifiedAs);
                filesHTML.add(line);

                //F1 Metric
                if (classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("yes")) {
                    countCorrect++;
                    countTp++;
                } else if (!classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("yes"))
                    countFp++;

                if (classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("no")) {
                    countCorrect++;
                    countTn++;
                } else if (!classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("no"))
                    countFn++;
                //if (classifiedAs.equals(getTagLine(line)))countCorrect++;
                //if (classifiedAs.equals(classIdentifier)) countCorrect++;
                //if (classifiedAs.equals(classIdentifier)) countCorrect++;
                // confusion:
                //   confusion[class2id.get(classifiedAs)]++;
//                    System.out.printf("%10s (%4.3f, %10d, %4d)\n", classifiedAs, ((double) countCorrect / (double) count), count, (System.currentTimeMillis() - ms) / count);
            } catch (Exception e) {
                System.err.println(">>> ERR:" + e.getMessage() + e);
                // throw (NullPointerException) e;
            }
        }

        double precisicon = getPrecision(countTp, countFp);
        double recall = getRecall(countTp, countFn);
        double trueNegativeRate = getTrueNegativeRate(countTn, countFp);
        double accuracy = getAccuracy(countTp, countFp, countTn, countFn);
        double fMeasure = getFmeasure(precisicon, recall);
        double falsePositiveRate = getFalsePositiveRate(countFp, countTn);
//            System.out.println("Results for class " + classIdentifier);
        // System.out.printf("Class\tPrecision\tRecall\tTrue Negative Rate\tAccuracy\tF-Measure\tCount Test Images\tCount Corret\tms per test\n");
        // System.out.printf("%s\t%4.5f\t%4.5f\t%4.5f\t%4.5f\t%4.5f\t%10d\t%10d\t%4d\n", classIdentifier, precisicon, recall, trueNegativeRate,accuracy, fMeasure,  count, countCorrect, (System.currentTimeMillis() - ms) / count);

        //   System.out.println(y + 1 + " of " + class1List.size() + " finished. " + (System.currentTimeMillis() - ms) / 1000 + " seconds per round. " + "Feature: " + class1List.get(y) + " " + class2List.get(y) + " " + class3List.get(y) + " Current y: " + y);

        print_line.printf("%s;%s;%s;%s;%s;%s;%4.5f;%4.5f;%4.5f;%4.5f;%4.5f;%4.5f;%10d;%10d;%4d;%4.5f;%4.5f;%4.5f;%4.5f\n", "all", "all", "all", k, weightByRank, classIdentifier, precisicon, recall, trueNegativeRate, accuracy, falsePositiveRate, fMeasure, count, countCorrect, (System.currentTimeMillis() - ms) / count, countTp, countFp, countTn, countFn);
        print_line.flush();

//        System.out.printf("Confusion\t");
//            for (int i = 0; i < classes.length; i++) {
//                System.out.printf("%s\t", classes[i]);
//            }
//            System.out.println();
//        for (int i = 0; i < classes.length; i++) {
        //           System.out.printf("%d\t", confusion[i]);
//        }
        //   System.out.println();

        //Create HTML
        if (createHTML == true) {

            String fileName = "classifieresults-" + System.currentTimeMillis() / 1000 + ".html";
            BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
            bw.write("<html>\n" +
                    "<head><title>Classification Results</title></head>\n" +
                    "<body bgcolor=\"#FFFFFF\">\n");
            bw.write("<table>");

            // int elems = Math.min(filesHTML.size(),50);
            int elems = filesHTML.size();

            for (int i = 0; i < elems; i++) {
                if (i % 3 == 0) bw.write("<tr>");

                String s = filesHTML.get(i);
                String colorF = "rgb(0, 255, 0)";

                if (classesHTML.get(i).equals("no"))
                    colorF = "rgb(255, 0, 0)";
                //  String s = reader.document(topDocs.scoreDocs[i].doc).get("descriptorImageIdentifier");
                //  System.out.println(reader.document(topDocs.scoreDocs[i].doc).get("featLumLay"));
                //  s = new File(s).getAbsolutePath();
                // System.out.println(s);
                bw.write("<td><a href=\"" + s + "\"><img style=\"max-width:220px;border:medium solid " + colorF + ";\"src=\"" + s + "\" border=\"" + 5 + "\" style=\"border: 3px\n" +
                        "black solid;\"></a></td>\n");
                if (i % 3 == 2) bw.write("</tr>");
            }
            if (elems % 3 != 0) {
                if (elems % 3 == 2) {
                    bw.write("<td>-</td with exit code 0\nd>\n");
                    bw.write("<td>-</td>\n");
                } else if (elems % 3 == 2) {
                    bw.write("<td>-</td>\n");
                }
                bw.write("</tr>");
            }

            bw.write("</table></body>\n" +
                    "</html>");
            bw.close();
        }
        //   } // kfor
//        }

        print_line.close();
    }

    public static boolean testClassifyNCombinedFeaturesMulti(int start, int end, String storeToFile, int numberOfNeighbours, String indexLocation, String photosLocation, String testSetFile, String searchedClass, String[] fieldsArray, String[] classArray  ) throws IOException {

        PrintWriter print_line = new PrintWriter(new BufferedWriter(new FileWriter(storeToFile)));

        //  String[] fieldsArray = {"CEDD", "EdgeHistogram", "FCTH", "ColorLayout", "PHOG", "JCD", "Gabor", "JpegCoeffs", "Tamura", "Luminance_Layout", "Opponent_Histogram", "ScalableColor"};
        //  String[] classArray = {"CEDD", "EdgeHistogram", "FCTH", "ColorLayout", "PHOG", "JCD", "Gabor", "JpegCoefficientHistogram", "Tamura", "LuminanceLayout", "OpponentHistogram", "ScalableColor"};

        //get the features for the column names
        String sCombinedFeatures = "";
        for (int i = 0; i < fieldsArray.length; i++) {
            sCombinedFeatures =   sCombinedFeatures + "Feature" + i + ";";
        }
        print_line.print(sCombinedFeatures + "K=;Weight Rank=;Class;Precision;Recall;True Negative Rate;Accuracy;False Positive Rate;F-Measure;Count Test Images;Count Correct;ms per test;TP;FP;TN;FN");
        print_line.println();
        print_line.flush();

        ArrayList<String> fields1List = new ArrayList<String>();
        ArrayList<String> fields2List = new ArrayList<String>();
        ArrayList<String> fields3List = new ArrayList<String>();
        ArrayList<String> fields4List = new ArrayList<String>();
        ArrayList<String> fields5List = new ArrayList<String>();
        ArrayList<String> fields6List = new ArrayList<String>();
        ArrayList<String> fields7List = new ArrayList<String>();
        ArrayList<String> fields8List = new ArrayList<String>();
        ArrayList<String> fields9List = new ArrayList<String>();
        ArrayList<String> fields10List = new ArrayList<String>();
        ArrayList<String> fields11List = new ArrayList<String>();
        ArrayList<String> fields12List = new ArrayList<String>();
        ArrayList<String> class1List = new ArrayList<String>();
        ArrayList<String> class2List = new ArrayList<String>();
        ArrayList<String> class3List = new ArrayList<String>();
        ArrayList<String> class4List = new ArrayList<String>();
        ArrayList<String> class5List = new ArrayList<String>();
        ArrayList<String> class6List = new ArrayList<String>();
        ArrayList<String> class7List = new ArrayList<String>();
        ArrayList<String> class8List = new ArrayList<String>();
        ArrayList<String> class9List = new ArrayList<String>();
        ArrayList<String> class10List = new ArrayList<String>();
        ArrayList<String> class11List = new ArrayList<String>();
        ArrayList<String> class12List = new ArrayList<String>();



        for (int i1 = 0; i1 < fieldsArray.length; i1++) {
            //   System.out.println(fieldsArray[g]);

            for (int i2 = i1 + 1; i2 < fieldsArray.length; i2++) {
                for (int i3 = i2 + 1; i3 < fieldsArray.length; i3++) {

                    for (int i4 = i3 + 1; i4 < fieldsArray.length; i4++) {

                        for (int i5 = i4 + 1; i5 < fieldsArray.length; i5++) {

                            for (int i6 = i5 + 1; i6 < fieldsArray.length; i6++) {

                                for (int i7 = i6 + 1; i7 < fieldsArray.length; i7++) {

                                    for (int i8 = i7 + 1; i8 < fieldsArray.length; i8++) {

                                        for (int i9 = i8 + 1; i9 < fieldsArray.length; i9++) {

                                            for (int i10 = i9 + 1; i10 < fieldsArray.length; i10++) {

                                                for (int i11 = i10 + 1; i11 < fieldsArray.length; i11++) {

                                                    for (int i12 = i11 + 1; i12 < fieldsArray.length; i12++) {


                                                        fields1List.add(fieldsArray[i1]);
                                                        fields2List.add(fieldsArray[i2]);
                                                        fields3List.add(fieldsArray[i3]);
                                                        fields4List.add(fieldsArray[i4]);
                                                        fields5List.add(fieldsArray[i5]);
                                                        fields6List.add(fieldsArray[i6]);
                                                        fields7List.add(fieldsArray[i7]);
                                                        fields8List.add(fieldsArray[i8]);
                                                        fields9List.add(fieldsArray[i9]);
                                                        fields10List.add(fieldsArray[i10]);
                                                        fields11List.add(fieldsArray[i11]);
                                                        fields12List.add(fieldsArray[i12]);

                                                        class1List.add(classArray[i1]);
                                                        class2List.add(classArray[i2]);
                                                        class3List.add(classArray[i3]);
                                                        class4List.add(classArray[i4]);
                                                        class5List.add(classArray[i5]);
                                                        class6List.add(classArray[i6]);
                                                        class7List.add(classArray[i7]);
                                                        class8List.add(classArray[i8]);
                                                        class9List.add(classArray[i9]);
                                                        class10List.add(classArray[i10]);
                                                        class11List.add(classArray[i11]);
                                                        class12List.add(classArray[i12]);


                                                    }

                                                }

                                            }

                                        }

                                    }

                                }

                            }

                        }

                    }

                }
            }

        }

        for (int y = start; y < end; y++) {

            String fS1 = class1List.get(y);
            String fS2 = class2List.get(y);
            String fS3 = class3List.get(y);

            String fN1 = "FIELD_NAME_" + fields1List.get(y).toUpperCase();
            String fN2 = "FIELD_NAME_" + fields2List.get(y).toUpperCase();
            String fN3 = "FIELD_NAME_" + fields3List.get(y).toUpperCase();

            boolean weightByRank = true;
            boolean createHTML = false;
            String[] classes = {"yes", "no"};
            int k = numberOfNeighbours;
            //  String indexLocation = sIndexLocation;
            //  String photosLocation = sPhotosLocation;
            //Testset
            //String listFiles = "location of test set file";
            // CONFIG

            String f1 = null;
            String f2 = null;
            String f3 = null;
            try {
                f1 = (String) DocumentBuilder.class.getField(fN1).get(null);
                f2 = (String) DocumentBuilder.class.getField(fN2).get(null);
                f3 = (String) DocumentBuilder.class.getField(fN3).get(null);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            LireFeature lf1 = null;
            LireFeature lf2 = null;
            LireFeature lf3 = null;
            try {
                lf1 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + fS1).newInstance();
                lf2 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + fS2).newInstance();
                lf3 = (LireFeature) Class.forName("net.semanticmetadata.lire.imageanalysis." + fS3).newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (ClassNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            String i1 = indexLocation + fS1;
            String i2 = indexLocation + fS2;
            String i3 = indexLocation + fS3;

            //boolean weightByRank = true;
            //String[] classes = {"yes", "no"};
            //int k = 70;
            // CONFIG
            //String f1 = DocumentBuilder.FIELD_NAME_EDGEHISTOGRAM;
            //String f2 = DocumentBuilder.FIELD_NAME_CEDD;
            //String f3 = DocumentBuilder.FIELD_NAME_PHOG;
            //LireFeature lf1 = new EdgeHistogram();
            //LireFeature lf2 = new CEDD();
            //LireFeature lf3 = new PHOG();
            //String i1 = "D:\\Datasets\\FashionTestFashionDataSet\\idx\\indexEdgeHistogram";
            //String i2 = "D:\\Datasets\\FashionTestFashionDataSet\\idx\\indexCEDD";
            //String i3 = "D:\\Datasets\\FashionTestFashionDataSet\\idx\\indexPHOG";

            //  for (int ik = 0;ik<k;ik++)       {

            //System.out.println("Tests for lf1 " + f1 + " with k=" + k + " combined with " + f2 + " - weighting by rank sum: " + weightByRank);
            //System.out.println("========================================");
            HashMap<String, Integer> tag2count = new HashMap<String, Integer>(k);
            HashMap<String, Double> tag2weight = new HashMap<String, Double>(k);
            int c = 0;   // used for just one class ...
            //        for (int c = 0; c < 10; c++) {
            if (searchedClass.equals("yes"))
                c = 0;
            else if(searchedClass.equals("no"))
                c = 1;

            String classIdentifier = classes[c];

            //"D:\\Datasets\\FashionTest\\fashion10000Test\\" + classIdentifier + ".txt";

            // INIT
            ArrayList<String> classesHTML = new ArrayList<String>();
            ArrayList<String> filesHTML = new ArrayList<String>();


            int[] confusion = new int[2];
            Arrays.fill(confusion, 0);
            HashMap<String, Integer> class2id = new HashMap<String, Integer>(2);
            for (int i = 0; i < classes.length; i++)
                class2id.put(classes[i], i);

            BufferedReader br = new BufferedReader(new FileReader(testSetFile));
            String line;

            IndexReader ir1 = DirectoryReader.open(MMapDirectory.open(new File(i1)));
            IndexReader ir2 = DirectoryReader.open(MMapDirectory.open(new File(i2)));
            IndexReader ir3 = DirectoryReader.open(MMapDirectory.open(new File(i3)));
            // in-memory linear search
            ImageSearcher bis1 = new GenericFastImageSearcher(k, lf1.getClass(), f1, true, ir1);
            ImageSearcher bis2 = new GenericFastImageSearcher(k, lf2.getClass(), f2, true, ir2);
            ImageSearcher bis3 = new GenericFastImageSearcher(k, lf3.getClass(), f3, true, ir3);
            // hashing based searcher
            //BitSamplingImageSearcher bis1 = new BitSamplingImageSearcher(k, f1, f1 + "_hash", lf1, 3000);
            ImageSearchHits hits1;
            ImageSearchHits hits2;
            ImageSearchHits hits3;

            int count = 0, countCorrect = 0;
            double countTp = 0, countFp = 0, countTn = 0, countFn = 0;      //F1 Metric
            long ms = System.currentTimeMillis();
            while ((line = br.readLine()) != null) {
                try {
                    tag2count.clear();
                    tag2weight.clear();
                    tag2count.put("yes",1);
                    tag2count.put("no",1);
                    tag2weight.put("yes",1.0);
                    tag2weight.put("no",1.0);

                    hits1 = bis1.search(ImageIO.read(new File(line)), ir1);
                    hits2 = bis2.search(ImageIO.read(new File(line)), ir2);
                    hits3 = bis3.search(ImageIO.read(new File(line)), ir3);
                    //Print the tag of both searches
                    //System.out.println(getTag(hits1.doc(0)) + "\n" + getTag(hits2.doc(0)));

                    // set tag weights and counts.
                    for (int l = 0; l < k; l++) {
                        String tag = getTag(hits1.doc(l), photosLocation);
                        String tagSecond = getTag(hits2.doc(l), photosLocation);
                        String tagThird = getTag(hits3.doc(l), photosLocation);

                        //Simple combination

                        tag2count.put(tag, tag2count.get(tag) + 1);
                        tag2count.put(tagSecond, tag2count.get(tagSecond) + 1);
                        tag2count.put(tagThird, tag2count.get(tagThird) + 1);

                        if (weightByRank) {
                            //only if rank weight used
                            if (tag2weight.get(tag) == null && tag2weight.get(tagSecond) == null && tag2weight.get(tagThird) == null) {
                                tag2weight.put(tag, (double) l);
                                tag2weight.put(tagSecond, (double) l);
                                tag2weight.put(tagThird, (double) l);
                            } else if (tag2weight.get(tag) != null && tag2weight.get(tagSecond) != null && tag2weight.get(tagThird) != null) {
                                tag2weight.put(tag, (double) l + tag2weight.get(tag));
                                tag2weight.put(tagSecond, (double) l + tag2weight.get(tagSecond));
                                tag2weight.put(tagThird, (double) l + tag2weight.get(tagThird));
                            } else if (tag2weight.get(tag) != null && tag2weight.get(tagSecond) != null && tag2weight.get(tagThird) == null) {
                                tag2weight.put(tag, (double) l + tag2weight.get(tag));
                                tag2weight.put(tagSecond, (double) l + tag2weight.get(tagSecond));
                                tag2weight.put(tagThird, (double) l);
                            } else if (tag2count.get(tag) != null && tag2count.get(tagSecond) == null && tag2weight.get(tagThird) != null) {
                                tag2weight.put(tag, (double) l + tag2weight.get(tag));
                                tag2weight.put(tagSecond, (double) l);
                                tag2weight.put(tagThird, (double) l + tag2weight.get(tagThird));
                            } else if (tag2count.get(tag) == null && tag2count.get(tagSecond) != null && tag2weight.get(tagThird) != null) {
                                tag2weight.put(tag, (double) l);
                                tag2weight.put(tagSecond, (double) l + tag2weight.get(tagSecond));
                                tag2weight.put(tagThird, (double) l + tag2weight.get(tagThird));
                            }
                            //  else {
                            //      tag2weight.put(tag, (double) l + tag2weight.get(tag));
                            //}
                        } else {
                            // System.out.println(hits1.score(l));
                            //  System.out.println(hits2.score(l));
                            if (tag2weight.get(tag) == null) {
                                if (Double.valueOf(hits1.score(l)) > Double.valueOf(hits2.score(l)) && Double.valueOf(hits1.score(l)) > Double.valueOf(hits3.score(l)))
                                    tag2weight.put(tag, Double.valueOf(hits1.score(l)));
                                else if (Double.valueOf(hits2.score(l)) > Double.valueOf(hits1.score(l)) && Double.valueOf(hits2.score(l)) > Double.valueOf(hits3.score(l)))
                                    tag2weight.put(tagSecond, Double.valueOf(hits2.score(l)));
                                else if (Double.valueOf(hits3.score(l)) > Double.valueOf(hits1.score(l)) && Double.valueOf(hits3.score(l)) > Double.valueOf(hits2.score(l)))
                                    tag2weight.put(tagThird, Double.valueOf(hits3.score(l)));
                            } else {

                                if (Double.valueOf(hits1.score(l)) > Double.valueOf(hits2.score(l)) && Double.valueOf(hits1.score(l)) > Double.valueOf(hits3.score(l)))
                                    tag2weight.put(tag, (double) l + hits1.score(l));
                                else if (Double.valueOf(hits2.score(l)) > Double.valueOf(hits1.score(l)) && Double.valueOf(hits2.score(l)) > Double.valueOf(hits3.score(l)))
                                    tag2weight.put(tagSecond, (double) l + hits2.score(l));
                                else if (Double.valueOf(hits3.score(l)) > Double.valueOf(hits1.score(l)) && Double.valueOf(hits3.score(l)) > Double.valueOf(hits2.score(l)))
                                    tag2weight.put(tagThird, Double.valueOf(hits3.score(l)));

                            }
                        }
                    }
                    // find class, iterate over the tags (classes):
                    int maxCount = 0, maxima = 0;
                    String classifiedAs = null;
                    for (Iterator<String> tagIterator = tag2count.keySet().iterator(); tagIterator.hasNext(); ) {
                        String tag = tagIterator.next();
                        if (tag2count.get(tag) > maxCount) {
                            maxCount = tag2count.get(tag);
                            maxima = 1;
                            classifiedAs = tag;
                        } else if (tag2count.get(tag) == maxCount) {
                            maxima++;
                        }
                    }
                    // if there are two or more classes with the same number of results, then we take a look at the weights.
                    // else the class is alread given in classifiedAs.
                    if (maxima > 1) {
                        double minWeight = Double.MAX_VALUE;
                        for (Iterator<String> tagIterator = tag2count.keySet().iterator(); tagIterator.hasNext(); ) {
                            String tag = tagIterator.next();
                            if (tag2weight.get(tag) < minWeight) {
                                minWeight = tag2weight.get(tag);
                                classifiedAs = tag;
                            }
                        }
                    }
//                    if (tag2.equals(tag3)) tag1 = tag2;
                    count++;
                    //SHOW THE CLASSIFICATION
                    //     System.out.println(classifiedAs+";"+line);
                    classesHTML.add(classifiedAs);
                    filesHTML.add(line);

                    //F1 Metric
                    if (classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("yes")) {
                        countCorrect++;
                        countTp++;
                    } else if (!classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("yes"))
                        countFp++;

                    if (classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("no")) {
                        countCorrect++;
                        countTn++;
                    } else if (!classifiedAs.equals(getTagLine(line, photosLocation)) && classifiedAs.equals("no"))
                        countFn++;
                    //if (classifiedAs.equals(getTagLine(line)))countCorrect++;
                    //if (classifiedAs.equals(classIdentifier)) countCorrect++;
                    //if (classifiedAs.equals(classIdentifier)) countCorrect++;
                    // confusion:
                    confusion[class2id.get(classifiedAs)]++;
//                    System.out.printf("%10s (%4.3f, %10d, %4d)\n", classifiedAs, ((double) countCorrect / (double) count), count, (System.currentTimeMillis() - ms) / count);
                } catch (Exception e) {
                    System.err.println(">>> ERR:" + e.getMessage() + e);
                    //   throw (NullPointerException) e;
                }
            }

            double precisicon = getPrecision(countTp, countFp);
            double recall = getRecall(countTp, countFn);
            double trueNegativeRate = getTrueNegativeRate(countTn, countFp);
            double accuracy = getAccuracy(countTp, countFp, countTn, countFn);
            double fMeasure = getFmeasure(precisicon, recall);
            double falsePositiveRate = getFalsePositiveRate(countFp, countTn);
//            System.out.println("Results for class " + classIdentifier);
            // System.out.printf("Class\tPrecision\tRecall\tTrue Negative Rate\tAccuracy\tF-Measure\tCount Test Images\tCount Corret\tms per test\n");
            // System.out.printf("%s\t%4.5f\t%4.5f\t%4.5f\t%4.5f\t%4.5f\t%10d\t%10d\t%4d\n", classIdentifier, precisicon, recall, trueNegativeRate,accuracy, fMeasure,  count, countCorrect, (System.currentTimeMillis() - ms) / count);

            System.out.println(y + 1 + " of " + class1List.size() + " finished. " + (System.currentTimeMillis() - ms) / 1000 + " seconds per round. " + "Feature: " + class1List.get(y) + " " + class2List.get(y) + " " + class3List.get(y) + " Current y: " + y);

            print_line.printf("%s;%s;%s;%s;%s;%s;%4.5f;%4.5f;%4.5f;%4.5f;%4.5f;%4.5f;%10d;%10d;%4d;%4.5f;%4.5f;%4.5f;%4.5f\n", class1List.get(y), class2List.get(y), class3List.get(y), k, weightByRank, classIdentifier, precisicon, recall, trueNegativeRate, accuracy, falsePositiveRate, fMeasure, count, countCorrect, (System.currentTimeMillis() - ms) / count, countTp, countFp, countTn, countFn);
            print_line.flush();

//        System.out.printf("Confusion\t");
//            for (int i = 0; i < classes.length; i++) {
//                System.out.printf("%s\t", classes[i]);
//            }
//            System.out.println();
//        for (int i = 0; i < classes.length; i++) {
            //           System.out.printf("%d\t", confusion[i]);
//        }
            //   System.out.println();

            //Create HTML
            if (createHTML == true) {

                String fileName = "classifieresults-" + System.currentTimeMillis() / 1000 + ".html";
                BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
                bw.write("<html>\n" +
                        "<head><title>Classification Results</title></head>\n" +
                        "<body bgcolor=\"#FFFFFF\">\n");
                bw.write("<table>");

                // int elems = Math.min(filesHTML.size(),50);
                int elems = filesHTML.size();

                for (int i = 0; i < elems; i++) {
                    if (i % 3 == 0) bw.write("<tr>");

                    String s = filesHTML.get(i);
                    String colorF = "rgb(0, 255, 0)";

                    if (classesHTML.get(i).equals("no"))
                        colorF = "rgb(255, 0, 0)";
                    //  String s = reader.document(topDocs.scoreDocs[i].doc).get("descriptorImageIdentifier");
                    //  System.out.println(reader.document(topDocs.scoreDocs[i].doc).get("featLumLay"));
                    //  s = new File(s).getAbsolutePath();
                    // System.out.println(s);
                    bw.write("<td><a href=\"" + s + "\"><img style=\"max-width:220px;border:medium solid " + colorF + ";\"src=\"" + s + "\" border=\"" + 5 + "\" style=\"border: 3px\n" +
                            "black solid;\"></a></td>\n");
                    if (i % 3 == 2) bw.write("</tr>");
                }
                if (elems % 3 != 0) {
                    if (elems % 3 == 2) {
                        bw.write("<td>-</td with exit code 0\nd>\n");
                        bw.write("<td>-</td>\n");
                    } else if (elems % 3 == 2) {
                        bw.write("<td>-</td>\n");
                    }
                    bw.write("</tr>");
                }

                bw.write("</table></body>\n" +
                        "</html>");
                bw.close();
            }
            //   } // kfor
//        }
        }
        print_line.close();
        return true;
    }

    private static String getTag(Document d, String photosLocation) {
        StringBuilder ab = new StringBuilder(d.getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0].replace(photosLocation, ""));

        //    System.out.println(ab.substring(0, ab.indexOf("\\")).toString());
        return ab.substring(0, ab.indexOf("\\")).toString();
        //  return ab.toString();
        //return "yes";
    }



    private static double getPrecision(double tp, double fp) {
        double precision;
        return precision = tp / (tp + fp);
    }

    private static double getRecall(double tp, double fn) {
        double recall;
        return recall = tp / (tp + fn);
    }

    private static double getTrueNegativeRate(double tn, double fp) {
        double negativeRate;
        return negativeRate = tn / (tn + fp);
    }

    private static double getAccuracy(double tp, double fp, double tn, double fn) {
        double accuracy;
        return accuracy = (tp + tn) / (tp + tn + fp + fn);
    }

    private static double getFalsePositiveRate(double fp, double tn) {
        double falsePositiveRate;
        return falsePositiveRate = fp / (fp + tn);
    }

    private static double getFmeasure(double precision, double recall) {
        double fMeasure;
        return fMeasure = 2 * ((precision * recall) / (precision + recall));
    }

    private static String getTagLine(String line, String photosLocation) {
        line = line.replace(photosLocation, "");
        //  System.out.println(line.substring(0, line.indexOf("\\")).toString());
        return line.substring(0, line.indexOf("\\")).toString();
        //return "yes";
    }

     //  public static void main(String[] args) {
    public void testThreadClassifyThreeFeatures() throws IOException {
        Thread[] all = new Thread[4];
        all[0] = new Thread(new Classifie3Task(0, 56, "D:\\resultsTripleFeatureItemK31.txt"));
        all[1] = new Thread(new Classifie3Task(56, 111, "D:\\resultsTripleFeatureItemK32.txt"));
        all[2] = new Thread(new Classifie3Task(111, 166, "D:\\resultsTripleFeatureItemK33.txt"));
        all[3] = new Thread(new Classifie3Task(166, 220, "D:\\resultsTripleFeatureItemK34.txt"));

        all[0].start();
        all[1].start();
        all[2].start();
        all[3].start();

        try {
            all[0].join();
            all[1].join();
            all[2].join();
            all[3].join();
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    //Thread test 3 features
    public static class Classifie3Task implements Runnable {

        int start;
        int end;
        String storeToFile;

        public Classifie3Task(int start, int end, String storeToFile) {
            this.start = start;
            this.end = end;
            this.storeToFile = storeToFile;
        }

        public void run() {
            // for (int i=0; i < end; i++) {
            // do something
            try {
                testClassifyFashionThreeCombinedFeaturesMulti(start, end, storeToFile);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            //  }
        }
    }

    public void testClassifyNFeatures() throws IOException {

        String indexLocation = "D:\\Datasets\\FashionTestItemDataSet\\idx\\index";
        String photosLocation = "D:\\Datasets\\FashionTestItemDataSet\\";
        //Testset
        String listFiles = "D:\\Datasets\\FashionTestItemDataSet\\itemtest.txt";

     //     String[] fieldsArray = {"CEDD", "EdgeHistogram", "FCTH", "ColorLayout", "PHOG", "JCD", "Gabor", "JpegCoeffs", "Tamura", "Luminance_Layout", "Opponent_Histogram", "ScalableColor"};
   //       String[] classArray = {"CEDD", "EdgeHistogram", "FCTH", "ColorLayout", "PHOG", "JCD", "Gabor", "JpegCoefficientHistogram", "Tamura", "LuminanceLayout", "OpponentHistogram", "ScalableColor"};

        String[] fieldsArray = {"CEDD", "EdgeHistogram", "FCTH"};
        String[] classArray = {"CEDD", "EdgeHistogram", "FCTH"};

        testClassifyNCombinedFeaturesMulti(0,220,"D:\\resultsTripleFeatureItemK31.txt",3,"D:\\Datasets\\FashionTestItemDataSet\\idx\\index","D:\\Datasets\\FashionTestItemDataSet\\","D:\\Datasets\\FashionTestItemDataSet\\itemtest.txt","yes",fieldsArray,classArray);
    }


}
