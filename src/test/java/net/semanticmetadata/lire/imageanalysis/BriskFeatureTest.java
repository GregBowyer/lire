package net.semanticmetadata.lire.imageanalysis;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.google.common.collect.ImmutableList;
import junit.framework.TestCase;
import net.semanticmetadata.lire.LireTestCase;
import net.semanticmetadata.lire.TestDataSets;
import net.semanticmetadata.lire.TestImage;
import net.semanticmetadata.lire.impl.BriskDocumentBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

import com.googlecode.javacv.cpp.opencv_core.CvMat;
import org.junit.Test;

/**
 * Unit tests for BriskFeature and BriskDocumentBuilder.
 * 
 * @author Mario Taschwer
 * @version $Id$
 */
public class BriskFeatureTest extends LireTestCase
{
    private final ImmutableList<? extends TestImage> TEST_IMAGES = TestDataSets.basicTestImages();
    private final double epsilon = 0.01;  // BRISK features are integers

    @Test
    public void testCreateDescriptorFields() throws IOException
    {
        BriskDocumentBuilder briskBuilder = new BriskDocumentBuilder();
        BriskFeature briskFeature = new BriskFeature();
        for (TestImage testImg : TEST_IMAGES) {
            BufferedImage img = testImg.image();
            Field[] fields = briskBuilder.createDescriptorFields(img);
            CvMat desc = briskBuilder.getDescriptor();

            assertTrue(desc.rows() > 0);
            assertTrue(desc.cols() == 64);
            assertTrue(fields.length == desc.rows());

            System.out.format("%s: extracted %d BRISK feature vectors at %d key points\n", 
                    testImg, fields.length, briskBuilder.numKeyPoints());

            for (int i=0; i < fields.length; i++) {
                BytesRef bref = fields[i].binaryValue();
                briskFeature.setByteArrayRepresentation(bref.bytes, bref.offset, bref.length);
                for (int j=0; j < desc.cols(); j++) {
                    assertTrue(Math.abs(desc.get(i, j) - briskFeature.descriptor[j]) < epsilon);
                }
            }
        }
    }

    @Test
    public void testCreateDocument() throws IOException
    {
        BriskDocumentBuilder briskBuilder = new BriskDocumentBuilder();
        BriskFeature briskFeature = new BriskFeature();
        for (TestImage testImg : TEST_IMAGES) {
            BufferedImage img = testImg.image();
            Document doc = briskBuilder.createDocument(img, testImg.name());
            CvMat desc = briskBuilder.getDescriptor();

            assertTrue(desc.rows() > 0);
            assertTrue(desc.cols() == 64);

            IndexableField[] fields = doc.getFields(BriskDocumentBuilder.FIELD_NAME_BRISK);
            assertTrue(fields.length == desc.rows());

            System.out.format("%s: extracted %d BRISK feature vectors at %d key points\n", 
                    testImg, fields.length, briskBuilder.numKeyPoints());

            for (int i=0; i < fields.length; i++) {
                BytesRef bref = fields[i].binaryValue();
                briskFeature.setByteArrayRepresentation(bref.bytes, bref.offset, bref.length);
                for (int j=0; j < desc.cols(); j++) {
                    assertTrue(Math.abs(desc.get(i, j) - briskFeature.descriptor[j]) < epsilon);
                }
            }
        }
    }
}
