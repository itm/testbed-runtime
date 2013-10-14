package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.ConfidentialReservationDataList;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.MakeReservationData;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.SecretReservationKeyListRs;
import eu.wisebed.api.v3.rs.*;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

public interface RsResource {

	@GET
	@Produces({MediaType.APPLICATION_JSON})
	Object listReservations(@QueryParam("from") DateTime from,
							@QueryParam("to") DateTime to,
							@QueryParam("userOnly") @DefaultValue("false") boolean userOnly,
							@Nullable @QueryParam("offset") Integer offset,
							@Nullable @QueryParam("amount") Integer amount)
			throws RSFault_Exception, AuthorizationFault, AuthenticationFault;

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("create")
	SecretReservationKeyListRs makeReservation(MakeReservationData request)
			throws RSFault_Exception, AuthorizationFault, ReservationConflictFault_Exception, AuthenticationFault;

	@DELETE
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.TEXT_PLAIN})
	void deleteReservation(SecretReservationKeyListRs secretReservationKeys)
			throws RSFault_Exception, UnknownSecretReservationKeyFault, AuthorizationFault, AuthenticationFault;

	@GET
	@Produces({MediaType.APPLICATION_JSON})
	ConfidentialReservationDataList getReservation(String secretReservationKeysBase64)
			throws RSFault_Exception, UnknownSecretReservationKeyFault;

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	ConfidentialReservationDataList getReservation(SecretReservationKeyListRs secretReservationKeys)
			throws RSFault_Exception, UnknownSecretReservationKeyFault;
}
