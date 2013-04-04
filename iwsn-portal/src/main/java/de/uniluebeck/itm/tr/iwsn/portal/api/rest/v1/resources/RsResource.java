package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.ConfidentialReservationDataList;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.PublicReservationDataList;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.SecretReservationKeyListRs;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.*;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;

import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.ResourceHelper.assertLoggedIn;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.ResourceHelper.getSAKsFromCookie;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.util.JSONHelper.toJSON;

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

			final Interval interval = new Interval(DateTime.parse(from), DateTime.parse(to));

			Object response = userOnly ?
					getConfidentialReservations(getSAKsFromCookie(httpHeaders), interval) :
					getPublicReservations(interval);

			return Response.ok(toJSON(response)).build();

		} catch (IllegalArgumentException e) {
			return returnError("Wrong input, please encode from and to as XMLGregorianCalendar", e, Status.BAD_REQUEST);
		} catch (RSFault_Exception e) {
			return returnError("Error while loading data from the reservation system", e, Status.BAD_REQUEST);
		}

	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("create")
	public Response makeReservation(PublicReservationData request) {

		final List<SecretAuthenticationKey> secretAuthenticationKeys = assertLoggedIn(httpHeaders);

		try {

			List<SecretReservationKey> reservation = rs.makeReservation(
					secretAuthenticationKeys,
					request.getNodeUrns(),
					request.getFrom(),
					request.getTo()
			);

			String jsonResponse = toJSON(new SecretReservationKeyListRs(reservation));
			log.debug("Made reservation: {}", jsonResponse);
			return Response.ok(jsonResponse).build();

		} catch (AuthorizationFault_Exception e) {
			return returnError("Authorization problem occurred", e, Status.UNAUTHORIZED);
		} catch (RSFault_Exception e) {
			return returnError("Error in the reservation system", e, Status.INTERNAL_SERVER_ERROR);
		} catch (ReservationConflictFault_Exception e) {
			return Response
					.status(Status.BAD_REQUEST)
					.entity(String.format("Another reservation is in conflict with yours: %s (%s)", e, e.getMessage()))
					.build();
		}

	}

	@DELETE
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.TEXT_PLAIN})
	public Response deleteReservation(SecretReservationKeyListRs secretReservationKeys) {

		List<SecretAuthenticationKey> secretAuthenticationKeys = assertLoggedIn(httpHeaders);

		log.debug("Cookie (secret authentication keys): {}", secretAuthenticationKeys);

		try {

			rs.deleteReservation(secretReservationKeys.reservations);
			return Response.ok("Ok, deleted reservation").build();

		} catch (RSFault_Exception e) {
			return returnError("Error while communicating with the reservation server", e,
					Status.INTERNAL_SERVER_ERROR
			);
		} catch (UnknownSecretReservationKeyFault e) {
			return returnError("Reservation not found", e, Status.BAD_REQUEST);
		}
	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public Response getReservation(SecretReservationKeyListRs secretReservationKeys) {

		try {

			List<ConfidentialReservationData> reservation = rs.getReservation(secretReservationKeys.reservations);
			String jsonResponse = toJSON(new ConfidentialReservationDataList(reservation));
			log.debug("Get reservation data for {}: {}", toJSON(secretReservationKeys), jsonResponse);
			return Response.ok(jsonResponse).build();

		} catch (RSFault_Exception e) {
			return returnError(
					"Error while communicating with the reservation server",
					e, Status.INTERNAL_SERVER_ERROR
			);
		} catch (UnknownSecretReservationKeyFault e) {
			return returnError("Reservation not found", e, Status.BAD_REQUEST);
		}
	}

	private Response returnError(String msg, Exception e, Status status) {
		log.debug(msg + " :" + e, e);
		String errorMessage = String.format("%s: %s (%s)", msg, e, e.getMessage());
		return Response.status(status).entity(errorMessage).build();
	}

	private ConfidentialReservationDataList getConfidentialReservations(
			final List<SecretAuthenticationKey> snaaSecretAuthenticationKeys,
			final Interval interval)
			throws RSFault_Exception {

		return new ConfidentialReservationDataList(
				rs.getConfidentialReservations(
						snaaSecretAuthenticationKeys,
						interval.getStart(),
						interval.getEnd()
				)
		);
	}

	private PublicReservationDataList getPublicReservations(final Interval interval)
			throws RSFault_Exception {

		List<PublicReservationData> reservations = rs.getReservations(interval.getStart(), interval.getEnd());

		log.debug("Got {} public reservations from {} until {}",
				reservations.size(), interval.getStart(), interval.getEnd()
		);

		return new PublicReservationDataList(reservations);
	}
}
