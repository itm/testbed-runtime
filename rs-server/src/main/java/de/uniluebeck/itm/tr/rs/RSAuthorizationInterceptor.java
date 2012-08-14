package de.uniluebeck.itm.tr.rs;

import com.google.inject.Inject;
import eu.wisebed.api.common.SecretAuthenticationKey;
import eu.wisebed.api.common.UsernameNodeUrnsMap;
import eu.wisebed.api.common.UsernameUrnPrefixPair;
import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.RSException;
import eu.wisebed.api.rs.RSExceptionException;
import eu.wisebed.api.snaa.Action;
import eu.wisebed.api.snaa.AuthorizationResponse;
import eu.wisebed.api.snaa.IsValidResponse.ValidationResult;
import eu.wisebed.api.snaa.SNAA;
import eu.wisebed.api.snaa.SNAAExceptionException;
import eu.wisebed.api.util.WisebedConversionHelper;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.naming.directory.InvalidAttributesException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Instances of this class intercept calls to methods which are annotated in a way that indicate
 * that authorization is required for the requested actions.
 *
 * @author Sebastian Ebers
 * @see AuthorizationRequired
 */
public class RSAuthorizationInterceptor implements MethodInterceptor {

	/**
	 * Logs messages
	 */
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
	 * 		SNAA server to be used to check the authorization.
	 */
	public RSAuthorizationInterceptor(SNAA snaa) {
		this.snaa = snaa;
	}

	// @SuppressWarnings("unchecked")
	// @Override
	// public Object invoke(final MethodInvocation invocation) throws Throwable {
	//
	// /*
	// * (1) Identify the action to be authorized and check whether it is known
	// */
	//
	// final AuthorizationRequired authorizationAnnotation =
	// invocation.getMethod().getAnnotation(AuthorizationRequired.class);
	//
	// // If the action is not known by the interceptor, throw an exception to indicate that an
	// // authorization is impossible
	// Action requestedAction = Action.valueOf(authorizationAnnotation.value());
	// if (!requestedAction.equals(Action.RS_DELETE_RESERVATION) &&
	// !requestedAction.equals(Action.RS_GET_RESERVATIONS)
	// && !requestedAction.equals(Action.RS_MAKE_RESERVATION)) {
	// throwAuthorizationFailedException("Unknown annotated value \"" + requestedAction + "\"!");
	// }
	//
	// /*
	// * (2a) Retrieve the list of tuples of user names and URN prefixes (2b) Retrieve the list of
	// * urns of the nodes where the provided action is to be applied to
	// */
	//
	// List<UsernameUrnPrefixPair> usernamePrefixPairs = null;
	// List<String> nodeURNs = null;
	//
	// Object[] arguments = invocation.getArguments();
	//
	// for (Object object : arguments) {
	//
	// if (object instanceof List<?>) {
	//
	// List<?> list = (List<?>) object;
	// if (list.size() > 0 && list.get(0) instanceof SecretAuthenticationKey) {
	// usernamePrefixPairs = new
	// LinkedList<UsernameUrnPrefixPair>(WisebedConversionHelper.convert((List<SecretAuthenticationKey>)
	// list));
	// }
	// }else if (object instanceof ConfidentialReservationData){
	// nodeURNs = ((ConfidentialReservationData)object).getNodeURNs();
	// }
	// }
	//
	// /*
	// * (3) Check whether the user is authorized to apply the action on the selected nodes and
	// * return the result
	// */
	//
	// // To delete a reservation, it is sufficient to know of the secret reservation key.
	// if (requestedAction.equals(Action.RS_DELETE_RESERVATION)){
	// return invocation.proceed();
	// }
	//
	// if (requestedAction.equals(Action.RS_GET_RESERVATIONS))
	//
	// AuthorizationResponse isAuthorized = checkAuthorization(usernamePrefixPairs, requestedAction,
	// nodeURNs);
	//
	// if (isAuthorized == null || !isAuthorized.isAuthorized()) {
	// log.warn("The user was NOT authorized to perform the action \"" + requestedAction + "\".");
	// throwAuthorizationFailedException("The user was NOT authorized to perform the action \"" +
	// requestedAction + "\".");
	// }
	//
	// log.debug("The user was authorized to perform the action \"" + requestedAction + "\".");
	// return invocation.proceed();
	//
	// }

