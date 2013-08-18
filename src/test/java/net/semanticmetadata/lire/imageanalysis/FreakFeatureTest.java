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
import net.semanticmetadata.lire.impl.FreakDocumentBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

import com.googlecode.javacv.cpp.opencv_core.CvMat;
import org.junit.Ignore;
import org.junit.Test;

import static net.semanticmetadata.lire.TestDataSets.basicTestImages;

/**
 * Unit tests for FreakFeature and FreakDocumentBuilder.
 * 
 * @author Mario Taschwer
 * @version $Id$
 */
@Ignore("The feature distancing is broken")
public class FreakFeatureTest extends LireTestCase
{
    public static final ImmutableList<? extends TestImage> TEST_IMAGES = basicTestImages();
    private final double epsilon = 0.01;  // FREAK features are integers

    @Test
    public void testCreateDescriptorFields() throws IOException
    {
        FreakDocumentBuilder freakBuilder = new FreakDocumentBuilder();
        FreakFeature freakFeature = new FreakFeature();
        for (TestImage testImg : TEST_IMAGES) {
            BufferedImage img = testImg.image();
            Field[] fields = freakBuilder.createDescriptorFields(img);
            CvMat desc = freakBuilder.getDescriptor();

            assertTrue(desc.rows() > 0);
            assertTrue(desc.cols() == 64);
            assertTrue(fields.length == desc.rows());

            System.out.format("%s: extracted %d FREAK feature vectors, using %d FAST key points\n", 
            		testImg, fields.length, freakBuilder.numKeyPoints());

            for (int i=0; i < fields.length; i++) {
                BytesRef bref = fields[i].binaryValue();
                freakFeature.setByteArrayRepresentation(bref.bytes, bref.offset, bref.length);
                for (int j=0; j < desc.cols(); j++) {
                    assertTrue(Math.abs(desc.get(i, j) - freakFeature.descriptor[j]) < epsilon);
                }
            }
        }
    }

    @Test
    public void testCreateDocument() throws IOException
    {
        FreakDocumentBuilder freakBuilder = new FreakDocumentBuilder();
        FreakFeature freakFeature = new FreakFeature();
        for (TestImage testImg : TEST_IMAGES) {
            BufferedImage img = testImg.image();
            Document doc = freakBuilder.createDocument(img, testImg.name());
            IndexableField[] fields = doc.getFields(BriskDocumentBuilder.FIELD_NAME_FREAK);
            CvMat desc = freakBuilder.getDescriptor();

            assertTrue(desc.rows() > 0);
            assertTrue(desc.cols() == 64);
            assertTrue(fields.length == desc.rows());

            System.out.format("%s: extracted %d FREAK feature vectors, using %d FAST key points\n",
            		testImg, fields.length, freakBuilder.numKeyPoints());

            for (int i=0; i < fields.length; i++) {
                BytesRef bref = fields[i].binaryValue();
                freakFeature.setByteArrayRepresentation(bref.bytes, bref.offset, bref.length);
                for (int j=0; j < desc.cols(); j++) {
                    assertTrue(Math.abs(desc.get(i, j) - freakFeature.descriptor[j]) < epsilon);
                }
            }
        }
    }
}
