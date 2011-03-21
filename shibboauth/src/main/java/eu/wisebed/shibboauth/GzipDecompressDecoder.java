package eu.wisebed.shibboauth;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

public class GzipDecompressDecoder extends HttpEntityWrapper {

    public GzipDecompressDecoder(final HttpEntity entity) {
        super(entity);
    }

    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
        InputStream wrappedInputStream = wrappedEntity.getContent();
        return new GZIPInputStream(wrappedInputStream);
    }

    @Override
    public long getContentLength() {
        // The length of the decompressed content is unknown
        return -1;
    }

}
