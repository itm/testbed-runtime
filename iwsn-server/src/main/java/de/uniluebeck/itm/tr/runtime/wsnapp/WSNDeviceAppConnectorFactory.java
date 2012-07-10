package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import javax.annotation.Nonnull;

public interface WSNDeviceAppConnectorFactory {

	WSNDeviceAppConnector create(@Nonnull final WSNDeviceAppConnectorConfiguration configuration,
								 @Nonnull final EventBus deviceObserverEventBus,
								 @Nonnull final AsyncEventBus deviceObserverAsyncEventBus);

}
