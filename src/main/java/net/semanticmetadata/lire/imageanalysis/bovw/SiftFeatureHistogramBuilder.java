/*
 * This file is part of the LIRe project: http://www.semanticmetadata.net/lire
 * LIRe is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * LIRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LIRe; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * We kindly ask you to refer the following paper in any publication mentioning Lire:
 *
 * Lux Mathias, Savvas A. Chatzichristofis. Lire: Lucene Image Retrieval –
 * An Extensible Java CBIR Library. In proceedings of the 16th ACM International
 * Conference on Multimedia, pp. 1085-1088, Vancouver, Canada, 2008
 *
 * http://doi.acm.org/10.1145/1459359.1459577
 *
 * Copyright statement:
 * ~~~~~~~~~~~~~~~~~~~~
 * (c) 2002-2011 by Mathias Lux (mathias@juggle.at)
 *     http://www.semanticmetadata.net/lire
 */

package net.semanticmetadata.lire.imageanalysis.bovw;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.LireFeature;
import net.semanticmetadata.lire.imageanalysis.sift.Feature;
import org.apache.lucene.index.IndexReader;

/**
 * Implementing the BoVW approach for SIFT features.
 *
 * @author Mathias Lux, mathias@juggle.at
 *         Date: 18.10.11
 *         Time: 11:15
 */
public class SiftFeatureHistogramBuilder extends LocalFeatureHistogramBuilder {
    public SiftFeatureHistogramBuilder(IndexReader reader) {
        super(reader);
        init();
    }

    public SiftFeatureHistogramBuilder(IndexReader reader, int numDocsForVocabulary) {
        super(reader, numDocsForVocabulary);
        init();
    }

    public SiftFeatureHistogramBuilder(IndexReader reader, int numDocsForVocabulary, int numClusters) {
        super(reader, numDocsForVocabulary, numClusters);
        init();
    }

    private void init() {
        localFeatureFieldName = DocumentBuilder.FIELD_NAME_SIFT;
        visualWordsFieldName = DocumentBuilder.FIELD_NAME_SIFT_VISUAL_WORDS;
        localFeatureHistFieldName = DocumentBuilder.FIELD_NAME_SIFT_LOCAL_FEATURE_HISTOGRAM;
        clusterFile = "./clusters-sift.dat";
    }

    @Override
    protected LireFeature getFeatureInstance() {
        return new Feature();
    }
}
