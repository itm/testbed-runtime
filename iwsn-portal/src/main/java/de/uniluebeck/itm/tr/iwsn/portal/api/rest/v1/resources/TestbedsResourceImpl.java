package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.EndpointManager;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.iwsn.portal.WiseGuiServiceConfig;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.TestbedDescription;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.common.NodeUrnPrefixHelper.NODE_URN_PREFIX_TO_STRING;

@Path("/testbeds/")
public class TestbedsResourceImpl implements TestbedsResource {

	private final EndpointManager endpointManager;

	private final ServedNodeUrnPrefixesProvider servedNodeUrnPrefixesProvider;

	private final WiseGuiServiceConfig wiseGuiServiceConfig;

	@Inject
	public TestbedsResourceImpl(final ServedNodeUrnPrefixesProvider servedNodeUrnPrefixesProvider,
								final WiseGuiServiceConfig wiseGuiServiceConfig,
								final EndpointManager endpointManager) {
		this.servedNodeUrnPrefixesProvider = servedNodeUrnPrefixesProvider;
		this.wiseGuiServiceConfig = checkNotNull(wiseGuiServiceConfig);
		this.endpointManager = checkNotNull(endpointManager);
	}

	@Override
	@GET
	@Produces({MediaType.APPLICATION_JSON})
	public List<TestbedDescription> getTestbedList() {

		final TestbedDescription testbed = new TestbedDescription();

		testbed.name = wiseGuiServiceConfig.getWiseguiTestbedName();
		testbed.testbedBaseUri = wiseGuiServiceConfig.getWiseGuiRestApiBaseUri();
		testbed.sessionManagementEndpointUrl = endpointManager.getSmEndpointUri().toString();
		testbed.urnPrefixes = newArrayList(transform(servedNodeUrnPrefixesProvider.get(), NODE_URN_PREFIX_TO_STRING));

		return newArrayList(testbed);
	}
}