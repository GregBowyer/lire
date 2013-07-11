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
 * Updated: 11.07.13 09:58
 */

package net.semanticmetadata.lire.imageanalysis;


import java.awt.image.BufferedImage;

/**
 * This is the basic interface for all content based features. It is needed for GenericDocumentBuilder etc.
 * Date: 28.05.2008
 * Time: 14:44:16
 *
 * @author Mathias Lux, mathias@juggle.at
 */
public interface LireFeature {
    /**
     * Gives a descriptive name of the feature, i.e. a name to show up in benchmarks, menus, UIs, etc.
     * @return the name of the feature.
     */
    public String getFeatureName();

    /**
     * Returns the preferred field name for indexing.
     * @return the field name preferred for indexing in a Lucene index.
     */
    public String getFieldName();

    /**
     * Extracts the feature vector from a BufferedImage.
     * @param image the source image
     */
    public void extract(BufferedImage image);

    /**
     * Returns a compact byte[] based representation of the feature vector.
     * @return a compact byte[] array containing the feature vector.
     * @see net.semanticmetadata.lire.imageanalysis.LireFeature#setByteArrayRepresentation(byte[])
     */
    public byte[] getByteArrayRepresentation();

    /**
     * Sets the feature vector values based on the byte[] data. Use
     * {@link net.semanticmetadata.lire.imageanalysis.LireFeature#getByteArrayRepresentation()}
     * to generate a compatible byte[] array.
     * @param featureData the byte[] data.
     * @see net.semanticmetadata.lire.imageanalysis.LireFeature#getByteArrayRepresentation()
     */
    public void setByteArrayRepresentation(byte[] featureData);

    /**
     * Sets the feature vector values based on the byte[] data.
     * Use {@link net.semanticmetadata.lire.imageanalysis.LireFeature#getByteArrayRepresentation()}
     * to generate a compatible byte[] array.
     * @param featureData the byte[] array containing the data.
     * @param offset the offset, i.e. where the feature vector starts.
     * @param length the length of the data representing the feature vector.
     * @see net.semanticmetadata.lire.imageanalysis.LireFeature#getByteArrayRepresentation()
     */
    public void setByteArrayRepresentation(byte[] featureData, int offset, int length);

    /**
     * Convenience method to get the feature vector as double[] array. This method is used by
     * classifiers, etc.
     * @return the feature vector as a double[] array.
     */
    public double[] getDoubleHistogram();

    /**
     * The distance function for this type of feature
     * @param feature the feature vector to compare the current instance to.
     * @return the distance (or dissimilarity) between the instance and the parameter.
     */
    float getDistance(LireFeature feature);

    /**
     * Legacy method from a time, when feature vectors were stored as Strings in Lucene. This is not
     * recommended, as Strings are immutable and therefore a lot of uneccessary object instances are created.
     * @return the feature vector as String.
     */
    java.lang.String getStringRepresentation();

    /**
     * Legacy method from a time, when feature vectors were stored as Strings in Lucene. This is not
     * recommended, as Strings are immutable and therefore a lot of uneccessary object instances are created.
     * @param featureVector
     * @see net.semanticmetadata.lire.imageanalysis.LireFeature#getStringRepresentation()
     */
    void setStringRepresentation(java.lang.String featureVector);
}
