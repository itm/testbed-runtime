package de.uniluebeck.itm.tr.rs;

import com.google.inject.Inject;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.common.UsernameUrnPrefixPair;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RSFault;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.AuthorizationResponse;
import eu.wisebed.api.v3.snaa.IsValidResponse.ValidationResult;
import eu.wisebed.api.v3.snaa.SNAA;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.naming.directory.InvalidAttributesException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static eu.wisebed.api.v3.util.WisebedConversionHelper.convert;
import static eu.wisebed.api.v3.util.WisebedConversionHelper.convertToUsernameNodeUrnsMap;

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
		List<NodeUrn> nodeURNs = null;
		List<SecretAuthenticationKey> secretAuthenticationKeys = null;

		Object[] arguments = invocation.getArguments();

		for (Object object : arguments) {

			if (object instanceof List<?>) {

				List<?> list = (List<?>) object;
				if (list.size() > 0 && list.get(0) instanceof SecretAuthenticationKey) {
					secretAuthenticationKeys = (List<SecretAuthenticationKey>) list;
					usernamePrefixPairs = new LinkedList<UsernameUrnPrefixPair>(
							convert((List<SecretAuthenticationKey>) list)
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

			AuthorizationResponse isAuthorized = checkMakeReservationAuthorization(
					usernamePrefixPairs,
					requestedAction,
					nodeURNs
			);

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
			throws SNAAFault_Exception, RSFault_Exception {

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
	 * @throws RSFault_Exception
	 * 		Thrown if an exception is thrown in the reservation system
	 */
	@Nonnull
	private AuthorizationResponse checkMakeReservationAuthorization(final List<UsernameUrnPrefixPair> upp,
																	final Action action,
																	final Collection<NodeUrn> nodeURNs)
			throws RSFault_Exception {

		try {

			final List<UsernameNodeUrnsMap> mapList = convertToUsernameNodeUrnsMap(upp, nodeURNs);
			AuthorizationResponse authorizationResponse = snaa.isAuthorized(mapList, action);
			log.debug("Authorization result: " + authorizationResponse);
			if (authorizationResponse == null) {
				throw createRSFault("Authorization result was null");
			}
			return authorizationResponse;

		} catch (SNAAFault_Exception e) {
			throw createRSFault(e.getMessage());
		} catch (InvalidAttributesException e) {
			throw createRSFault(e.getMessage());
		}
	}

	private RSFault_Exception createRSFault(final String message) {
		RSFault rse = new RSFault();
		log.warn(message);
		rse.setMessage(message);
		return new RSFault_Exception(message, rse);
	}

	// ------------------------------------------------------------------------

	/**
	 * Returns an exception due to an authorization failure.
	 *
	 * @param message
	 * 		States the cause of the authorization failure.
	 */
	private RSFault_Exception getAuthorizationFailedException(final String message) {
		RSFault e = new RSFault();

		String msg = "Authorization failed" + ((message != null && message.length() > 0) ? ": " + message : "");
		e.setMessage(msg);
		log.warn(msg, e);
		return new RSFault_Exception(msg, e);
	}
}
