package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Inject;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.core.Application;
import java.util.HashMap;

import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

public class DeviceDBRestApplication extends Application {

	private final DeviceDBRestResource resource;

	private final DeviceDBRestAdminResource adminResource;

	@Inject
	public DeviceDBRestApplication(final DeviceDBRestResource resource, final DeviceDBRestAdminResource adminResource) {
		this.resource = resource;
		this.adminResource = adminResource;
	}

	@Override
	public Set<Object> getSingletons() {
		return newHashSet(resource, adminResource, createJaxbElementProvider(), createJsonProvider());
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
		jsonProvider.setMapper(new ObjectMapper());
		return jsonProvider;
	}
}
