package de.uniluebeck.itm.tr.plugins.defaultimage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.tr.iwsn.common.EventBusService;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.rs.RSHelperModule;
import org.joda.time.Duration;

public class DefaultImagePluginModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new RSHelperModule());
		install(new FactoryModuleBuilder()
				.implement(NodeStatusTracker.class, NodeStatusTrackerImpl.class)
				.build(NodeStatusTrackerFactory.class)
		);
		bind(DefaultImagePlugin.class).to(DefaultImagePluginImpl.class).in(Scopes.SINGLETON);
	}

	@Provides
	@Singleton
	public NodeStatusTracker provideNodeStatusTracker(final NodeStatusTrackerFactory factory) {
		return factory.create(Duration.standardHours(1));
	}

	@Provides
	@Singleton
	public EventBusService provideEventBusService(final PortalEventBus portalEventBus) {
		return portalEventBus;
	}
}
