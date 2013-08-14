package net.semanticmetadata.lire;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface TestImage {

    /**
     * @return The name of this image, a friendly name to denote what it is
     */
    public String name();

    /**
     * @return The location of this image
     */
    public URL url();

    /**
     * @return The image as a buffered image
     * @throws IOException
     */
    public BufferedImage image() throws IOException;

    /**
     * @return This image as a raw I/O stream
     * @throws IOException
     */
    public InputStream stream() throws IOException;

}
