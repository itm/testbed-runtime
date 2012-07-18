package de.uniluebeck.itm.tr.iwsn;

import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import eu.wisebed.api.rs.AuthorizationException;
import eu.wisebed.api.rs.AuthorizationExceptionException;
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.api.snaa.SNAA;

public class IWSNAuthorizationInterceptor implements MethodInterceptor{
	

	/** Logs messages */
	private static final Logger log = LoggerFactory.getLogger(IWSNAuthorizationInterceptor.class);
	
	/**
	 * The Authorization and Authentication server to be used to delegate the check for the user's
	 * privileges
	 */
	private SNAA snaa;
	
	private String authenticatedUser;
	
	
	@Inject
	public IWSNAuthorizationInterceptor(SNAA snaa, @Named("AUTHENTICATED_USER") String authenticatedUser) {
		this.setSnaa(snaa);
		this.authenticatedUser = authenticatedUser;
	}
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		final AuthorizationRequired authorizationAnnotation = invocation.getMethod().getAnnotation(AuthorizationRequired.class);
		
		if (snaa == null){
			throw new AuthorizationExceptionException("No Authorization server configured", new AuthorizationException());
		}
		
		log.debug("Interception successful");
		
		return invocation.proceed();
	}

	// ------------------------------------------------------------------------
	/**
	 * @param snaa the snaa to set
	 */
	@Inject
	public void setSnaa(SNAA snaa) {
		this.snaa = snaa;
	}
}
