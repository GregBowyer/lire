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
 * Updated: 11.07.13 10:40
 */

package net.semanticmetadata.lire.imageanalysis.spatialpyramid;

import net.semanticmetadata.lire.imageanalysis.CEDD;
import net.semanticmetadata.lire.imageanalysis.LireFeature;
import net.semanticmetadata.lire.utils.MetricsUtils;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * This file is part of LIRE, a Java library for content based image retrieval.
 *
 * @author Mathias Lux, mathias@juggle.at, 19.05.13
 */

public class SPCEDD implements LireFeature {
    int histogramSize = 144 * 5 + 144 * 4 * 4;
    double[] histogram = new double[histogramSize];

    // Temp:
    int tmp;

    @Override
    public void extract(BufferedImage bimg) {
        // level 0:
        CEDD cedd = new CEDD();
        cedd.extract(bimg);
        System.arraycopy(cedd.getDoubleHistogram(), 0, histogram, 0, 144);
        // level 1:
        int w = bimg.getWidth() / 2;
        int h = bimg.getHeight() / 2;
        cedd.extract(bimg.getSubimage(0, 0, w, h));
        System.arraycopy(cedd.getDoubleHistogram(), 0, histogram, 144 * 1, 144);
        cedd.extract(bimg.getSubimage(w, 0, w, h));
        System.arraycopy(cedd.getDoubleHistogram(), 0, histogram, 144 * 2, 144);
        cedd.extract(bimg.getSubimage(0, h, w, h));
        System.arraycopy(cedd.getDoubleHistogram(), 0, histogram, 144 * 3, 144);
        cedd.extract(bimg.getSubimage(w, h, w, h));
        System.arraycopy(cedd.getDoubleHistogram(), 0, histogram, 144 * 4, 144);
        // level 2:
        int wstep = bimg.getWidth() / 4;
        int hstep = bimg.getHeight() / 4;
        int binPos = 5; // the next free section in the histogram
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                cedd.extract(bimg.getSubimage(i * wstep, j * hstep, wstep, hstep));
                System.arraycopy(cedd.getDoubleHistogram(), 0, histogram, 144 * binPos, 144);
                binPos++;
            }
        }

    }

    /**
     * Provides a faster way of serialization.
     *
     * @return a byte array that can be read with the corresponding method.
     * @see SPCEDD#setByteArrayRepresentation(byte[])
     */
    public byte[] getByteArrayRepresentation() {
        // find out the position of the beginning of the trailing zeros.
        int position = -1;
        for (int i = 0; i < histogram.length; i++) {
            if (position == -1) {
                if (histogram[i] == 0) position = i;
            } else if (position > -1) {
                if (histogram[i] != 0) position = -1;
            }
        }
        if (position < 0) position = 143;
        // find out the actual length. two values in one byte, so we have to round up.
        int length = (position + 1) / 2;
        if ((position + 1) % 2 == 1) length = position / 2 + 1;
        byte[] result = new byte[length];
        for (int i = 0; i < result.length; i++) {
            tmp = ((int) (histogram[(i << 1)])) << 4;
            tmp = (tmp | ((int) (histogram[(i << 1) + 1])));
            result[i] = (byte) (tmp - 128);
        }
        return result;
    }

    /**
     * Reads descriptor from a byte array. Much faster than the String based method.
     *
     * @param in byte array from corresponding method
     * @see SPCEDD#getByteArrayRepresentation
     */
    public void setByteArrayRepresentation(byte[] in) {
        if ((in.length << 1) < histogram.length)
            Arrays.fill(histogram, in.length << 1, histogram.length - 1, 0);
        for (int i = 0; i < in.length; i++) {
            tmp = in[i] + 128;
            histogram[(i << 1) + 1] = ((double) (tmp & 0x000F));
            histogram[i << 1] = ((double) (tmp >> 4));
        }
    }

    public void setByteArrayRepresentation(byte[] in, int offset, int length) {
        if ((length << 1) < histogram.length)
            Arrays.fill(histogram, length << 1, histogram.length - 1, 0);
        for (int i = offset; i < offset + length; i++) {
            tmp = in[i] + 128;
            histogram[((i - offset) << 1) + 1] = ((double) (tmp & 0x000F));
            histogram[(i - offset) << 1] = ((double) (tmp >> 4));
        }
    }

    @Override
    public double[] getDoubleHistogram() {
        return histogram;
    }

    @Override
    public float getDistance(LireFeature feature) {
        if (!(feature instanceof SPCEDD)) return -1;
        return (float) MetricsUtils.tanimoto(histogram, feature.getDoubleHistogram());
    }

    @Override
    public String getStringRepresentation() {
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public void setStringRepresentation(String s) {
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public String getFeatureName() {
        return "CEDD Spatial Pyramid";
    }

    @Override
    public String getFieldName() {
        return "f_spcedd";
    }
}
