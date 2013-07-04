package de.uniluebeck.itm.tr.federator.utils;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.Endpoint;
import java.net.URI;

import static com.google.common.base.Preconditions.checkState;

public class WebservicePublisher<T> extends AbstractService implements Service {

	private static final Logger log = LoggerFactory.getLogger(WebservicePublisher.class);

	private final URI endpointUrl;

	private T implementer;

	private Endpoint endpoint;

	public WebservicePublisher(final URI endpointUrl) {
		this.endpointUrl = endpointUrl;
	}

	public T getImplementer() {
		return implementer;
	}

	public void setImplementer(final T implementer) {
		this.implementer = implementer;
	}

	@Override
	protected void doStart() {

		try {

			checkState(implementer != null, "Implementer must be set before calling start()!");

			if (log.isInfoEnabled()) {
				log.info("Started {} endpoint using endpoint URL {}", implementer.getClass().getSimpleName(),
						endpointUrl
				);
			}

			endpoint = Endpoint.publish(endpointUrl.toString(), implementer);

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		try {

			if (endpoint != null) {
				if (endpoint.isPublished()) {
					endpoint.stop();
				}
				endpoint = null;
			}

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	public URI getEndpointUrl() {
		return endpointUrl;
	}
}
