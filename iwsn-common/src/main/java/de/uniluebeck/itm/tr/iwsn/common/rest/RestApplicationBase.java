package de.uniluebeck.itm.tr.iwsn.common.rest;

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

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

public abstract class RestApplicationBase extends Application {

	@Override
	public Set<Object> getSingletons() {
		final Set<Object> objects = newHashSet(
				createJsonProvider(),
				createJaxbElementProvider(),
				new DateTimeParameterHandler(),
				new NodeUrnParameterHandler(),
				new NodeUrnPrefixParameterHandler()
		);
		objects.addAll(getSingletonsInternal());
		return objects;
	}

	public abstract Set<?> getSingletonsInternal();

	/**
	 * Customized JAXB serialization provider to enable formatted pretty-print by default.
	 *
	 * @return a JAXB serialization provider
	 */
	protected JAXBElementProvider createJaxbElementProvider() {
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
	protected JacksonJsonProvider createJsonProvider() {
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
