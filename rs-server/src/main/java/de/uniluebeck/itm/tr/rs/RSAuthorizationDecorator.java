package de.uniluebeck.itm.tr.rs;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.DecoratedImpl;
import eu.wisebed.api.v3.common.*;
import eu.wisebed.api.v3.rs.AuthenticationFault;
import eu.wisebed.api.v3.rs.AuthorizationFault;
import eu.wisebed.api.v3.rs.*;
import eu.wisebed.api.v3.rs.UnknownSecretReservationKeyFault;
import eu.wisebed.api.v3.snaa.*;
import org.joda.time.DateTime;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class RSAuthorizationDecorator implements RS {

	private final RS rs;

	private final SNAA snaa;

	@Inject
	public RSAuthorizationDecorator(@DecoratedImpl final RS rs, final SNAA snaa) {
		this.rs = rs;
		this.snaa = snaa;
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "deleteReservation", targetNamespace = "http://wisebed.eu/api/v3/rs",
			className = "eu.wisebed.api.v3.rs.DeleteReservation")
	@ResponseWrapper(localName = "deleteReservationResponse", targetNamespace = "http://wisebed.eu/api/v3/rs",
			className = "eu.wisebed.api.v3.rs.DeleteReservationResponse")
	public void deleteReservation(
			@WebParam(name = "secretAuthenticationKeys", targetNamespace = "") final
			List<SecretAuthenticationKey> secretAuthenticationKeys,
			@WebParam(name = "secretReservationKey", targetNamespace = "") final
			List<SecretReservationKey> secretReservationKey)
			throws AuthorizationFault, RSFault_Exception, UnknownSecretReservationKeyFault, AuthenticationFault {
		assertAuthorized(Action.RS_DELETE_RESERVATION, secretReservationKey);
		rs.deleteReservation(secretAuthenticationKeys, secretReservationKey);
	}

	@Override
	@WebMethod
	@WebResult(name = "reservationData", targetNamespace = "")
	@RequestWrapper(localName = "getConfidentialReservations", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.GetConfidentialReservations")
	@ResponseWrapper(localName = "getConfidentialReservationsResponse", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.GetConfidentialReservationsResponse")
	public List<ConfidentialReservationData> getConfidentialReservations(
			@WebParam(name = "secretAuthenticationKey", targetNamespace = "")
			List<SecretAuthenticationKey> secretAuthenticationKey,
			@WebParam(name = "from", targetNamespace = "")
			DateTime from,
			@WebParam(name = "to", targetNamespace = "")
			DateTime to,
			@WebParam(name = "offset", targetNamespace = "")
			Integer offset,
			@WebParam(name = "amount", targetNamespace = "")
			Integer amount)
			throws AuthorizationFault, RSFault_Exception, AuthenticationFault {

		try {

			assertSAKsValid(secretAuthenticationKey);

		} catch (SNAAFault_Exception e) {

			final RSFault faultInfo = new RSFault();
			faultInfo.setMessage(e.getMessage());
			throw new RSFault_Exception(e.getMessage(), faultInfo);
		}

		return rs.getConfidentialReservations(secretAuthenticationKey, from, to, offset, amount);
	}

	@Override
	@WebMethod
	@WebResult(name = "reservationData", targetNamespace = "")
	@RequestWrapper(localName = "getReservation", targetNamespace = "http://wisebed.eu/api/v3/rs",
			className = "eu.wisebed.api.v3.rs.GetReservation")
	@ResponseWrapper(localName = "getReservationResponse", targetNamespace = "http://wisebed.eu/api/v3/rs",
			className = "eu.wisebed.api.v3.rs.GetReservationResponse")
	public List<ConfidentialReservationData> getReservation(
			@WebParam(name = "secretReservationKey", targetNamespace = "") final
			List<SecretReservationKey> secretReservationKey)
			throws RSFault_Exception, UnknownSecretReservationKeyFault {
		return rs.getReservation(secretReservationKey);
	}

	@Override
	@WebMethod
	@WebResult(name = "reservations", targetNamespace = "")
	@RequestWrapper(localName = "getReservations", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.GetReservations")
	@ResponseWrapper(localName = "getReservationsResponse", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.GetReservationsResponse")
	public List<PublicReservationData> getReservations(
			@WebParam(name = "from", targetNamespace = "")
			DateTime from,
			@WebParam(name = "to", targetNamespace = "")
			DateTime to,
			@WebParam(name = "offset", targetNamespace = "")
			Integer offset,
			@WebParam(name = "amount", targetNamespace = "")
			Integer amount)
			throws RSFault_Exception {
		return rs.getReservations(from, to, offset, amount);
	}

	@Override
	@WebMethod
	@WebResult(name = "secretReservationKey", targetNamespace = "")
	@RequestWrapper(localName = "makeReservation", targetNamespace = "http://wisebed.eu/api/v3/rs",
			className = "eu.wisebed.api.v3.rs.MakeReservation")
	@ResponseWrapper(localName = "makeReservationResponse", targetNamespace = "http://wisebed.eu/api/v3/rs",
			className = "eu.wisebed.api.v3.rs.MakeReservationResponse")
	public List<SecretReservationKey> makeReservation(
			@WebParam(name = "secretAuthenticationKeys", targetNamespace = "") final
			List<SecretAuthenticationKey> secretAuthenticationKeys,
			@WebParam(name = "nodeUrns", targetNamespace = "") final
			List<NodeUrn> nodeUrns,
			@WebParam(name = "from", targetNamespace = "") final DateTime from,
			@WebParam(name = "to", targetNamespace = "") final DateTime to,
			@WebParam(name = "description", targetNamespace = "") final String description,
			@WebParam(name = "options", targetNamespace = "") final
			List<KeyValuePair> options)
			throws AuthorizationFault, RSFault_Exception, ReservationConflictFault_Exception, AuthenticationFault {
		assertAuthorized(Action.RS_MAKE_RESERVATION, secretAuthenticationKeys, nodeUrns);
		return rs.makeReservation(secretAuthenticationKeys, nodeUrns, from, to, description, options);
	}

	private void assertAuthorized(final Action action,
								  final List<SecretAuthenticationKey> secretAuthenticationKeys,
								  final List<NodeUrn> nodeUrns) throws AuthorizationFault, RSFault_Exception {

		try {
			assertSAKsValid(secretAuthenticationKeys);

			final List<UsernameNodeUrnsMap> list = newArrayList();
			for (SecretAuthenticationKey secretAuthenticationKey : secretAuthenticationKeys) {
				final UsernameNodeUrnsMap usernameNodeUrnsMap = new UsernameNodeUrnsMap();
				usernameNodeUrnsMap.setUrnPrefix(secretAuthenticationKey.getUrnPrefix());
				usernameNodeUrnsMap.setUsername(secretAuthenticationKey.getUsername());
				for (NodeUrn nodeUrn : nodeUrns) {
					if (nodeUrn.belongsTo(secretAuthenticationKey.getUrnPrefix())) {
						usernameNodeUrnsMap.getNodeUrns().add(nodeUrn);
					}
				}
				list.add(usernameNodeUrnsMap);
			}

			final AuthorizationResponse authorizationResponse = snaa.isAuthorized(list, action);
			if (!authorizationResponse.isAuthorized()) {
				final eu.wisebed.api.v3.common.AuthorizationFault faultInfo =
						new eu.wisebed.api.v3.common.AuthorizationFault();
				faultInfo.setMessage(authorizationResponse.getMessage());
				throw new AuthorizationFault(authorizationResponse.getMessage(), faultInfo);
			}
		} catch (SNAAFault_Exception e) {
			final RSFault faultInfo = new RSFault();
			faultInfo.setMessage(e.getMessage());
			throw new RSFault_Exception(e.getMessage(), faultInfo, e);
		}
	}

	private void assertSAKsValid(final List<SecretAuthenticationKey> secretAuthenticationKeys)
			throws SNAAFault_Exception, AuthorizationFault {
		final List<ValidationResult> validationResults = snaa.isValid(secretAuthenticationKeys);
		for (ValidationResult validationResult : validationResults) {
			if (!validationResult.isValid()) {
				final eu.wisebed.api.v3.common.AuthorizationFault faultInfo =
						new eu.wisebed.api.v3.common.AuthorizationFault();
				faultInfo.setMessage(validationResult.getMessage());
				throw new AuthorizationFault(validationResult.getMessage(), faultInfo);
			}
		}
	}

	private void assertAuthorized(final Action action, final List<SecretReservationKey> secretReservationKeys)
			throws AuthorizationFault, UnknownSecretReservationKeyFault, RSFault_Exception {

		final List<ConfidentialReservationData> reservation = rs.getReservation(secretReservationKeys);
		final List<UsernameNodeUrnsMap> usernameNodeUrnsMapList = newArrayList();

		for (ConfidentialReservationData confidentialReservationData : reservation) {
			final UsernameNodeUrnsMap usernameNodeUrnsMap = new UsernameNodeUrnsMap();
			usernameNodeUrnsMap.setUsername(confidentialReservationData.getUsername());
			usernameNodeUrnsMap.setUrnPrefix(confidentialReservationData.getSecretReservationKey().getUrnPrefix());
			usernameNodeUrnsMap.getNodeUrns().addAll(confidentialReservationData.getNodeUrns());
		}

		final AuthorizationResponse response;
		try {
			response = snaa.isAuthorized(usernameNodeUrnsMapList, action);
		} catch (SNAAFault_Exception e) {
			final RSFault faultInfo = new RSFault();
			faultInfo.setMessage(e.getMessage());
			throw new RSFault_Exception(e.getMessage(), faultInfo, e);
		}

		if (!response.isAuthorized()) {
			String message = "You are not response to execute the " + action + " operation!";
			final eu.wisebed.api.v3.common.AuthorizationFault faultInfo =
					new eu.wisebed.api.v3.common.AuthorizationFault();
			faultInfo.setMessage(message);
			throw new AuthorizationFault(message, faultInfo);
		}
	}
}
