package de.uniluebeck.itm.tr.rs;

import eu.wisebed.api.v3.common.KeyValuePair;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.*;
import org.joda.time.DateTime;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import java.util.List;

public class RSAuthorizationDecorator implements RS {

	private final RS rs;

	public RSAuthorizationDecorator(final RS rs) {
		this.rs = rs;
	}

	@Override
	@WebMethod
	@RequestWrapper(localName = "deleteReservation", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.DeleteReservation")
	@ResponseWrapper(localName = "deleteReservationResponse", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.DeleteReservationResponse")
	public void deleteReservation(
			@WebParam(name = "secretAuthenticationKeys", targetNamespace = "") final
			List<SecretAuthenticationKey> secretAuthenticationKeys,
			@WebParam(name = "secretReservationKey", targetNamespace = "") final
			List<SecretReservationKey> secretReservationKey)
			throws AuthorizationFault, RSFault_Exception, UnknownSecretReservationKeyFault {
		rs.deleteReservation(secretAuthenticationKeys, secretReservationKey);
	}

	@Override
	@WebMethod
	@WebResult(name = "reservationData", targetNamespace = "")
	@RequestWrapper(localName = "getConfidentialReservations", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.GetConfidentialReservations")
	@ResponseWrapper(localName = "getConfidentialReservationsResponse", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.GetConfidentialReservationsResponse")
	public List<ConfidentialReservationData> getConfidentialReservations(
			@WebParam(name = "secretAuthenticationKey", targetNamespace = "") final
			List<SecretAuthenticationKey> secretAuthenticationKey,
			@WebParam(name = "from", targetNamespace = "") final DateTime from,
			@WebParam(name = "to", targetNamespace = "") final DateTime to)
			throws AuthorizationFault, RSFault_Exception {
		return rs.getConfidentialReservations(secretAuthenticationKey, from, to);
	}

	@Override
	@WebMethod
	@WebResult(name = "reservationData", targetNamespace = "")
	@RequestWrapper(localName = "getReservation", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.GetReservation")
	@ResponseWrapper(localName = "getReservationResponse", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.GetReservationResponse")
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
			@WebParam(name = "from", targetNamespace = "") final DateTime from,
			@WebParam(name = "to", targetNamespace = "") final DateTime to) throws RSFault_Exception {
		return rs.getReservations(from, to);
	}

	@Override
	@WebMethod
	@WebResult(name = "secretReservationKey", targetNamespace = "")
	@RequestWrapper(localName = "makeReservation", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.MakeReservation")
	@ResponseWrapper(localName = "makeReservationResponse", targetNamespace = "http://wisebed.eu/api/v3/rs", className = "eu.wisebed.api.v3.rs.MakeReservationResponse")
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
			throws AuthorizationFault, RSFault_Exception, ReservationConflictFault_Exception {
		return rs.makeReservation(secretAuthenticationKeys, nodeUrns, from, to, description, options);
	}
}
