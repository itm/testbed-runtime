package de.uniluebeck.itm.tr.federator.snaa;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.federatorutils.FederationManager;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethSNAA;
import de.uniluebeck.itm.tr.snaa.shibboleth.ShibbolethSNAAModule;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.snaa.SNAA;

import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static de.uniluebeck.itm.util.propconf.PropConfBuilder.buildConfig;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class SNAAFederatorModule extends AbstractModule {

	private final Properties properties;

	public SNAAFederatorModule(final Properties properties) {
		this.properties = properties;
	}

	@Override
	protected void configure() {

		final CommonConfig commonConfig = buildConfig(CommonConfig.class, properties);
		final SNAAServiceConfig snaaServiceConfig = buildConfig(SNAAServiceConfig.class, properties);
		final SNAAFederatorConfig snaaFederatorConfig = buildConfig(SNAAFederatorConfig.class, properties);

		bind(CommonConfig.class).toInstance(commonConfig);
		bind(SNAAServiceConfig.class).toInstance(snaaServiceConfig);
		bind(SNAAFederatorConfig.class).toInstance(snaaFederatorConfig);

		install(new ServicePublisherCxfModule());

		switch (snaaFederatorConfig.getSnaaFederatorType()) {

			case API:
				bind(SNAAFederatorService.class).to(SNAAFederatorServiceImpl.class);
				break;
			case SHIBBOLETH:
				install(new ShibbolethSNAAModule(commonConfig, snaaServiceConfig));
				bind(SNAAFederatorService.class)
						.to(DelegatingSNAAFederatorServiceImpl.class)
						.in(Scopes.SINGLETON);
				bind(SNAAService.class)
						.annotatedWith(Names.named("authorizationSnaa"))
						.to(SNAAFederatorServiceImpl.class);
				bind(SNAAService.class)
						.annotatedWith(Names.named("authenticationSnaa"))
						.to(ShibbolethSNAA.class);
				break;
			default:
				throw new RuntimeException("Unknown SNAA federator type: " + snaaFederatorConfig.getSnaaFederatorType());
		}
	}

	@Provides
	ServicePublisher provideServicePublisher(final ServicePublisherFactory servicePublisherFactory,
											 final CommonConfig commonConfig) {
		return servicePublisherFactory.create(new ServicePublisherConfig(commonConfig.getPort()));
	}

	@Provides
	public ExecutorService provideExecutorService() {
		return newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("SNAAFederatorService-Thread %d").build());
	}

	@Provides
	public FederationManager<SNAA> provideSnaaFederationManager(final SNAAFederatorConfig config) {

		final Function<URI, SNAA> uriToRSEndpointFunction = new Function<URI, SNAA>() {
			@Override
			public SNAA apply(final URI uri) {
				return WisebedServiceHelper.getSNAAService(uri.toString());
			}
		};

		final ImmutableMap.Builder<URI, ImmutableSet<NodeUrnPrefix>> mapBuilder = ImmutableMap.builder();
		for (Map.Entry<URI, Set<NodeUrnPrefix>> entry : config.getFederates().entrySet()) {
			mapBuilder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
		}

		return new FederationManager<SNAA>(uriToRSEndpointFunction, mapBuilder.build());
	}
}
