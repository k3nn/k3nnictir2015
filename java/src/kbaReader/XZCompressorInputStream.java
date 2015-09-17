package kbaReader;

import java.io.IOException;
import java.io.InputStream;
import org.tukaani.xz.XZ;
import org.tukaani.xz.SingleXZInputStream;
import org.tukaani.xz.XZInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;

/**
 * XZ decompressor, wraps tukaani's xz codec.
 */

public class XZCompressorInputStream extends CompressorInputStream {
    private final InputStream inputStream;

    /**
     * Checks if the signature matches what is expected for a .xz file.
     *
     * @param   signature     the bytes to check
     * @param   length        the number of bytes to check
     * @return  true if signature matches the .xz magic bytes, false otherwise
     */
    public static boolean matches(byte[] signature, int length) {
        if (length < XZ.HEADER_MAGIC.length) {
            return false;
        }

        for (int i = 0; i < XZ.HEADER_MAGIC.length; ++i) {
            if (signature[i] != XZ.HEADER_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a new input stream that decompresses XZ-compressed data
     * from the specified input stream. This doesn't support
     * concatenated .xz files.
     *
     * @param       inputStream where to read the compressed data
     *
     * @throws      IOException if the input is not in the .xz format,
     *                          the input is corrupt or truncated, the .xz
     *                          headers specify options that are not supported
     *                          by this implementation, or the underlying
     *                          <code>inputStream</code> throws an exception
     */
    public XZCompressorInputStream(InputStream inputStream) throws IOException {
        this(inputStream, false);
    }

    /**
     * Creates a new input stream that decompresses XZ-compressed data
     * from the specified input stream.
     *
     * @param       inputStream where to read the compressed data
     * @param       decompressConcatenated
     *                          if true, decompress until the end of the
     *                          input; if false, stop after the first .xz
     *                          stream and leave the input position to point
     *                          to the next byte after the .xz stream
     *
     * @throws      IOException if the input is not in the .xz format,
     *                          the input is corrupt or truncated, the .xz
     *                          headers specify options that are not supported
     *                          by this implementation, or the underlying
     *                          <code>inputStream</code> throws an exception
     */
    public XZCompressorInputStream(InputStream inputStream,
                                   boolean decompressConcatenated)
            throws IOException {
        if (decompressConcatenated) {
            this.inputStream = new XZInputStream(inputStream);
        } else {
            this.inputStream = new SingleXZInputStream(inputStream);
        }
    }

    @Override
    public int read() throws IOException {
        int byteRead = inputStream.read();
        count(byteRead == -1 ? -1 : 1);
        return byteRead;
    }

    @Override
    public int read(byte[] bytebuffer, int bufferOffset, int maxReadLength) throws IOException {
        int bytesRead = inputStream.read(bytebuffer, bufferOffset, maxReadLength);
        count(bytesRead);
        return bytesRead;
    }

    @Override
    public long skip(long byteLength) throws IOException {
        return inputStream.skip(byteLength);
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}