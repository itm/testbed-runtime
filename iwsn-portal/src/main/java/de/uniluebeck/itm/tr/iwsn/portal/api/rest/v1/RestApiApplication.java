package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers.*;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.*;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.serializers.NodeUrnDeserializer;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.serializers.NodeUrnPrefixDeserializer;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.serializers.NodeUrnPrefixSerializer;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.serializers.NodeUrnSerializer;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;

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
		return newHashSet(
				experimentResource,
				remoteExperimentConfigurationResource,
				rsResource,
				snaaResource,
				cookieResource,
				testbedsResource,
				createJsonProvider(),
				createJaxbElementProvider(),
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
				new RuntimeExceptionMapper(),
				new SMUnknownSecretReservationKeyFaultExceptionMapper(),
				new WebServiceExceptionMapper()
		);
	}

	/**
	 * Customized JAXB serialization provider to enable formatted pretty-print by default.
	 *
	 * @return a JAXB serialization provider
	 */
	private JAXBElementProvider createJaxbElementProvider() {
		final JAXBElementProvider jaxbElementProvider = new JAXBElementProvider();
		final HashMap<Object, Object> marshallerProperties = newHashMap();
		marshallerProperties.put("jaxb.formatted.output", true);
		jaxbElementProvider.setMarshallerProperties(marshallerProperties);
		return jaxbElementProvider;
	}

	/**
	 * Customized Jackson provider for JSON serialization.
	 *
	 * @return a serialization provider for JSON
	 */
	private JacksonJsonProvider createJsonProvider() {
		final JacksonJsonProvider jsonProvider = new JacksonJsonProvider();
		final ObjectMapper objectMapper = new ObjectMapper();
		final SimpleModule module = new SimpleModule(
				"TR REST API Custom Serialization Module",
				new Version(1, 0, 0, null)
		);
		module.addSerializer(NodeUrnPrefix.class, new NodeUrnPrefixSerializer());
		module.addDeserializer(NodeUrnPrefix.class, new NodeUrnPrefixDeserializer());
		module.addSerializer(NodeUrn.class, new NodeUrnSerializer());
		module.addDeserializer(NodeUrn.class, new NodeUrnDeserializer());
		objectMapper.registerModule(module);
		jsonProvider.setMapper(objectMapper);
		return jsonProvider;
	}
}