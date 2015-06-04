package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import de.uniluebeck.itm.tr.iwsn.messages.MessageFactory;

public class DeviceDBInMemoryModule extends PrivateModule {

	@Override
	protected void configure() {
		requireBinding(MessageFactory.class);
		bind(DeviceDBService.class).to(DeviceDBInMemory.class).in(Scopes.SINGLETON);
		expose(DeviceDBService.class);
	}
}
