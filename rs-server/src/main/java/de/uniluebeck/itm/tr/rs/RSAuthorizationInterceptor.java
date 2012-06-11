package de.uniluebeck.itm.tr.rs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import eu.wisebed.api.rs.AuthorizationException;
import eu.wisebed.api.rs.AuthorizationExceptionException;
import eu.wisebed.api.rs.RSException;
import eu.wisebed.api.rs.RSExceptionException;
import eu.wisebed.api.rs.SecretAuthenticationKey;
import eu.wisebed.api.snaa.Action;
import eu.wisebed.api.snaa.Actions;
import eu.wisebed.api.snaa.SNAA;
import eu.wisebed.api.snaa.SNAAExceptionException;

/**
 * Instances of this class intercept calls to methods which are annotated in a way that indicate
 * that authorization is required for the requested actions.
 * 
 * @author Sebastian Ebers
 * @see AuthorizationRequired
 */
public class RSAuthorizationInterceptor implements MethodInterceptor {

	/** Logs messages */
	private static final Logger log = LoggerFactory.getLogger(RSAuthorizationInterceptor.class);

	/**
	 * The Authorization and Authentication server to be used to delegate the check for the user's
	 * privileges
	 */
	@Inject
	private SNAA snaa;

	// ------------------------------------------------------------------------
	/**
	 * Constructor
	 * 
	 * @param snaa
	 *            SNAA server to be used to check the authorization.
	 */
	public RSAuthorizationInterceptor(SNAA snaa) {
		this.snaa = snaa;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {

		final AuthorizationRequired authorizationAnnotation = invocation.getMethod().getAnnotation(AuthorizationRequired.class);
		boolean keysFound = false;
		boolean isAuthenticated = false;

		Action requestedAction = new Action(authorizationAnnotation.value());

		if (!requestedAction.equals(Actions.DELETE_RESERVATION) && !requestedAction.equals(Actions.GET_CONFIDENTIAL_RESERVATION)
				&& !requestedAction.equals(Actions.MAKE_RESERVATION)) {
			throwAuthorizationFailedException("Unknown annotated value \"" + requestedAction + "\"!");
		}

		Object[] arguments = invocation.getArguments();

		for (Object object : arguments) {

			if (object instanceof List<?>) {

				List<?> list = (List<?>) object;

				if (list.size() > 0 && list.get(0) instanceof eu.wisebed.api.rs.SecretAuthenticationKey) {
					isAuthenticated = checkAuthentication(convert((List<eu.wisebed.api.rs.SecretAuthenticationKey>) list), requestedAction);
					keysFound = true;
					break;
				} else if (list.size() > 0 && list.get(0) instanceof eu.wisebed.api.snaa.SecretAuthenticationKey) {
					isAuthenticated = checkAuthentication((List<eu.wisebed.api.snaa.SecretAuthenticationKey>) list, requestedAction);
					keysFound = true;
					break;
				}
			}
		}

		if (!keysFound) {
			throwAuthorizationFailedException("No sekret authentication keys found!");
		}
		if (!isAuthenticated) {
			throwAuthorizationFailedException("The user's access privileges are not sufficient");
		}

		log.debug("The user was authorized to perform the action \"MAKE_RESERVATION\".");
		return invocation.proceed();

	}

	// ------------------------------------------------------------------------
	/**
	 * Converts from RS specific secret reservation keys to SNAA specific ones
	 * 
	 * @param from
	 *            A collection of RS specific secret reservarion keys
	 * @return A collection of SNAA specific secret reservarion keys
	 */
	private List<eu.wisebed.api.snaa.SecretAuthenticationKey> convert(final Collection<eu.wisebed.api.rs.SecretAuthenticationKey> from) {
		List<eu.wisebed.api.snaa.SecretAuthenticationKey> to = new ArrayList<eu.wisebed.api.snaa.SecretAuthenticationKey>();

		for (SecretAuthenticationKey key : from) {
			eu.wisebed.api.snaa.SecretAuthenticationKey sak = new eu.wisebed.api.snaa.SecretAuthenticationKey();
			sak.setSecretAuthenticationKey(key.getSecretAuthenticationKey());
			sak.setUrnPrefix(key.getUrnPrefix());
			sak.setUsername(key.getUsername());
			to.add(sak);
		}

		return to;
	}

	// ------------------------------------------------------------------------
	/**
	 * Checks and returns whether an action is allowed to be performed due to the privileges bound
	 * to certain secret reservation keys.
	 * 
	 * @param saks
	 *            A collection of secret reservation keys
	 * @param action
	 *            Action to be performed.
	 * @return <code>true</code> if the action is authorized, <code>false</code> otherwise
	 * @throws RSExceptionException
	 *             Thrown if an exception is thrown in the reservation system
	 */
	private boolean checkAuthentication(final List<eu.wisebed.api.snaa.SecretAuthenticationKey> saks, final Action action)
			throws RSExceptionException {

		// Invoke isAuthorized
		try {

			boolean authorized = snaa.isAuthorized(saks, action);
			log.debug("Authorization result: " + authorized);
			return authorized;

		} catch (SNAAExceptionException e) {
			RSException rse = new RSException();
			log.warn(e.getMessage());
			rse.setMessage(e.getMessage());
			throw new RSExceptionException(e.getMessage(), rse);
		}
	}

	// ------------------------------------------------------------------------
	/**
	 * Throws an exception due to an authorization failure.
	 * 
	 * @param message
	 *            States the cause of the authorization failure.
	 * @throws AuthorizationExceptionException
	 *             Thrown to indicate an authorization failure.
	 */
	private void throwAuthorizationFailedException(final String message) throws AuthorizationExceptionException {
		AuthorizationException e = new AuthorizationException();

		String msg = "Authorization failed" + ((message != null && message.length() > 0) ? ": " + message : "");
		e.setMessage(msg);
		log.warn(msg, e);
		throw new AuthorizationExceptionException(msg, e);
	}
}
