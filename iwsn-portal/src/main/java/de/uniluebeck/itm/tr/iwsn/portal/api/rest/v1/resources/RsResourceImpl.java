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

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static de.uniluebeck.itm.tr.iwsn.portal.ReservationHelper.deserialize;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.ResourceHelper.assertLoggedIn;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.ResourceHelper.getSAKsFromCookie;

@Path("/reservations/")
public class RsResourceImpl implements RsResource {

	private static final Logger log = LoggerFactory.getLogger(RsResourceImpl.class);

	@Context
	private HttpHeaders httpHeaders;

	private final RS rs;

	@Inject
	public RsResourceImpl(final RS rs) {
		this.rs = rs;
	}

	@Override
	@GET
	@Produces({MediaType.APPLICATION_JSON})
	public Object listReservations(@QueryParam("from") final DateTime from,
								   @QueryParam("to") final DateTime to,
								   @QueryParam("userOnly") @DefaultValue("false") final boolean userOnly,
								   @Nullable @QueryParam("offset") final Integer offset,
								   @Nullable @QueryParam("amount") final Integer amount)
			throws RSFault_Exception, AuthorizationFault, AuthenticationFault {

		final Interval interval = new Interval(from, to);
		return userOnly ?
				getConfidentialReservations(getSAKsFromCookie(httpHeaders), interval, offset, amount) :
				getPublicReservations(interval, offset, amount);
	}

	@Override
	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("create")
	public SecretReservationKeyListRs makeReservation(MakeReservationData request)
			throws RSFault_Exception, AuthorizationFault, ReservationConflictFault_Exception, AuthenticationFault {

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

	@Override
	@DELETE
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.TEXT_PLAIN})
	public void deleteReservation(SecretReservationKeyListRs secretReservationKeys)
			throws RSFault_Exception, UnknownSecretReservationKeyFault, AuthorizationFault, AuthenticationFault {

		List<SecretAuthenticationKey> secretAuthenticationKeys = assertLoggedIn(httpHeaders);
		log.debug("Cookie (secret authentication keys): {}", secretAuthenticationKeys);
		rs.deleteReservation(secretAuthenticationKeys, secretReservationKeys.reservations);
	}

	@Override
	@GET
	@Path("byExperimentId/{secretReservationKeysBase64}")
	@Produces({MediaType.APPLICATION_JSON})
	public ConfidentialReservationDataList getReservation(
			@PathParam("secretReservationKeysBase64") final String secretReservationKeysBase64)
			throws RSFault_Exception, UnknownSecretReservationKeyFault {
		return new ConfidentialReservationDataList(rs.getReservation(deserialize(secretReservationKeysBase64)));
	}

	@Override
	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public ConfidentialReservationDataList getReservation(SecretReservationKeyListRs secretReservationKeys)
			throws RSFault_Exception, UnknownSecretReservationKeyFault {

		return new ConfidentialReservationDataList(rs.getReservation(secretReservationKeys.reservations));
	}

	private ConfidentialReservationDataList getConfidentialReservations(
			final List<SecretAuthenticationKey> snaaSecretAuthenticationKeys,
			final Interval interval, final Integer offset, final Integer amount)
			throws RSFault_Exception, AuthorizationFault, AuthenticationFault {

		log.trace(
				"RsResourceImpl.getConfidentialReservations(snaaSecretAuthenticationKeys={}, interval={}, offset={}, amount={})",
				snaaSecretAuthenticationKeys, interval, offset, amount
		);

		return new ConfidentialReservationDataList(
				rs.getConfidentialReservations(
						snaaSecretAuthenticationKeys,
						interval.getStart(),
						interval.getEnd(),
						offset,
						amount
				)
		);
	}

	private PublicReservationDataList getPublicReservations(final Interval interval, final Integer offset,
															final Integer amount)
			throws RSFault_Exception {

		final List<PublicReservationData> reservations = rs.getReservations(
				interval.getStart(),
				interval.getEnd(),
				offset,
				amount
		);

		log.debug("Got {} public reservations from {} until {} (offset: {}, amount: {})",
				reservations.size(),
				interval.getStart(),
				interval.getEnd(),
				offset,
				amount
		);

		return new PublicReservationDataList(reservations);
	}
}
