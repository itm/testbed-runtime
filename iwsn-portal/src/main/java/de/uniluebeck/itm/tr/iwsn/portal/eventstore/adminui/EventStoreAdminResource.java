package de.uniluebeck.itm.tr.iwsn.portal.eventstore.adminui;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.Constants;
import de.uniluebeck.itm.tr.common.ReservationHelper;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

public class EventStoreAdminResource {

    private final RSPersistence rsPersistence;
    private final CommonConfig commonConfig;

    public static class ReservationEntry {
        public ConfidentialReservationData reservationData;
        public String secretReservationKeyBase64;
        public String href;
    }

    @Inject
    public EventStoreAdminResource(final CommonConfig commonConfig, final RSPersistence rsPersistence) {
        this.commonConfig = commonConfig;
        this.rsPersistence = checkNotNull(rsPersistence);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public List<ReservationEntry> listReservations(@Nullable @QueryParam("from") final DateTime from,
                                                   @Nullable @QueryParam("to") final DateTime to,
                                                   @Nullable @QueryParam("offset") final Integer offset,
                                                   @Nullable @QueryParam("amount") final Integer amount,
                                                   @QueryParam("showCancelled") @DefaultValue("true") boolean showCancelled) {

        final String hostname = commonConfig.getHostname();
        final int port = commonConfig.getPort();
        final String baseUri = "http://" + hostname + ":" + port + Constants.REST_API_V1.REST_API_CONTEXT_PATH_VALUE + "/events/";

        try {
            List<ConfidentialReservationData> reservations = rsPersistence.getReservations(from, to, offset, amount, showCancelled);
            List<ReservationEntry> entries = newArrayList();
            for (ConfidentialReservationData reservation : reservations) {
                ReservationEntry entry = new ReservationEntry();
                entry.reservationData = reservation;
                entry.secretReservationKeyBase64 = ReservationHelper.serialize(reservation.getSecretReservationKey());
                entry.href = baseUri + ReservationHelper.serialize(reservation.getSecretReservationKey()) + ".json";
                entries.add(entry);
            }
            return entries;
        } catch (RSFault_Exception e) {
            throw new RuntimeException("Exception while fetching reservations", e);
        }
    }
}
