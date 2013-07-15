package de.uniluebeck.itm.tr.snaa.remote;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;
import de.uniluebeck.itm.tr.common.DecoratedImpl;
import de.uniluebeck.itm.tr.snaa.SNAAServiceConfig;
import de.uniluebeck.itm.tr.snaa.SNAAService;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.snaa.SNAA;

public class RemoteSNAAModule extends PrivateModule {

	@Override
	protected void configure() {

		requireBinding(SNAAServiceConfig.class);
		requireBinding(ServicePublisher.class);

		bind(RemoteSNAA.class).in(Scopes.SINGLETON);
		bind(SNAA.class).to(RemoteSNAA.class);
		bind(SNAAService.class).to(RemoteSNAA.class);

		expose(SNAA.class);
		expose(SNAAService.class);
	}

	@Provides
	@DecoratedImpl
	SNAA provideSNAA(final SNAAServiceConfig snaaServiceConfig) {
		return WisebedServiceHelper.getSNAAService(snaaServiceConfig.getSnaaRemoteUri().toString());
	}
}
