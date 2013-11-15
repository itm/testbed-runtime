package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.common.EndpointManager;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.iwsn.portal.WiseGuiServiceConfig;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.RestApiModule;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.TestbedDescription;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.common.NodeUrnPrefixHelper.NODE_URN_PREFIX_TO_STRING;

@Path("/")
public class RootResourceImpl implements RootResource {

	private final EndpointManager endpointManager;

	private final boolean federator;

	private final ServedNodeUrnPrefixesProvider servedNodeUrnPrefixesProvider;

	private final WiseGuiServiceConfig wiseGuiServiceConfig;

	@Inject
	public RootResourceImpl(@Named(RestApiModule.IS_FEDERATOR) final boolean federator,
							final ServedNodeUrnPrefixesProvider servedNodeUrnPrefixesProvider,
							final WiseGuiServiceConfig wiseGuiServiceConfig,
							final EndpointManager endpointManager) {
		this.federator = federator;
		this.servedNodeUrnPrefixesProvider = servedNodeUrnPrefixesProvider;
		this.wiseGuiServiceConfig = checkNotNull(wiseGuiServiceConfig);
		this.endpointManager = checkNotNull(endpointManager);
	}

	@Override
	@GET
	@Produces({MediaType.APPLICATION_JSON})
	@Path("testbedDescription")
	public TestbedDescription getTestbedDescription() {

		final TestbedDescription testbed = new TestbedDescription();

		testbed.name = wiseGuiServiceConfig.getWiseguiTestbedName();
		testbed.sessionManagementEndpointUrl = endpointManager.getSmEndpointUri().toString();
		testbed.urnPrefixes = newArrayList(transform(servedNodeUrnPrefixesProvider.get(), NODE_URN_PREFIX_TO_STRING));
		testbed.isFederator = federator;

		return testbed;
	}
}