package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.ConfidentialReservationDataList;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.PublicReservationDataList;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.SnaaSecretAuthenticationKeyList;
import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.rs.*;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.rs.*;
import eu.wisebed.restws.dto.SecretReservationKeyListRs;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

import static eu.wisebed.restws.resources.ResourceHelper.getSnaaSecretAuthCookie;
import static eu.wisebed.restws.util.JSONHelper.toJSON;

@Path("/reservations")
public class RsResource {

	private static final Logger log = LoggerFactory.getLogger(RsResource.class);

	@Context
	private HttpHeaders httpHeaders;

	private final RS rs;

	public RsResource(final RS rs) {
		this.rs = rs;
	}

	@GET
	@Produces({MediaType.APPLICATION_JSON})
	public Response listReservations(@QueryParam("from") final String from,
									 @QueryParam("to") final String to,
									 @QueryParam("userOnly") @DefaultValue("false") final boolean userOnly) {

		try {

			Object response = userOnly ?
					getConfidentialReservations(testbedId, getSnaaSecretAuthCookie(httpHeaders, testbedId), from, to) :
					getPublicReservations(testbedId, from, to);

			return Response.ok(toJSON(response)).build();

		} catch (IllegalArgumentException e) {
			return returnError("Wrong input, please encode from and to as XMLGregorianCalendar", e, Status.BAD_REQUEST);
		} catch (RSExceptionException e) {
			return returnError("Error while loading data from the reservation system", e, Status.BAD_REQUEST);
		}

	}

