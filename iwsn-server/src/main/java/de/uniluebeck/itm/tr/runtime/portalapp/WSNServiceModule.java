package de.uniluebeck.itm.tr.runtime.portalapp;

import org.aopalliance.intercept.MethodInterceptor;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;

import de.uniluebeck.itm.tr.iwsn.IWSNAuthorizationInterceptor;
import eu.wisebed.api.v3.WisebedServiceHelper;
import eu.wisebed.api.v3.snaa.SNAA;

public class WSNServiceModule extends AbstractModule {
	
	private SessionManagementServiceConfig config;
	private String authorizedUser;
	
	
	
	// ------------------------------------------------------------------------
	/**
	 * Constructor
	 * @param config
	 * @param authorizedUser
	 */
	public WSNServiceModule(SessionManagementServiceConfig config, String authorizedUser) {
		super();
		this.config = config;
		this.authorizedUser = authorizedUser;
	}



	@Override
	protected void configure() {
		
		Module pm = new AbstractModule() {
			
			@Override
			public void configure() {
				bind(String.class).annotatedWith(Names.named("AUTHENTICATED_USER")).toInstance(authorizedUser);
				SNAA snaa = WisebedServiceHelper.getSNAAService(config.getSnaaEndpointUrl().toString());
				bind(SNAA.class).toInstance(snaa);
				bind(MethodInterceptor.class).annotatedWith(Names.named("IWSNAuthorizationInterceptor")).to(IWSNAuthorizationInterceptor.class);
			}
		};
		install(pm);
		
		MethodInterceptor iwsnAuthorizationInterceptor = Guice.createInjector(pm).getInstance(Key.get(MethodInterceptor.class, Names.named("IWSNAuthorizationInterceptor")));
		bindInterceptor(Matchers.any(), Matchers.annotatedWith(de.uniluebeck.itm.tr.iwsn.AuthorizationRequired.class), iwsnAuthorizationInterceptor);
		
		install(new FactoryModuleBuilder()
		.implement(WSNService.class, WSNServiceImpl.class)
		.build(WSNServiceFactory.class)
);
	}
	

}
