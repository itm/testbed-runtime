package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.ConfidentialReservationDataList;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.MakeReservationData;
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
import java.util.List;

import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.ResourceHelper.assertLoggedIn;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.ResourceHelper.getSAKsFromCookie;

@Path("/reservations/")
public class RsResource {

	private static final Logger log = LoggerFactory.getLogger(RsResource.class);

	@Context
	private HttpHeaders httpHeaders;

	private final RS rs;

	@Inject
	public RsResource(final RS rs) {
		this.rs = rs;
	}

	@GET
	@Produces({MediaType.APPLICATION_JSON})
	public Object listReservations(@QueryParam("from") final DateTime from,
								   @QueryParam("to") final DateTime to,
								   @QueryParam("userOnly") @DefaultValue("false") final boolean userOnly)
			throws RSFault_Exception {

		final Interval interval = new Interval(from, to);
		return userOnly ?
				getConfidentialReservations(getSAKsFromCookie(httpHeaders), interval) :
				getPublicReservations(interval);
	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("create")
	public SecretReservationKeyListRs makeReservation(MakeReservationData request)
			throws RSFault_Exception, AuthorizationFault, ReservationConflictFault_Exception {

		final List<SecretAuthenticationKey> secretAuthenticationKeys = assertLoggedIn(httpHeaders);

		final List<SecretReservationKey> reservation = rs.makeReservation(
				secretAuthenticationKeys,
				request.nodeUrns,
				request.from,
				request.to,
				request.description,
				request.options
		);

		return new SecretReservationKeyListRs(reservation);
	}

	@DELETE
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.TEXT_PLAIN})
	public void deleteReservation(SecretReservationKeyListRs secretReservationKeys)
			throws RSFault_Exception, UnknownSecretReservationKeyFault {

		List<SecretAuthenticationKey> secretAuthenticationKeys = assertLoggedIn(httpHeaders);
		log.debug("Cookie (secret authentication keys): {}", secretAuthenticationKeys);
		rs.deleteReservation(secretReservationKeys.reservations);
	}

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public ConfidentialReservationDataList getReservation(SecretReservationKeyListRs secretReservationKeys)
			throws RSFault_Exception, UnknownSecretReservationKeyFault {

		return new ConfidentialReservationDataList(rs.getReservation(secretReservationKeys.reservations));
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

		final List<PublicReservationData> reservations = rs.getReservations(interval.getStart(), interval.getEnd());

		log.debug("Got {} public reservations from {} until {}",
				reservations.size(),
				interval.getStart(),
				interval.getEnd()
		);

		return new PublicReservationDataList(reservations);
	}
}