	private PublicReservationDataList getPublicReservations(final String from, final String to)
			throws RSExceptionException {
		RS rs = endpointManager.getRsEndpoint(testbedId);

		Tuple<XMLGregorianCalendar, XMLGregorianCalendar> duration = convertToDuration(from, to);
		XMLGregorianCalendar fromDate = duration.getFirst();
		XMLGregorianCalendar toDate = duration.getSecond();

		List<PublicReservationData> reservations = rs.getReservations(fromDate, toDate);
		
		log.debug("Got {} public reservations from {} until {}", new Object[] { reservations.size(), fromDate, toDate });
		
		return new PublicReservationDataList(reservations);
	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("create")
	public Response makeReservation(PublicReservationData request) {

		SnaaSecretAuthenticationKeyList snaaSecretAuthCookie = getSnaaSecretAuthCookie(httpHeaders, testbedId);

		try {

			RS rs = endpointManager.getRsEndpoint(testbedId);

			ConfidentialReservationData confidentialReservation =
					createFrom(snaaSecretAuthCookie.secretAuthenticationKeys, request);

			log.debug("Reservation request: " + toJSON(confidentialReservation));
			
			List<SecretReservationKey> reservation =
					rs.makeReservation(
							copySnaaToRs(snaaSecretAuthCookie.secretAuthenticationKeys),
							confidentialReservation
					);

			String jsonResponse = toJSON(new SecretReservationKeyListRs(reservation));
			log.debug("Made reservation: {}", jsonResponse);
			return Response.ok(jsonResponse).build();

		} catch (AuthorizationExceptionException e) {
			return returnError("Authorization problem occured", e, Status.UNAUTHORIZED);
		} catch (RSExceptionException e) {
			return returnError("Error in the reservation system", e, Status.INTERNAL_SERVER_ERROR);
		} catch (ReservervationConflictExceptionException e) {
			return Response
					.status(Status.BAD_REQUEST)
					.entity(String.format("Another reservation is in conflict with yours: %s (%s)", e, e.getMessage()))
					.build();
		}

	}

	/**
	 * Deletes a single reservation. public void deleteReservation(List<SecretAuthenticationKey> authenticationData,
	 * List<SecretReservationKey> secretReservationKey)
	 *
	 * @param testbedId
	 * @param secretReservationKeys
	 *
	 * @return
	 */
	@DELETE
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.TEXT_PLAIN})
	public Response deleteReservation(@PathParam("testbedId") final String testbedId,
									  SecretReservationKeyListRs secretReservationKeys) {

		SnaaSecretAuthenticationKeyList snaaSecretAuthCookie = getSnaaSecretAuthCookie(httpHeaders, testbedId);

		log.debug("Cookie (secret authentication key): {}", snaaSecretAuthCookie);

		if (snaaSecretAuthCookie != null) {
			try {

				RS rs = endpointManager.getRsEndpoint(testbedId);
				rs.deleteReservation(copySnaaToRs(snaaSecretAuthCookie.secretAuthenticationKeys),
						secretReservationKeys.reservations
				);
				return Response.ok("Ok, deleted reservation").build();

			} catch (RSExceptionException e) {
				return returnError("Error while communicating with the reservation server", e,
						Status.INTERNAL_SERVER_ERROR
				);
			} catch (ReservervationNotFoundExceptionException e) {
				return returnError("Reservation not found", e, Status.BAD_REQUEST);
			}
		}
		return returnError("Not logged in", new Exception("Not logged in"), Status.FORBIDDEN);
	}

	/**
	 * Returns data about a single reservation (including confidential information).
	 * <p/>
	 * WISEBED API function:
	 * <p/>
	 * public List<ConfidentialReservationData> getReservation(List<SecretReservationKey> secretReservationKey)
	 *
	 * @param testbedId
	 * @param secretReservationKeys
	 *
	 * @return
	 */
	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public Response getReservation(@PathParam("testbedId") final String testbedId,
								   SecretReservationKeyListRs secretReservationKeys) {

		try {

			RS rs = endpointManager.getRsEndpoint(testbedId);

			List<ConfidentialReservationData> reservation = rs.getReservation(secretReservationKeys.reservations);
			String jsonResponse = toJSON(new ConfidentialReservationDataList(reservation));
			log.debug("Get reservation data for {}: {}", toJSON(secretReservationKeys), jsonResponse);
			return Response.ok(jsonResponse).build();

		} catch (RSExceptionException e) {
			return returnError("Error while communicating with the reservation server", e, Status.INTERNAL_SERVER_ERROR
			);
		} catch (ReservervationNotFoundExceptionException e) {
			return returnError("Reservation not found", e, Status.BAD_REQUEST);
		}
	}

	private Response returnError(String msg, Exception e, Status status) {
		log.debug(msg + " :" + e, e);
		String errorMessage = String.format("%s: %s (%s)", msg, e, e.getMessage());
		return Response.status(status).entity(errorMessage).build();
	}

	private ConfidentialReservationData createFrom(List<SecretAuthenticationKey> secretAuthenticationKeys,
												   PublicReservationData reservation) {

		final ConfidentialReservationData confidentialReservation = new ConfidentialReservationData();

		for (SecretAuthenticationKey key : secretAuthenticationKeys) {

			final ConfidentialReservationDataKey target = new ConfidentialReservationDataKey();
			target.setUrnPrefix(key.getUrnPrefix());
			target.setKey(key.getKey());
			target.setUsername(key.getUsername());

			confidentialReservation.getKeys().add(target);
		}

		confidentialReservation.getNodeUrns().addAll(reservation.getNodeUrns());
		confidentialReservation.setFrom(reservation.getFrom());
		confidentialReservation.setTo(reservation.getTo());

		return confidentialReservation;
	}

	private Tuple<XMLGregorianCalendar, XMLGregorianCalendar> convertToDuration(String from, String to)
			throws IllegalArgumentException {

		try {
			DateTime now = new DateTime();
			DateTime startOfToday = new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), 0, 0);

			if (from == null || "".equals(from)) {
				from = DatatypeFactory.newInstance().newXMLGregorianCalendar(startOfToday.toGregorianCalendar()).toString();
			}

			if (to == null || "".equals(to)) {
				to = DatatypeFactory.newInstance().newXMLGregorianCalendar(startOfToday.plusDays(7).toGregorianCalendar()).toString();
			}

			XMLGregorianCalendar fromDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(from);
			XMLGregorianCalendar toDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(to);

			return new Tuple<XMLGregorianCalendar, XMLGregorianCalendar>(fromDate, toDate);

		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException("Unable to create a DataType factory: " + e, e);
		}

	}

	private ConfidentialReservationDataList getConfidentialReservations(final SnaaSecretAuthenticationKeyList snaaSecretAuthenticationKeyList,
																		final DateTime from, final DateTime to)
			throws RSFault_Exception {
		List<SecretAuthenticationKey> rsSAKs = snaaSecretAuthenticationKeyList.secretAuthenticationKeys;
		return new ConfidentialReservationDataList(rs.getConfidentialReservations(rsSAKs, from, to));
	}
}
