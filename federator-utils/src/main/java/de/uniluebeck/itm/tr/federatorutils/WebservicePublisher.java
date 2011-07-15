package de.uniluebeck.itm.tr.federatorutils;

import de.uniluebeck.itm.tr.util.Service;
import de.uniluebeck.itm.tr.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.Endpoint;
import java.net.URL;

import static com.google.common.base.Preconditions.checkState;

public class WebservicePublisher<T> implements Service {

	private static final Logger log = LoggerFactory.getLogger(WebservicePublisher.class);

	private final URL endpointUrl;

	private T implementer;

	private Endpoint endpoint;

	public WebservicePublisher(final URL endpointUrl) {
		this.endpointUrl = endpointUrl;
	}

	public T getImplementer() {
		return implementer;
	}

	public void setImplementer(final T implementer) {
		this.implementer = implementer;
	}

	@Override
	public void start() throws Exception {

		checkState(implementer != null, "Implementer must be set before calling start()!");

		String bindAllInterfacesUrl = UrlUtils.convertHostToZeros(endpointUrl.toString());

		if (log.isInfoEnabled()) {
			log.info("Started {} endpoint using endpoint URL {} and binding URL {}.",
					new Object[]{
							implementer.getClass().getSimpleName(),
							endpointUrl.toString(),
							bindAllInterfacesUrl
					}
			);
		}

		endpoint = Endpoint.publish(bindAllInterfacesUrl, implementer);
	}

	@Override
	public void stop() {
		if (endpoint != null) {
			if (endpoint.isPublished()) {
				endpoint.stop();
			}
			endpoint = null;
		}
	}

	public URL getEndpointUrl() {
		return endpointUrl;
	}
}
