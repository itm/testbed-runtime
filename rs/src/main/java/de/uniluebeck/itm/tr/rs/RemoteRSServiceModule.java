package de.uniluebeck.itm.tr.rs;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.DecoratedImpl;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.rs.RS;

public class RemoteRSServiceModule extends PrivateModule {

	@Override
	protected void configure() {

		requireBinding(ServicePublisher.class);
		requireBinding(RSServiceConfig.class);

		bind(RemoteRSService.class).in(Scopes.SINGLETON);
		bind(RS.class).to(RemoteRSService.class);
		bind(RSService.class).to(RemoteRSService.class);

		expose(RS.class);
		expose(RSService.class);
	}

	@Provides
	@DecoratedImpl
	RS provideRS(final RSServiceConfig rsServiceConfig) {
		return WisebedServiceHelper.getRSService(rsServiceConfig.getRsRemoteUri().toString());
	}
}
