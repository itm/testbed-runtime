package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.MakeReservationData;
import eu.wisebed.api.v3.common.SecretReservationKey;
import eu.wisebed.api.v3.rs.*;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

public interface RsResource {

	@GET
	@Produces({MediaType.APPLICATION_JSON})
	List<PublicReservationData> listPublicReservations(@QueryParam("from") DateTime from,
													   @QueryParam("to") DateTime to,
													   @Nullable @QueryParam("offset") Integer offset,
													   @Nullable @QueryParam("amount") Integer amount,
													   @DefaultValue("true") boolean showCancelled)
			throws RSFault_Exception, AuthorizationFault, AuthenticationFault;

	@GET
	@Produces({MediaType.APPLICATION_JSON})
	List<ConfidentialReservationData> listPersonalReservations(@QueryParam("from") DateTime from,
															   @QueryParam("to") DateTime to,
															   @Nullable @QueryParam("offset") Integer offset,
															   @Nullable @QueryParam("amount") Integer amount,
															   @DefaultValue("true") boolean showCancelled)
			throws RSFault_Exception, AuthorizationFault, AuthenticationFault;

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	@Path("create")
	List<ConfidentialReservationData> makeReservation(MakeReservationData request)
			throws RSFault_Exception, AuthorizationFault, ReservationConflictFault_Exception, AuthenticationFault;

	@DELETE
	@Produces({MediaType.TEXT_PLAIN})
	@Path("byExperimentId/{secretReservationKeysBase64}")
	void deleteReservation(@PathParam("secretReservationKeysBase64") String secretReservationKeysBase64)
			throws RSFault_Exception, UnknownSecretReservationKeyFault, AuthorizationFault, AuthenticationFault;

	@GET
	@Produces({MediaType.APPLICATION_JSON})
	List<ConfidentialReservationData> getReservation(String secretReservationKeysBase64)
			throws RSFault_Exception, UnknownSecretReservationKeyFault;

	@POST
	@Consumes({MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_JSON})
	List<ConfidentialReservationData> getReservation(List<SecretReservationKey> secretReservationKeys)
			throws RSFault_Exception, UnknownSecretReservationKeyFault;
}
