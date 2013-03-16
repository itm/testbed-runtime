package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManagerImpl;
import de.uniluebeck.itm.tr.iwsn.portal.RandomRequestIdProvider;
import de.uniluebeck.itm.tr.iwsn.portal.RequestIdProvider;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.api.v3.wsn.WSN;

public class SoapApiModule extends AbstractModule {

	@Override
	protected void configure() {

		install(new FactoryModuleBuilder()
				.implement(WSNService.class, WSNServiceImpl.class)
				.build(WSNServiceFactory.class)
		);

		install(new FactoryModuleBuilder()
				.implement(WSN.class, WSNImpl.class)
				.build(WSNFactory.class)
		);

		install(new FactoryModuleBuilder()
				.implement(AuthorizingWSN.class, AuthorizingWSNImpl.class)
				.build(AuthorizingWSNFactory.class)
		);

		install(new FactoryModuleBuilder()
				.implement(DeliveryManager.class, DeliveryManagerAdapter.class)
				.build(DeliveryManagerFactory.class)
		);

		bind(DeliveryManager.class).to(DeliveryManagerImpl.class);
		bind(SoapApiService.class).to(SoapApiServiceImpl.class).in(Singleton.class);
		bind(SessionManagement.class).to(SessionManagementImpl.class).in(Singleton.class);
		bind(RequestIdProvider.class).to(RandomRequestIdProvider.class).in(Singleton.class);
	}
}
