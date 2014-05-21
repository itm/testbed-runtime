package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.IEventContainer;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationUnknownException;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.*;
import de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.Converters;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.PortalEventStoreService;
import eu.wisebed.api.v3.common.NodeUrn;
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.tr.iwsn.common.json.JSONHelper.toJSON;

@Path("/events/")
public class EventStoreResourceImpl implements EventStoreResource {

	private static final Logger log = LoggerFactory.getLogger(EventStoreResourceImpl.class);

	private final PortalEventStoreService eventStoreService;

	private final PortalServerConfig portalServerConfig;

	private final ReservationManager reservationManager;

	@Context
	private ServletContext ctx;

	@Context
	private HttpServletResponse response;

	@Inject
	public EventStoreResourceImpl(final PortalEventStoreService eventStoreService,
								  final PortalServerConfig portalServerConfig,
								  final ReservationManager reservationManager) {
		this.eventStoreService = eventStoreService;
		this.portalServerConfig = portalServerConfig;
		this.reservationManager = reservationManager;
	}

	@Override
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{secretReservationKeyBase64}.json")
	public Response getEvents(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
							  @DefaultValue("-1") @QueryParam("from") long fromTimestamp,
							  @DefaultValue("-1") @QueryParam("to") long toTimestamp) {

		// check if reservation is (still) known to RS (could have been deleted in the meantime, or given key is invalid)
		try {
			reservationManager.getReservation(secretReservationKeyBase64);
		} catch (ReservationUnknownException e) {
			return Response
					.status(Response.Status.NOT_FOUND)
					.entity("No reservation with secret reservation key \"" + secretReservationKeyBase64 + "\" found!")
					.build();
		}

		try {

			final CloseableIterator<IEventContainer> iterator =
					createIterator(secretReservationKeyBase64, fromTimestamp, toTimestamp);

			//response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

			final StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(output));
					out.write("[");
					while (iterator.hasNext()) {
						out.write(eventToJSON(iterator.next().getEvent()));
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
				}
			};

			return Response.ok(stream).build();

		} catch (IOException e) {
			return Response
					.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(e.getMessage())
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

		if (event instanceof UpstreamMessageEvent) {

			return toJSON(new WebSocketUpstreamMessage((UpstreamMessageEvent) event), true);

		}
		if (event instanceof ReservationStartedEvent) {

			final String serializedKey = ((ReservationStartedEvent) event).getSerializedKey();
			final Reservation reservation = reservationManager.getReservation(serializedKey);
			return toJSON(new ReservationStartedMessage(reservation), true);

		} else if (event instanceof ReservationEndedEvent) {

			final String serializedKey = ((ReservationEndedEvent) event).getSerializedKey();
			final Reservation reservation = reservationManager.getReservation(serializedKey);
			return toJSON(new ReservationEndedMessage(reservation), true);

		} else if (event instanceof SingleNodeResponse) {

			return toJSON(new SingleNodeResponseMessage((SingleNodeResponse) event), true);

		} else if (event instanceof Request) {

			return toJSON(new RequestMessage((Request) event), true);

		} else if (event instanceof GetChannelPipelinesResponse.GetChannelPipelineResponse) {

			final GetChannelPipelinesResponse.GetChannelPipelineResponse response =
					(GetChannelPipelinesResponse.GetChannelPipelineResponse) event;
			Map<NodeUrn, GetChannelPipelinesResponse.GetChannelPipelineResponse> map = newHashMap();
			map.put(new NodeUrn(response.getNodeUrn()), response);
			return toJSON(Converters.convert(map), true);

		} else if (event instanceof DevicesAttachedEvent) {

			return toJSON(new DevicesAttachedMessage((DevicesAttachedEvent) event), true);

		} else if (event instanceof DevicesDetachedEvent) {

			return toJSON(new DevicesDetachedMessage((DevicesDetachedEvent) event), true);

		} else if (event instanceof NotificationEvent) {

			return toJSON(new WebSocketNotificationMessage((NotificationEvent) event), true);

		} else {
			throw new IllegalArgumentException("Unknown event type. Can't generate JSON for type " + event.getClass());
		}
	}
}
