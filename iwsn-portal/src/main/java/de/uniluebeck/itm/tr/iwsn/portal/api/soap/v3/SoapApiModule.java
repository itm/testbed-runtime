package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import de.uniluebeck.itm.tr.iwsn.portal.RandomRequestIdProvider;
import de.uniluebeck.itm.tr.iwsn.portal.RequestIdProvider;
import eu.wisebed.api.v3.sm.SessionManagement;

public class SoapApiModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(SoapApiService.class).to(SoapApiServiceImpl.class).in(Singleton.class);
		bind(SessionManagement.class).to(SessionManagementImpl.class).in(Singleton.class);
		bind(RequestIdProvider.class).to(RandomRequestIdProvider.class).in(Singleton.class);
	}
}
