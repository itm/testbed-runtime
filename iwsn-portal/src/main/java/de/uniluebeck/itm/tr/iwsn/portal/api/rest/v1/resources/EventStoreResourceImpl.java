package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.resources;

import com.google.inject.Inject;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import de.uniluebeck.itm.tr.iwsn.portal.PortalServerConfig;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEndedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationStartedEvent;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.ReservationEndedMessage;
import de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.dto.ReservationStartedMessage;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.PortalEventStoreService;
import eventstore.IEventContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import static de.uniluebeck.itm.tr.iwsn.common.json.JSONHelper.toJSON;

@Path("/events/")
public class EventStoreResourceImpl implements EventStoreResource {
    private static final Logger log = LoggerFactory.getLogger(EventStoreResourceImpl.class);
    private final PortalEventStoreService eventStoreService;
    private final PortalServerConfig portalServerConfig;

    @Inject
    public EventStoreResourceImpl(final PortalEventStoreService eventStoreService, final PortalServerConfig portalServerConfig) {
        this.eventStoreService = eventStoreService;
        this.portalServerConfig = portalServerConfig;
    }

    @Override
    @GET
    @Path("{secretReservationKeyBase64}")
    public Response getEvents(@PathParam("secretReservationKeyBase64") final String secretReservationKeyBase64) {

        try {
            log.trace("EventStoreResourceImpl.getEvents({})", secretReservationKeyBase64);
            Iterator<IEventContainer> iterator = eventStoreService.getEvents(secretReservationKeyBase64);
            String path = portalServerConfig.getEventStoreDownloadPath() + "/"
                    + secretReservationKeyBase64 + "/allEvents-"
                    + System.currentTimeMillis() + ".json";

            log.trace("EventStorePath: {}", path);
            File file = buildJsonFile(path, iterator);
            return buildJsonResponse(file);


        } catch (IOException e) {
            log.warn("Can't generate json response!");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No events found for given reservation key!").build();
        }
    }

    @Override
    @GET
    @Path("{secretReservationKeyBase64}")
    public Response getEventsBetweenTimestamps(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
                                               @QueryParam("from") long fromTimestamp,
                                               @QueryParam("to") long toTimestamp) {
        try {
            Iterator<IEventContainer> iterator = eventStoreService.getEventsBetween(secretReservationKeyBase64
                    , fromTimestamp, toTimestamp);
            String path = portalServerConfig.getEventStoreDownloadPath() + "/"
                    + secretReservationKeyBase64 + "/eventsBetween-"
                    + fromTimestamp + "-" + toTimestamp + ".json";

            File file = buildJsonFile(path, iterator);
            return buildJsonResponse(file);


        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No events found for given reservation key!").build();
        }
    }

    private File buildJsonFile(String filePath, Iterator<IEventContainer> iterator) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        file.getParentFile().deleteOnExit();
        file.delete();
        file.createNewFile();
        file.deleteOnExit();
        FileWriter fw = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fw);
        out.write("[");
        while (iterator.hasNext()) {
            Object event = iterator.next().getEvent();
            try {
                String json = eventToJSON(event);
                if (json != null) {
                    out.write(json);
                    if (iterator.hasNext()) {
                        out.write(", ");
                    }
                }
            } catch (IllegalArgumentException e) {
                log.error("Unknown event type. Can't generate JSON");
            }
        }
        out.write("]");
        out.close();
        return file;
    }

    private Response buildJsonResponse(File file) throws WebApplicationException {
        if (file == null || !file.exists()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return Response.ok(file).build();
    }


    private String eventToJSON(Object event) throws IllegalArgumentException {
        if (event instanceof ReservationStartedEvent) {
            return toJSON(new ReservationStartedMessage(((ReservationStartedEvent) event).getReservation()));
        } else if (event instanceof ReservationEndedEvent) {
            return toJSON(new ReservationEndedMessage(((ReservationEndedEvent) event).getReservation()));
        } else if (event instanceof com.google.protobuf.Message) {
            return JsonFormat.printToString((Message) event);
        } else {
            throw new IllegalArgumentException("Unknown event type. Can't generate JSON for type "+ event.getClass());
        }
    }
}
