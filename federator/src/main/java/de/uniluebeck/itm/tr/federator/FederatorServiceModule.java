package de.uniluebeck.itm.tr.federator;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import de.uniluebeck.itm.tr.federator.iwsn.IWSNFederatorService;
import de.uniluebeck.itm.tr.federator.rs.RSFederatorService;
import de.uniluebeck.itm.tr.federator.snaa.SNAAFederatorService;

public class FederatorServiceModule extends AbstractModule {

	@Override
	protected void configure() {

		requireBinding(IWSNFederatorService.class);
		requireBinding(RSFederatorService.class);
		requireBinding(SNAAFederatorService.class);

		bind(FederatorService.class).to(FederatorServiceImpl.class).in(Scopes.SINGLETON);
	}
}
