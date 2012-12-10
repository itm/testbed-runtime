package eu.wisebed.shibboauth;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;
import org.slf4j.LoggerFactory;

public class GzipDecompressDecoder extends HttpEntityWrapper {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GzipDecompressDecoder.class);
    
    public GzipDecompressDecoder(final HttpEntity entity) {
        super(entity);
        log.debug("New instance for GZIP decoding");
    }

    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
        log.debug("Adding GZIP decoding wrapper");
        InputStream wrappedInputStream = wrappedEntity.getContent();
        return new GZIPInputStream(wrappedInputStream);
    }

    @Override
    public long getContentLength() {
        // The length of the decompressed content is unknown
        return -1;
    }

}
