package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.tr.util.ListenerManager;
import de.uniluebeck.itm.tr.util.ListenerManagerImpl;

public class WSNDeviceAppModule extends AbstractModule {

	@Override
	protected void configure() {

		bind(new TypeLiteral<ListenerManager<WSNDeviceAppConnector.NodeOutputListener>>() {
		}
		).to(new TypeLiteral<ListenerManagerImpl<WSNDeviceAppConnector.NodeOutputListener>>() {
		}
		);

		install(new FactoryModuleBuilder()
				.implement(WSNDeviceApp.class, WSNDeviceAppImpl.class)
				.build(WSNDeviceAppGuiceFactory.class)
		);

		install(new FactoryModuleBuilder()
				.implement(WSNDeviceAppConnector.class, WSNDeviceAppConnectorImpl.class)
				.build(WSNDeviceAppConnectorFactory.class)
		);
	}
}
