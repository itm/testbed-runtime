package de.uniluebeck.itm.tr.devicedb;

import de.uniluebeck.itm.tr.util.Logging;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class TestClient {

	static {
		Logging.setLoggingDefaults();
	}

	public static void main(String[] args) {


	}

	private static DeviceDBRestResource client() {

		final JSONProvider jsonProvider = new JSONProvider();
		jsonProvider.setDropRootElement(true);
		jsonProvider.setSupportUnwrapped(true);
		jsonProvider.setDropCollectionWrapperElement(true);
		jsonProvider.setSerializeAsArray(true);

		final List<JSONProvider> providers = newArrayList(jsonProvider);

		return JAXRSClientFactory.create("http://localhost:7654/rest/", DeviceDBRestResource.class, providers);
	}

}
