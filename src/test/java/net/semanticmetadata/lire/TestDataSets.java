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
package net.semanticmetadata.lire;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

/**
 * There are a lot of test images that become present within the lire test suite
 * some of these are provided with lire, whilst others may be found on the internet
 * at various locations.
 *
 * To make the test suite easier to manage and more flexible the following methods
 * provide functions to enable easier access to test images
 *
 * @author Greg Bowyer
 */
public class TestDataSets {

    public static ImmutableList<? extends TestImage> basicTestImages() {
        return ImmutableList.of(
            new ClasspathTestImage("img01", "images/img01.JPG"),
            new ClasspathTestImage("img02", "images/img02.JPG"),
            new ClasspathTestImage("img03", "images/img03.JPG"),
            new ClasspathTestImage("img04", "images/img04.JPG"),
            new ClasspathTestImage("img05", "images/img05.JPG"),
            new ClasspathTestImage("img06", "images/img05.JPG"),
            new ClasspathTestImage("img07", "images/img05.JPG"),
            new ClasspathTestImage("img08", "images/img05.JPG")
        );
    }

    /**
     * Implementation of test image that is able to provide test images from the classpath,
     * this is used for test images that are directly included in Lire and as a result do
     * not need any special treatment (such as downloading a training set, construction etc)
     */
    static class ClasspathTestImage implements TestImage {

        private final String name;
        private final URL url;

        public ClasspathTestImage(String name, String classpathLocation) {
            this.name = name;
            this.url = Resources.getResource(classpathLocation);
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public URL url() {
            return this.url;
        }

        @Override
        public BufferedImage image() throws IOException {
            return ImageIO.read(this.url);
        }

        @Override
        public InputStream stream() throws IOException {
            return this.url.openStream();
        }
    }

}
