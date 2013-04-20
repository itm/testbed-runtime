package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers.*;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.*;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import javax.ws.rs.core.Application;
import java.util.HashMap;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

public class RestApiApplication extends Application {

	private final ExperimentResource experimentResource;

	private final RemoteExperimentConfigurationResource remoteExperimentConfigurationResource;

	private final RsResource rsResource;

	private final SnaaResource snaaResource;

	private final CookieResource cookieResource;

	private final TestbedsResource testbedsResource;

	@Inject
	public RestApiApplication(final ExperimentResource experimentResource,
							  final RemoteExperimentConfigurationResource remoteExperimentConfigurationResource,
							  final RsResource rsResource,
							  final SnaaResource snaaResource,
							  final CookieResource cookieResource, final TestbedsResource testbedsResource) {
		this.experimentResource = checkNotNull(experimentResource);
		this.remoteExperimentConfigurationResource = checkNotNull(remoteExperimentConfigurationResource);
		this.rsResource = checkNotNull(rsResource);
		this.snaaResource = checkNotNull(snaaResource);
		this.cookieResource = checkNotNull(cookieResource);
		this.testbedsResource = checkNotNull(testbedsResource);
	}

	@Override
	public Set<Object> getSingletons() {

		final JacksonJsonProvider jsonProvider = new JacksonJsonProvider();
		final JAXBElementProvider jaxbElementProvider = new JAXBElementProvider();
		final HashMap<Object, Object> marshallerProperties = newHashMap();
		marshallerProperties.put("jaxb.formatted.output", true);
		jaxbElementProvider.setMarshallerProperties(marshallerProperties);

		return newHashSet(
				experimentResource,
				remoteExperimentConfigurationResource,
				rsResource,
				snaaResource,
				cookieResource,
				testbedsResource,
				jsonProvider,
				jaxbElementProvider,
				new DateTimeParameterHandler(),
				new NodeUrnParameterHandler(),
				new NodeUrnPrefixParameterHandler(),
				new Base64ExceptionMapper(),
				new SNAAFaultExceptionMapper(),
				new RSFaultExceptionMapper(),
				new AuthenticationFaultExceptionMapper(),
				new AuthorizationFaultExceptionMapper(),
				new ReservationConflictFaultExceptionMapper(),
				new RSUnknownSecretReservationKeyFaultExceptionMapper(),
				new SMUnknownSecretReservationKeyFaultExceptionMapper()
		);
	}
}