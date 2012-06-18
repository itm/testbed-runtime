package de.uniluebeck.itm.tr.iwsn;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import eu.wisebed.api.snaa.SNAA;

public class IWSNAuthorizationInterceptor implements MethodInterceptor{
	

	/** Logs messages */
	private static final Logger log = LoggerFactory.getLogger(IWSNAuthorizationInterceptor.class);
	
	/**
	 * The Authorization and Authentication server to be used to delegate the check for the user's
	 * privileges
	 */
	private SNAA snaa;
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		final AuthorizationRequired authorizationAnnotation = invocation.getMethod().getAnnotation(AuthorizationRequired.class);
		
		log.debug("Interception successfully");
		
		return invocation.proceed();
	}
}
