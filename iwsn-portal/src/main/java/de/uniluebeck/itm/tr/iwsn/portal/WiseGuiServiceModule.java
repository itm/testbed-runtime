package de.uniluebeck.itm.tr.iwsn.portal;

import com.google.inject.PrivateModule;
import de.uniluebeck.itm.servicepublisher.ServicePublisher;

public class WiseGuiServiceModule extends PrivateModule {

	@Override
	protected void configure() {

		requireBinding(ServicePublisher.class);
		requireBinding(PortalServerConfig.class);
		requireBinding(WiseGuiServiceConfig.class);

		bind(WiseGuiService.class).to(WiseGuiServiceImpl.class);

		expose(WiseGuiService.class);
	}
}
