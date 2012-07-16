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
 * --------------------
 * (c) 2002-2011 by Mathias Lux (mathias@juggle.at)
 *     http://www.semanticmetadata.net/lire
 */

package net.semanticmetadata.lire.imageanalysis.cedd;

public class RGB2HSV {

    public int[] ApplyFilter(int red, int green, int blue) {
        int[] Results = new int[3];
        int HSV_H = 0;
        int HSV_S = 0;
        int HSV_V = 0;

        double MaxHSV = (Math.max(red, Math.max(green, blue)));
        double MinHSV = (Math.min(red, Math.min(green, blue)));

        HSV_V = (int) (MaxHSV);

        HSV_S = 0;
        if (MaxHSV != 0) HSV_S = (int) (255 - 255 * (MinHSV / MaxHSV));

        if (MaxHSV != MinHSV) {

            int IntegerMaxHSV = (int) (MaxHSV);

            if (IntegerMaxHSV == red && green >= blue) {
                HSV_H = (int) (60 * (green - blue) / (MaxHSV - MinHSV));
            } else if (IntegerMaxHSV == red && green < blue) {
                HSV_H = (int) (359 + 60 * (green - blue) / (MaxHSV - MinHSV));
            } else if (IntegerMaxHSV == green) {
                HSV_H = (int) (119 + 60 * (blue - red) / (MaxHSV - MinHSV));
            } else if (IntegerMaxHSV == blue) {
                HSV_H = (int) (239 + 60 * (red - green) / (MaxHSV - MinHSV));
            }


        } else HSV_H = 0;

        Results[0] = HSV_H;
        Results[1] = HSV_S;
        Results[2] = HSV_V;

        return (Results);
    }
}
