package de.uniluebeck.itm.tr.federator.rs;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.rs.RS;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newCachedThreadPool;

public class RSFederatorModule extends AbstractModule {

	private final RSFederatorConfig config;

	public RSFederatorModule(final RSFederatorConfig config) {
		this.config = config;
	}

	@Override
	protected void configure() {
		bind(RSFederatorService.class).to(RSFederatorServiceImpl.class);
		install(new ServicePublisherCxfModule());
	}

	@Provides
	ServicePublisher provideServicePublisher(final ServicePublisherFactory servicePublisherFactory) {
		return servicePublisherFactory.create(new ServicePublisherConfig(config.port));
	}

	@Provides
	public ExecutorService provideExecutorService() {
		return newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("RSFederatorService-Thread %d").build());
	}

	@Provides
	public FederationManager<RS> provideRsFederationManager() {

		final Function<URI, RS> uriToRSEndpointFunction = new Function<URI, RS>() {
			@Override
			public RS apply(final URI uri) {
				return WisebedServiceHelper.getRSService(uri.toString());
			}
		};

		final ImmutableMap.Builder<URI, ImmutableSet<NodeUrnPrefix>> mapBuilder = ImmutableMap.builder();
		for (Map.Entry<URI, Set<NodeUrnPrefix>> entry : config.federates.entrySet()) {
			mapBuilder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
		}

		return new FederationManager<RS>(
				uriToRSEndpointFunction,
				mapBuilder.build()
		);
	}
}
