package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayDevice;
import de.uniluebeck.itm.tr.iwsn.gateway.GatewayDeviceFactory;
import de.uniluebeck.itm.tr.iwsn.gateway.WSNDeviceAppConnectorImpl;
import de.uniluebeck.itm.tr.util.ListenerManager;
import de.uniluebeck.itm.tr.util.ListenerManagerImpl;

public class WSNDeviceAppModule extends AbstractModule {

	@Override
	protected void configure() {

		bind(new TypeLiteral<ListenerManager<GatewayDevice.NodeOutputListener>>() {
		}
		).to(new TypeLiteral<ListenerManagerImpl<GatewayDevice.NodeOutputListener>>() {
		}
		);

		install(new FactoryModuleBuilder()
				.implement(WSNDeviceApp.class, WSNDeviceAppImpl.class)
				.build(WSNDeviceAppGuiceFactory.class)
		);

		install(new FactoryModuleBuilder()
				.implement(GatewayDevice.class, WSNDeviceAppConnectorImpl.class)
				.build(GatewayDeviceFactory.class)
		);
	}
}
