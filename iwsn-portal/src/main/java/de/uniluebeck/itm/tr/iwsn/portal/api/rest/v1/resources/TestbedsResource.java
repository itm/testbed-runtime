package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.TestbedDescription;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@Path("/testbeds/")
public class TestbedsResource {

	private final CommonConfig commonConfig;

	private final PortalServerConfig portalServerConfig;

	@Context
	private UriInfo uriInfo;

	@Inject
	public TestbedsResource(final CommonConfig commonConfig, final PortalServerConfig portalServerConfig) {
		this.commonConfig = commonConfig;
		this.portalServerConfig = portalServerConfig;
	}

	@GET
	@Produces({MediaType.APPLICATION_JSON})
	public List<TestbedDescription> getTestbedList() {

		final URI baseUri = uriInfo.getBaseUri();

		final TestbedDescription testbed = new TestbedDescription();
		testbed.name = portalServerConfig.getWiseguiTestbedName();
		testbed.testbedBaseUri =
				baseUri.getScheme() + "://" + baseUri.getHost() + ":" + baseUri.getPort() + "/rest/v1.0";
		testbed.sessionManagementEndpointUrl =
				baseUri.getScheme() + "://" + baseUri.getHost() + ":" + baseUri.getPort() + "/soap/v3.0/sm";
		testbed.urnPrefixes = newArrayList(commonConfig.getUrnPrefix().toString());

		return newArrayList(testbed);
	}
}