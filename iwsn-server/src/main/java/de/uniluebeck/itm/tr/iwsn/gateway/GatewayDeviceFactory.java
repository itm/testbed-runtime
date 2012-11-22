package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;

import javax.annotation.Nonnull;

public interface GatewayDeviceFactory {

	GatewayDevice create(@Nonnull final GatewayDeviceConfiguration configuration,
								 @Nonnull final DeviceFactory deviceFactory,
								 @Nonnull final EventBus deviceObserverEventBus,
								 @Nonnull final AsyncEventBus deviceObserverAsyncEventBus);

}
