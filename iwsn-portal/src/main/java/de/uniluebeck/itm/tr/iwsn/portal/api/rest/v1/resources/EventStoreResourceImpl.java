package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.IEventContainer;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.ReservationEndedMessage;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.ReservationStartedMessage;
import de.uniluebeck.itm.tr.iwsn.portal.events.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.events.ReservationStartedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.PortalEventStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static de.uniluebeck.itm.tr.iwsn.common.json.JSONHelper.toJSON;

@Path("/events/")
public class EventStoreResourceImpl implements EventStoreResource {

	private static final Logger log = LoggerFactory.getLogger(EventStoreResourceImpl.class);

	private final PortalEventStoreService eventStoreService;

	private final PortalServerConfig portalServerConfig;

	@Context
	private ServletContext ctx;

	@Context
	private HttpServletResponse response;

	@Inject
	public EventStoreResourceImpl(final PortalEventStoreService eventStoreService,
								  final PortalServerConfig portalServerConfig) {
		this.eventStoreService = eventStoreService;
		this.portalServerConfig = portalServerConfig;
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path("{secretReservationKeyBase64}.json")
	public Response getEvents(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
							  @DefaultValue("-1") @QueryParam("from") long fromTimestamp,
							  @DefaultValue("-1") @QueryParam("to") long toTimestamp) {
		try {

			final CloseableIterator<IEventContainer> iterator =
					createIterator(secretReservationKeyBase64, fromTimestamp, toTimestamp);

			String filename = secretReservationKeyBase64 + "-" + System.currentTimeMillis() + ".json";
			response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

			final StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					final BufferedOutputStream out = new BufferedOutputStream(output);
					while (iterator.hasNext()) {
						out.write(eventToJSON(iterator.next()).getBytes());
					}
					out.flush();
					out.close();
					iterator.close();
				}
			};

			return Response.ok(stream).build();

		} catch (IOException e) {
			return Response
					.status(Response.Status.BAD_REQUEST)
					.entity("No events found for given reservation key!")
					.build();
		}
	}

	private CloseableIterator<IEventContainer> createIterator(String secretReservationKeyBase64, long fromTimestamp,
															  long toTimestamp) throws IOException {
		CloseableIterator<IEventContainer> iterator;
		if (fromTimestamp == -1 && toTimestamp == -1) {
			iterator = eventStoreService.getEvents(secretReservationKeyBase64);
		} else if (toTimestamp == -1) {
			iterator = eventStoreService
					.getEventsBetween(secretReservationKeyBase64, fromTimestamp, System.currentTimeMillis());
		} else if (fromTimestamp == -1) {
			iterator = eventStoreService.getEventsBetween(secretReservationKeyBase64, 0, toTimestamp);
		} else {
			iterator = eventStoreService.getEventsBetween(secretReservationKeyBase64, fromTimestamp, toTimestamp);
		}
		return iterator;
	}

	private String eventToJSON(Object event) throws IllegalArgumentException {
		if (event == null) {
			throw new IllegalArgumentException("Can't generate JSON from null");
		}
		if (event instanceof ReservationStartedEvent) {
			return toJSON(new ReservationStartedMessage(((ReservationStartedEvent) event).getReservation()));
		} else if (event instanceof ReservationEndedEvent) {
			return toJSON(new ReservationEndedMessage(((ReservationEndedEvent) event).getReservation()));
		} else if (event instanceof com.google.protobuf.Message) {
			return JsonFormat.printToString((Message) event);
		} else {
			throw new IllegalArgumentException("Unknown event type. Can't generate JSON for type " + event.getClass());
		}
	}
}
