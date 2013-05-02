package de.uniluebeck.itm.tr.rs;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.matcher.Matchers;
import com.google.inject.util.Providers;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.servicepublisher.ServicePublisherConfig;
import de.uniluebeck.itm.servicepublisher.ServicePublisherFactory;
import de.uniluebeck.itm.servicepublisher.cxf.ServicePublisherCxfModule;
import de.uniluebeck.itm.tr.rs.persistence.gcal.GCalRSPersistenceModule;
import de.uniluebeck.itm.tr.rs.persistence.inmemory.InMemoryRSPersistenceModule;
import de.uniluebeck.itm.tr.rs.persistence.jpa.RSPersistenceJPAModule;
import de.uniluebeck.itm.tr.rs.singleurnprefix.ServedNodeUrnsProvider;
import de.uniluebeck.itm.tr.rs.singleurnprefix.SingleUrnPrefixRS;
import de.uniluebeck.itm.tr.rs.singleurnprefix.SingleUrnPrefixSoapRS;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.rs.RS;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.snaa.SNAA;

import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.util.concurrent.MoreExecutors.getExitingExecutorService;
import static com.google.inject.matcher.Matchers.annotatedWith;
import static eu.wisebed.api.v3.WisebedServiceHelper.getSNAAService;
import static eu.wisebed.api.v3.WisebedServiceHelper.getSessionManagementService;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class RSModule extends AbstractModule {

	private final RSConfig config;

	public RSModule(final RSConfig config) {
		this.config = config;
	}

	@Override
	protected void configure() {

		switch (config.persistence) {
			case GCAL:
				install(new GCalRSPersistenceModule(config.persistenceConfig));
				break;
			case IN_MEMORY:
				install(new InMemoryRSPersistenceModule());
				break;
			case JPA:
				install(new RSPersistenceJPAModule(config.persistenceConfig));
				break;
		}

		bind(SNAA.class).toInstance(getSNAAService(config.snaaEndpointUrl.toString()));

		if (config.smEndpointUrl == null) {

			bind(SessionManagement.class).toProvider(Providers.<SessionManagement>of(null));
			bind(NodeUrn[].class).toProvider(Providers.<NodeUrn[]>of(null));

		} else {

			bind(SessionManagement.class).toInstance(getSessionManagementService(config.smEndpointUrl.toString()));
			bind(NodeUrn[].class).toProvider(ServedNodeUrnsProvider.class);
		}


		bind(RSService.class).to(SingleUrnPrefixSoapRS.class);
		bind(RS.class).to(SingleUrnPrefixRS.class);

		bindInterceptor(
				Matchers.any(),
				annotatedWith(AuthorizationRequired.class),
				new RSAuthorizationInterceptor(getSNAAService(config.snaaEndpointUrl.toString()))
		);

		install(new ServicePublisherCxfModule());
	}

	@Provides
	TimeLimiter provideTimeLimiter() {
		return new SimpleTimeLimiter(getExitingExecutorService((ThreadPoolExecutor) newCachedThreadPool()));
	}

	@Provides
	ServicePublisher provideServicePublisher(final ServicePublisherFactory servicePublisherFactory) {
		return servicePublisherFactory.create(new ServicePublisherConfig(config.port));
	}
}
