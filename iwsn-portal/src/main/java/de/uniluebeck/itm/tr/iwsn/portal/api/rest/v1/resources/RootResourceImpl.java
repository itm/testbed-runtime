package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.common.Constants;
import de.uniluebeck.itm.tr.common.EndpointManager;
import de.uniluebeck.itm.tr.common.ServedNodeUrnPrefixesProvider;
import de.uniluebeck.itm.tr.iwsn.portal.WiseGuiServiceConfig;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.RestApiModule;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.TestbedDescription;
import eu.wisebed.api.v3.common.NodeUrnPrefix;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

@Path("/")
public class RootResourceImpl implements RootResource {

	private final EndpointManager endpointManager;

	private final boolean federator;

	private final String apiVersion;

	private final String appName;

	private final String appVersion;

	private final String appBuild;

	private final String appBranch;

	private final ServedNodeUrnPrefixesProvider servedNodeUrnPrefixesProvider;

	private final WiseGuiServiceConfig wiseGuiServiceConfig;

	@Inject
	public RootResourceImpl(@Named(RestApiModule.IS_FEDERATOR) final boolean federator,
							@Named(RestApiModule.API_VERSION) final String apiVersion,
							@Named(Constants.APP_NAME_KEY) final String appName,
							@Named(Constants.APP_VERSION_KEY) final String appVersion,
							@Named(Constants.APP_BUILD_KEY) final String appBuild,
							@Named(Constants.APP_BRANCH_KEY) final String appBranch,
							final ServedNodeUrnPrefixesProvider servedNodeUrnPrefixesProvider,
							final WiseGuiServiceConfig wiseGuiServiceConfig,
							final EndpointManager endpointManager) {
		this.federator = federator;
		this.apiVersion = apiVersion;
		this.appName = appName;
		this.appVersion = appVersion;
		this.appBuild = appBuild;
		this.appBranch = appBranch;
		this.servedNodeUrnPrefixesProvider = servedNodeUrnPrefixesProvider;
		this.wiseGuiServiceConfig = checkNotNull(wiseGuiServiceConfig);
		this.endpointManager = checkNotNull(endpointManager);
	}

	@Override
	@GET
	@Produces({MediaType.APPLICATION_JSON})
	@Path("testbedDescription")
	public TestbedDescription getTestbedDescription() {

		final TestbedDescription desc = new TestbedDescription();

		desc.name = wiseGuiServiceConfig.getWiseguiTestbedName();
		desc.sessionManagementEndpointUrl = endpointManager.getSmEndpointUri().toString();
		desc.urnPrefixes = newArrayList(transform(servedNodeUrnPrefixesProvider.get(), NodeUrnPrefix::toString));
		desc.isFederator = federator;
		desc.apiVersion = apiVersion;
		desc.appName = appName;
		desc.appVersion = appVersion;
		desc.appBuild = appBuild;
		desc.appBranch = appBranch;

		return desc;
	}
}