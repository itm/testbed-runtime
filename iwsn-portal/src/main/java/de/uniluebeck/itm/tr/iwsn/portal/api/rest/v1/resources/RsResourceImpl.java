package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.MakeReservationData;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.*;
import eu.wisebed.api.v3.snaa.SNAA;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static de.uniluebeck.itm.tr.common.ReservationHelper.deserializeToList;
import static de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources.ResourceHelper.getSAKsFromCookieOrHeader;

@Path("/reservations/")
public class RsResourceImpl implements RsResource {

	private static final Logger log = LoggerFactory.getLogger(RsResourceImpl.class);

	@Context
	private HttpHeaders httpHeaders;

	private final RS rs;

	private final SNAA snaa;

	@Inject
	public RsResourceImpl(final RS rs, final SNAA snaa) {
		this.rs = rs;
		this.snaa = snaa;
	}

	@Override
	@GET
	@Path("/public")
	@Produces({MediaType.APPLICATION_JSON})
	public List<PublicReservationData> listPublicReservations(
			@Nullable @QueryParam("from") final DateTime from,
			@Nullable @QueryParam("to") final DateTime to,
			@Nullable @QueryParam("offset") final Integer offset,
			@Nullable @QueryParam("amount") final Integer amount,
			@QueryParam("showCancelled") @DefaultValue("true") boolean showCancelled)
			throws RSFault_Exception, AuthorizationFault, AuthenticationFault {
		return getPublicReservations(from, to, offset, amount, showCancelled);
	}

	@Override
	@GET
	@Path("/personal")
	@Produces({MediaType.APPLICATION_JSON})
	public List<ConfidentialReservationData> listPersonalReservations(
			@Nullable @QueryParam("from") final DateTime from,
			@Nullable @QueryParam("to") final DateTime to,
			@Nullable @QueryParam("offset") final Integer offset,
			@Nullable @QueryParam("amount") final Integer amount,
			@QueryParam("showCancelled") @DefaultValue("true") boolean showCancelled)
			throws RSFault_Exception, AuthorizationFault, AuthenticationFault {
		final List<SecretAuthenticationKey> saks = getSAKsFromCookieOrHeader(httpHeaders, snaa);
		return getConfidentialReservations(saks, from, to, offset, amount, showCancelled);
	}

	@Override
	@POST
	@Path("create")
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public List<ConfidentialReservationData> makeReservation(MakeReservationData request)
			throws RSFault_Exception, AuthorizationFault, ReservationConflictFault_Exception, AuthenticationFault {

		final List<SecretReservationKey> secretReservationKeys = rs.makeReservation(
				getSAKsFromCookieOrHeader(httpHeaders, snaa),
				request.nodeUrns,
				request.from,
				request.to,
				request.description,
				request.options
		);
		try {
			return rs.getReservation(secretReservationKeys);
		} catch (UnknownSecretReservationKeyFault unknownSecretReservationKeyFault) {
			final String msg = "Internal error while retrieving reservation that was just created.";
			throw new RSFault_Exception(msg, new RSFault().withMessage(msg), unknownSecretReservationKeyFault);
		}
	}

	@Override
	@DELETE
	@Path("byExperimentId/{secretReservationKeysBase64}")
	@Produces({MediaType.TEXT_PLAIN})
	public void deleteReservation(@PathParam("secretReservationKeysBase64") final String secretReservationKeysBase64)
			throws RSFault_Exception, UnknownSecretReservationKeyFault, AuthorizationFault, AuthenticationFault {

		final List<SecretAuthenticationKey> secretAuthenticationKeys = getSAKsFromCookieOrHeader(httpHeaders, snaa);
		log.debug("Cookie (secret authentication keys): {}", secretAuthenticationKeys);
		rs.deleteReservation(secretAuthenticationKeys, deserializeToList(secretReservationKeysBase64));
	}

	@Override
	@GET
	@Path("byExperimentId/{secretReservationKeysBase64}")
	@Produces({MediaType.APPLICATION_JSON})
	public List<ConfidentialReservationData> getReservation(
			@PathParam("secretReservationKeysBase64") final String secretReservationKeysBase64)
			throws RSFault_Exception, UnknownSecretReservationKeyFault {
		return rs.getReservation(deserializeToList(secretReservationKeysBase64));
	}

	@Override
	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	public List<ConfidentialReservationData> getReservation(List<SecretReservationKey> secretReservationKeys)
			throws RSFault_Exception, UnknownSecretReservationKeyFault {
		return rs.getReservation(secretReservationKeys);
	}

	private List<ConfidentialReservationData> getConfidentialReservations(
			final List<SecretAuthenticationKey> snaaSecretAuthenticationKeys,
			@Nullable final DateTime from,
			@Nullable final DateTime to,
			@Nullable final Integer offset,
			@Nullable final Integer amount,
			@Nullable final Boolean showCancelled)
			throws RSFault_Exception, AuthorizationFault, AuthenticationFault {

		log.trace(
				"RsResourceImpl.getConfidentialReservations(snaaSecretAuthenticationKeys={}, from={}, to={}, offset={}, amount={}, showCancelled={})",
				snaaSecretAuthenticationKeys, from, to, offset, amount, showCancelled
		);

		return rs.getConfidentialReservations(snaaSecretAuthenticationKeys, from, to, offset, amount, showCancelled);
	}

	private List<PublicReservationData> getPublicReservations(@Nullable final DateTime from,
															  @Nullable final DateTime to,
															  @Nullable final Integer offset,
															  @Nullable final Integer amount,
															  @Nullable final Boolean showCancelled)
			throws RSFault_Exception {

		final List<PublicReservationData> reservations = rs.getReservations(from, to, offset, amount, showCancelled);

		log.debug("Got {} public reservations from {} until {} (offset: {}, amount: {})",
				reservations.size(),
				from,
				to,
				offset,
				amount
		);

		return reservations;
	}
}
