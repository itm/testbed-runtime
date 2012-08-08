package de.uniluebeck.itm.tr.rs;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import eu.wisebed.api.common.SecretAuthenticationKey;
import eu.wisebed.api.common.UsernameUrnPrefixPair;
import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.RSException;
import eu.wisebed.api.rs.RSExceptionException;
import eu.wisebed.api.snaa.Action;
import eu.wisebed.api.snaa.AuthorizationResponse;
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

		/*
		 * (1) Identify the action to be authorized and check whether it is known
		 */

		final AuthorizationRequired authorizationAnnotation = invocation.getMethod().getAnnotation(AuthorizationRequired.class);

		// If the action is not known by the intercepter, throw an exception to indicate that an
		// authorization is impossible
		Action requestedAction = Action.valueOf(authorizationAnnotation.value());
		if (!requestedAction.equals(Action.RS_DELETE_RESERVATION) && !requestedAction.equals(Action.RS_GET_RESERVATIONS)
				&& !requestedAction.equals(Action.RS_MAKE_RESERVATION)) {
			throwAuthorizationFailedException("Unknown annotated value \"" + requestedAction + "\"!");
		}

		/*
		 * (2a) Retrieve the list of tuples of user names and URN prefixes (2b) Retrieve the list of
		 * urns of the nodes where the provided action is to be applied to
		 */

		List<UsernameUrnPrefixPair> usernamePrefixPairs = null;
		List<String> nodeURNs = null;

		Object[] arguments = invocation.getArguments();

		for (Object object : arguments) {

			if (object instanceof List<?>) {

				List<?> list = (List<?>) object;
				if (list.size() > 0 && list.get(0) instanceof SecretAuthenticationKey) {
					usernamePrefixPairs = new LinkedList<UsernameUrnPrefixPair>(convert((List<SecretAuthenticationKey>) list));
				}
			}else if (object instanceof ConfidentialReservationData){
				nodeURNs = ((ConfidentialReservationData)object).getNodeURNs();
			}
		}

		/*
		 * (3) Check whether the user is authorized to apply the action on the selected nodes and
		 * return the result
		 */	

		// To delete a reservation, it is sufficient to know of the secret reservation key.
		if (requestedAction.equals(Action.RS_DELETE_RESERVATION)){
			return invocation.proceed();
		}

		AuthorizationResponse isAuthorized = checkAuthorization(usernamePrefixPairs, requestedAction, nodeURNs);

		if (isAuthorized == null || !isAuthorized.isAuthorized()) {
			log.warn("The user was NOT authorized to perform the action \"" + requestedAction + "\".");
			throwAuthorizationFailedException("The user was NOT authorized to perform the action \"" + requestedAction + "\".");
		}

		log.debug("The user was authorized to perform the action \"" + requestedAction + "\".");
		return invocation.proceed();

	}

	// ------------------------------------------------------------------------
	/**
	 * Converts a list of secret authentication keys to a list of tuples comprising user names and
	 * urn prefixes and returns the result.
	 * 
	 * @param secretAuthenticationKeys
	 *            A list of secret authentication keys
	 * @return A list of tuples comprising user names and urn prefixes
	 */
	public static List<UsernameUrnPrefixPair> convert(final List<SecretAuthenticationKey> secretAuthenticationKeys) {
		List<UsernameUrnPrefixPair> usernamePrefixPairs = new LinkedList<UsernameUrnPrefixPair>();
		for (SecretAuthenticationKey secretAuthenticationKey : secretAuthenticationKeys) {
			UsernameUrnPrefixPair upp = new UsernameUrnPrefixPair();
			usernamePrefixPairs.add(upp);
			upp.setUsername(secretAuthenticationKey.getUsername());
			upp.setUrnPrefix(secretAuthenticationKey.getUrnPrefix());
		}
		return usernamePrefixPairs;
	}

	// ------------------------------------------------------------------------
	/**
	 * Checks and returns whether an action is allowed to be performed due to the privileges bound
	 * to certain secret reservation keys.
	 * 
	 * @param upp
	 *            A collection of tuples of user names and urn prefixes
	 * @param action
	 *            Action to be performed.
	 * @param nodeURNs
	 *            The urns of the nodes the action is to be performed at
	 * @return An object which returns whether the requested action is authorized.
	 * @throws RSExceptionException
	 *             Thrown if an exception is thrown in the reservation system
	 */
	private AuthorizationResponse checkAuthorization(final List<UsernameUrnPrefixPair> upp, final Action action, Collection<String> nodeURNs)
			throws RSExceptionException {

		try {
			String nodeURNsSting = null;
			if (nodeURNs != null) {
				StringBuffer nodeURNStringBuffer = new StringBuffer();
				for (String urn : nodeURNs) {
					nodeURNStringBuffer.append(urn + ",");
				}
				nodeURNsSting = nodeURNStringBuffer.substring(0, nodeURNStringBuffer.length() - 1);
			}

			AuthorizationResponse authorizationResponse = snaa.isAuthorized(upp, action, nodeURNsSting);
			log.debug("Authorization result: " + authorizationResponse);
			return authorizationResponse;

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
	 * @throws RSExceptionException
	 *             Thrown to indicate an authorization failure.
	 */
	private void throwAuthorizationFailedException(final String message) throws RSExceptionException {
		RSException e = new RSException();

		String msg = "Authorization failed" + ((message != null && message.length() > 0) ? ": " + message : "");
		e.setMessage(msg);
		log.warn(msg, e);
		throw new RSExceptionException(msg, e);
	}
}
