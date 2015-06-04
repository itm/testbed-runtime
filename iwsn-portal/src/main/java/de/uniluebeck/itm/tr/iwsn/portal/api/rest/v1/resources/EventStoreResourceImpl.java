package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.tr.iwsn.messages.MessageHeaderPair;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationUnknownException;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.DtoHelper;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.ReservationEventStore;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import static de.uniluebeck.itm.tr.common.json.JSONHelper.toJSON;

@Path("/events/")
public class EventStoreResourceImpl implements EventStoreResource {

	private static final Logger log = LoggerFactory.getLogger(EventStoreResource.class);

	private final ReservationManager reservationManager;

	private final PortalServerConfig portalServerConfig;
	private final DtoHelper dtoHelper;

	@Context
	private ServletContext ctx;

	@Context
	private HttpServletResponse response;

	@Inject
	private EventStoreResourceImpl(final ReservationManager reservationManager,
								   final PortalServerConfig portalServerConfig,
								   final DtoHelper dtoHelper) {
		this.reservationManager = reservationManager;
		this.portalServerConfig = portalServerConfig;
		this.dtoHelper = dtoHelper;
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{secretReservationKeyBase64}.json")
	public Response getEvents(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
							  @DefaultValue("-1") @QueryParam("from") long fromTimestamp,
							  @DefaultValue("-1") @QueryParam("to") long toTimestamp) {

		if (!portalServerConfig.isReservationEventStoreEnabled()) {
			return Response
					.status(Response.Status.SERVICE_UNAVAILABLE)
					.entity("Reservation event storage has been disabled")
					.build();
		}

		log.trace("EventStoreResource.getEvents(key = {}, from = {}, to = {})", secretReservationKeyBase64, fromTimestamp, toTimestamp);
		// check if reservation is (still) known to RS (could have been deleted in the meantime, or given key is invalid)
		try {
			final Reservation reservation = reservationManager.getReservation(secretReservationKeyBase64);
			if (reservation.touch()) {
				log.trace("EventStoreResource: Got reservation {} from RM", reservation);

			} else {
				log.warn("Failed to touch reservation {}.", reservation);
			}
		} catch (ReservationUnknownException e) {
			return Response
					.status(Response.Status.NOT_FOUND)
					.entity("No reservation with secret reservation key \"" + secretReservationKeyBase64 + "\" found!")
					.build();
		}

		try {

			final CloseableIterator<MessageHeaderPair> iterator =
					createIterator(secretReservationKeyBase64, fromTimestamp, toTimestamp);

			final String filename = secretReservationKeyBase64 + "_" + DateTime.now().toString() + ".json";

			response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

			final StreamingOutput stream = output -> {

				final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(output));
				out.write("[");
				while (iterator.hasNext()) {
					out.write(toJSON(dtoHelper.encodeToJsonPojo(iterator.next())));
					if (iterator.hasNext()) {
						out.write(",");
						out.write("\n");
					}
				}
				out.write("]");
				out.write("\n");
				out.flush();
				out.close();
				iterator.close();
			};

			return Response.ok(stream).build();

		} catch (IOException e) {
			return Response
					.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(e.getMessage())
					.build();
		}
	}

	private CloseableIterator<MessageHeaderPair> createIterator(String secretReservationKeyBase64,
																long fromTimestamp,
																long toTimestamp) throws IOException {
		final ReservationEventStore eventStore =
				reservationManager.getReservation(secretReservationKeyBase64).getEventStore();

		CloseableIterator<MessageHeaderPair> iterator;
		if (fromTimestamp == -1 && toTimestamp == -1) {
			iterator = eventStore.getEvents();
		} else if (toTimestamp == -1) {
			iterator = eventStore.getEventsBetween(fromTimestamp, System.currentTimeMillis());
		} else if (fromTimestamp == -1) {
			iterator = eventStore.getEventsBetween(0, toTimestamp);
		} else {
			iterator = eventStore.getEventsBetween(fromTimestamp, toTimestamp);
		}
		return iterator;
	}
}
