package de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.matcher.Matchers;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManagerImpl;
import de.uniluebeck.itm.tr.iwsn.portal.RandomRequestIdProvider;
import de.uniluebeck.itm.tr.iwsn.portal.RequestIdProvider;
import eu.wisebed.api.v3.sm.SessionManagement;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class SoapApiModule extends AbstractModule {

	@Override
	protected void configure() {

		install(new FactoryModuleBuilder()
				.implement(WSNService.class, WSNServiceImpl.class)
				.build(WSNServiceFactory.class)
		);

		install(new FactoryModuleBuilder()
				.implement(DeliveryManager.class, DeliveryManagerAdapter.class)
				.build(DeliveryManagerFactory.class)
		);

		bind(DeliveryManager.class).to(DeliveryManagerImpl.class);
		bindInterceptor(Matchers.subclassesOf(SessionManagement.class), Matchers.annotatedWith(Test.class), new MethodInterceptor() {
			@Override
			public Object invoke(final MethodInvocation invocation) throws Throwable {
				System.out.println("before!");
				Object result = invocation.proceed();
				System.out.println("after!");
				return result;
			}
		});
		bind(SoapApiService.class).to(SoapApiServiceImpl.class).in(Singleton.class);
		bind(SessionManagement.class).to(SessionManagementImpl.class).in(Singleton.class);
		bind(RequestIdProvider.class).to(RandomRequestIdProvider.class).in(Singleton.class);
	}
}