	@SuppressWarnings("unchecked")
	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {

		/*
		 * (1) Identify the action to be authorized and check whether it is known
		 */

		final AuthorizationRequired authorizationAnnotation =
				invocation.getMethod().getAnnotation(AuthorizationRequired.class);
		Action requestedAction = Action.valueOf(authorizationAnnotation.value());

		/*
		 * (2) Get the list of parameters and convert them into their original type
		 */

		List<UsernameUrnPrefixPair> usernamePrefixPairs = null;
		List<String> nodeURNs = null;
		List<SecretAuthenticationKey> secretAuthenticationKeys = null;

		Object[] arguments = invocation.getArguments();

		for (Object object : arguments) {

			if (object instanceof List<?>) {

				List<?> list = (List<?>) object;
				if (list.size() > 0 && list.get(0) instanceof SecretAuthenticationKey) {
					secretAuthenticationKeys = (List<SecretAuthenticationKey>) list;
					usernamePrefixPairs = new LinkedList<UsernameUrnPrefixPair>(
							WisebedConversionHelper.convert((List<SecretAuthenticationKey>) list)
					);
				}
			} else if (object instanceof ConfidentialReservationData) {
				nodeURNs = ((ConfidentialReservationData) object).getNodeUrns();
			}
		}

		// To delete a reservation, it is sufficient to know of the secret reservation key.
		// This key is unknown to SNAA. Thus, it is considered as authorized.
		if (requestedAction.equals(Action.RS_DELETE_RESERVATION)) {
			return invocation.proceed();
		}

		if (requestedAction.equals(Action.RS_GET_RESERVATIONS)) {
			AuthorizationResponse isAuthorized = checkRSGetReservationsAuthorization(secretAuthenticationKeys);
			if (!isAuthorized.isAuthorized()) {
				log.warn(
						"The user was NOT authorized to perform the action \"" + requestedAction + "\".\r\n" + isAuthorized
								.getMessage()
				);
				throw getAuthorizationFailedException(
						"The user was NOT authorized to perform the action \"" + requestedAction + "\".\r\n"
								+ isAuthorized.getMessage()
				);
			}
			return invocation.proceed();
		}

		if (requestedAction.equals(Action.RS_MAKE_RESERVATION)) {

			AuthorizationResponse isAuthorized =
					checkMakeReservationAuthorization(usernamePrefixPairs, requestedAction, nodeURNs);

			if (!isAuthorized.isAuthorized()) {
				log.warn(
						"The user was NOT authorized to perform the action \"" + requestedAction + "\".\r\n" + isAuthorized
								.getMessage()
				);
				throw getAuthorizationFailedException(
						"The user was NOT authorized to perform the action \"" + requestedAction + "\".\r\n"
								+ isAuthorized.getMessage()
				);
			}

			log.debug("The user was authorized to perform the action \"" + requestedAction + "\".");
			return invocation.proceed();
		}

		throw getAuthorizationFailedException(
				"The requested Action '" + authorizationAnnotation.value() + "' is unknown."
		);

	}

	private AuthorizationResponse checkRSGetReservationsAuthorization(
			List<SecretAuthenticationKey> secretAuthenticationKeys)
			throws SNAAExceptionException, RSExceptionException {

		AuthorizationResponse response = new AuthorizationResponse();
		response.setAuthorized(true);

		if (secretAuthenticationKeys != null) {
			for (SecretAuthenticationKey sak : secretAuthenticationKeys) {
				ValidationResult result = snaa.isValid(sak);
				if (!result.isValid()) {
					response.setMessage(
							"A provided secret authentication key is not valid" + (result.getMessage() != null ?
									": '" + result.getMessage() + "'" : ".")
					);
					response.setAuthorized(false);
				}
			}
		}

		return response;
	}

	// ------------------------------------------------------------------------

	/**
	 * Checks and returns whether an action is allowed to be performed due to the privileges bound
	 * to certain secret reservation keys.
	 *
	 * @param upp
	 * 		A collection of tuples of user names and urn prefixes
	 * @param action
	 * 		Action to be performed.
	 * @param nodeURNs
	 * 		The urns of the nodes the action is to be performed at
	 *
	 * @return An object which returns whether the requested action is authorized.
	 *
	 * @throws RSExceptionException
	 * 		Thrown if an exception is thrown in the reservation system
	 */
	@Nonnull
	private AuthorizationResponse checkMakeReservationAuthorization(final List<UsernameUrnPrefixPair> upp,
																	final Action action,
																	Collection<String> nodeURNs)
			throws RSExceptionException {

		try {

			final List<UsernameNodeUrnsMap> mapList =
					WisebedConversionHelper.convertToUsernameNodeUrnsMap(upp, nodeURNs);
			AuthorizationResponse authorizationResponse = snaa.isAuthorized(mapList, action);
			log.debug("Authorization result: " + authorizationResponse);
			if (authorizationResponse == null) {
				throw createRSException("Authorization result was null");
			}
			return authorizationResponse;

		} catch (SNAAExceptionException e) {
			throw createRSException(e.getMessage());
		} catch (InvalidAttributesException e) {
			throw createRSException(e.getMessage());
		}
	}

	private RSExceptionException createRSException(final String message) {
		RSException rse = new RSException();
		log.warn(message);
		rse.setMessage(message);
		return new RSExceptionException(message, rse);
	}

	// ------------------------------------------------------------------------

	/**
	 * Returns an exception due to an authorization failure.
	 *
	 * @param message
	 * 		States the cause of the authorization failure.
	 */
	private RSExceptionException getAuthorizationFailedException(final String message) {
		RSException e = new RSException();

		String msg = "Authorization failed" + ((message != null && message.length() > 0) ? ": " + message : "");
		e.setMessage(msg);
		log.warn(msg, e);
		return new RSExceptionException(msg, e);
	}
}
