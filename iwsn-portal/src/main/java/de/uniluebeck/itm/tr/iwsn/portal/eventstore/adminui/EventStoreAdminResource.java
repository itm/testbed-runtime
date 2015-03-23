package de.uniluebeck.itm.tr.iwsn.portal.eventstore.adminui;

import com.google.inject.Inject;
import de.uniluebeck.itm.eventstore.CloseableIterator;
import de.uniluebeck.itm.eventstore.EventContainer;
import de.uniluebeck.itm.tr.common.Constants;
import de.uniluebeck.itm.tr.common.ReservationHelper;
import de.uniluebeck.itm.tr.common.config.CommonConfig;
import de.uniluebeck.itm.tr.iwsn.messages.UpstreamMessageEvent;
import de.uniluebeck.itm.tr.iwsn.portal.Reservation;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationManager;
import de.uniluebeck.itm.tr.iwsn.portal.eventstore.ReservationEventStore;
import de.uniluebeck.itm.tr.rs.persistence.RSPersistence;
import eu.wisebed.api.v3.rs.ConfidentialReservationData;
import eu.wisebed.api.v3.rs.RSFault_Exception;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.common.json.JSONHelper.toJSON;

public class EventStoreAdminResource {

    private final RSPersistence rsPersistence;
    private final CommonConfig commonConfig;
    private final ReservationManager reservationManager;

    public static class ReservationEntry {
        public ConfidentialReservationData reservationData;
        public String secretReservationKeyBase64;
        public String href;
        public String stats;
    }

    @Inject
    public EventStoreAdminResource(final CommonConfig commonConfig, final RSPersistence rsPersistence,
                                   final ReservationManager reservationManager) {
        this.commonConfig = commonConfig;
        this.reservationManager = reservationManager;
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
        final String eventStoreBaseUri = "http://" + hostname + ":" + port + Constants.REST_API_V1.REST_API_CONTEXT_PATH_VALUE + "/events/";
        final String adminBaseUri = "http://" + hostname + ":" + port + Constants.EVENTSTORE.ADMIN_WEB_APP_CONTEXT_PATH + "/";

        try {
            List<ConfidentialReservationData> reservations = rsPersistence.getReservations(from, to, offset, amount, showCancelled);
            List<ReservationEntry> entries = newArrayList();

            for (ConfidentialReservationData reservation : reservations) {
                String secretReservationKeyBase64 = ReservationHelper.serialize(reservation.getSecretReservationKey());
                ReservationEntry entry = new ReservationEntry();
                entry.reservationData = reservation;
                entry.secretReservationKeyBase64 = secretReservationKeyBase64;
                entry.href = eventStoreBaseUri + secretReservationKeyBase64 + ".json";
                entry.stats = adminBaseUri + "stats/" + secretReservationKeyBase64 + ".json";
                entries.add(entry);
            }
            return entries;
        } catch (RSFault_Exception e) {
            throw new RuntimeException("Exception while fetching reservations", e);
        }
    }

    @XmlRootElement
    static class EventStoreStatistics {

        public DescriptiveStatisticsDto upstreamMessageEventTimestamps;
        public DescriptiveStatisticsDto upstreamMessageEventTimestampDiffs;
    }

    @XmlRootElement
    static class DescriptiveStatisticsDto {
        public double geometricMean;
        public double kurtosis;
        public double max;
        public double mean;
        public double min;
        public long n;
        public double percentile25;
        public double percentile50;
        public double percentile75;
        public double percentile100;
        public double populationVariance;
        public double skewness;
        public double standardDeviation;
        public double sum;
        public double sumsq;
        public double variance;
        public double[] values;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/stats/{secretReservationKeyBase64}.json")
    public Response getAnalysis(@PathParam("secretReservationKeyBase64") String secretReservationKeyBase64) {
        try {

            Reservation reservation = reservationManager.getReservation(secretReservationKeyBase64);
            ReservationEventStore eventStore = reservation.getEventStore();
            CloseableIterator<EventContainer> events = eventStore.getEvents();
            DescriptiveStatistics upstreamMessageTimestampStats = new DescriptiveStatistics();
            DescriptiveStatistics upstreamMessageTimestampDiffStats = new DescriptiveStatistics();
            boolean first = true;
            long lastTimestamp = 0;

            while (events.hasNext()) {
                EventContainer next = events.next();
                Object event = next.getEvent();
                if (event instanceof UpstreamMessageEvent) {
                    UpstreamMessageEvent ume = (UpstreamMessageEvent) event;
                    long diff;
                    if (first) {
                        first = false;
                        lastTimestamp = ume.getTimestamp();
                        diff = 0;
                    } else {
                        diff = Math.abs(ume.getTimestamp() - lastTimestamp);
                        lastTimestamp = ume.getTimestamp();
                    }
                    upstreamMessageTimestampStats.addValue(ume.getTimestamp());
                    upstreamMessageTimestampDiffStats.addValue(diff);
                }
            }

            EventStoreStatistics stats = new EventStoreStatistics();
            stats.upstreamMessageEventTimestamps = toDto(upstreamMessageTimestampStats);
            stats.upstreamMessageEventTimestampDiffs = toDto(upstreamMessageTimestampDiffStats);

            return Response.ok(toJSON(stats)).build();

        } catch (IOException e) {
            return Response.serverError().entity(e).build();
        }
    }

    private DescriptiveStatisticsDto toDto(DescriptiveStatistics upstreamMessageStats) {
        final DescriptiveStatisticsDto dto = new DescriptiveStatisticsDto();
        dto.geometricMean = upstreamMessageStats.getGeometricMean();
        dto.kurtosis = upstreamMessageStats.getKurtosis();
        dto.max = upstreamMessageStats.getMax();
        dto.mean = upstreamMessageStats.getMean();
        dto.min = upstreamMessageStats.getMin();
        dto.n = upstreamMessageStats.getN();
        dto.percentile25 = upstreamMessageStats.getPercentile(25);
        dto.percentile50 = upstreamMessageStats.getPercentile(50);
        dto.percentile75 = upstreamMessageStats.getPercentile(75);
        dto.percentile100 = upstreamMessageStats.getPercentile(100);
        dto.populationVariance = upstreamMessageStats.getPopulationVariance();
        dto.skewness = upstreamMessageStats.getSkewness();
        dto.standardDeviation = upstreamMessageStats.getStandardDeviation();
        dto.sum = upstreamMessageStats.getSum();
        dto.sumsq = upstreamMessageStats.getSumsq();
        dto.values = upstreamMessageStats.getValues();
        dto.variance = upstreamMessageStats.getVariance();
        return dto;
    }
}
