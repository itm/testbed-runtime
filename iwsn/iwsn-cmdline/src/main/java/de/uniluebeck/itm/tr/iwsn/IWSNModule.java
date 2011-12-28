package de.uniluebeck.itm.tr.iwsn;

import com.google.inject.AbstractModule;
import de.uniluebeck.itm.gtr.TestbedRuntimeModule;
import de.uniluebeck.itm.tr.util.domobserver.DOMObserverModule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public class IWSNModule extends AbstractModule {

	private final ExecutorService asyncEventBusExecutor;

	private final ScheduledExecutorService messageServerServiceScheduler;

	private final ScheduledExecutorService reliableMessagingServiceScheduler;

	public IWSNModule(final ExecutorService asyncEventBusExecutor,
					  final ScheduledExecutorService messageServerServiceScheduler,
					  final ScheduledExecutorService reliableMessagingServiceScheduler) {

		this.asyncEventBusExecutor = asyncEventBusExecutor;
		this.messageServerServiceScheduler = messageServerServiceScheduler;
		this.reliableMessagingServiceScheduler = reliableMessagingServiceScheduler;
	}

	@Override
	protected void configure() {
		install(new DOMObserverModule());
		install(
				new TestbedRuntimeModule(
						asyncEventBusExecutor,
						messageServerServiceScheduler,
						reliableMessagingServiceScheduler
				)
		);
		install(new IWSNOverlayManagerModule());
		install(new IWSNApplicationManagerModule());
		bind(IWSNFactory.class).to(IWSNFactoryImpl.class);
	}
}
