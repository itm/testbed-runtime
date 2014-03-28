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
import eventstore.CloseableIterator;
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
    public Response getEvents(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64,
                                               @DefaultValue("-1") @QueryParam("from") long fromTimestamp,
                                               @DefaultValue("-1") @QueryParam("to") long toTimestamp) {
        try {
            CloseableIterator<IEventContainer> iterator = createIterator(secretReservationKeyBase64, fromTimestamp, toTimestamp);
            String path = portalServerConfig.getEventStoreDownloadPath() + "/"
                    + secretReservationKeyBase64 + "/events-"
                    + System.currentTimeMillis() + ".json";

            File file = buildJsonFile(path, iterator);
            iterator.close();
            return buildJsonResponse(file);


        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No events found for given reservation key!").build();
        }
    }

    private CloseableIterator<IEventContainer> createIterator(String secretReservationKeyBase64, long fromTimestamp, long toTimestamp) throws IOException {
        CloseableIterator<IEventContainer> iterator;
        if (fromTimestamp == -1 && toTimestamp == -1) {
            iterator = eventStoreService.getEvents(secretReservationKeyBase64);
        } else if (toTimestamp == -1) {
            iterator = eventStoreService.getEventsBetween(secretReservationKeyBase64, fromTimestamp, System.currentTimeMillis());
        }  else if(fromTimestamp == -1){
            iterator = eventStoreService.getEventsBetween(secretReservationKeyBase64,0,toTimestamp);
        } else {
            iterator = eventStoreService.getEventsBetween(secretReservationKeyBase64,fromTimestamp,toTimestamp);
        }
        return iterator;
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
                log.error("Unknown event type {}. Can't generate JSON", event);
            }
        }
        // TODO close iterator
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
