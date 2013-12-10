package de.uniluebeck.itm.tr.iwsn.portal.api;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBus;
import de.uniluebeck.itm.util.concurrent.SettableFutureMap;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.wsn.ChannelPipelinesMap;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newGetChannelPipelinesRequest;
import static de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.Converters.convert;

public abstract class RequestHelper {

	public static List<ChannelPipelinesMap> getChannelPipelines(@Nonnull final Iterable<NodeUrn> nodeUrns,
																@Nonnull final String reservationId,
																final long requestId,
																@Nonnull
																final ReservationEventBus reservationEventBus) {

		checkNotNull(nodeUrns);
		checkNotNull(reservationId);
		checkNotNull(reservationEventBus);

		final Request request = newGetChannelPipelinesRequest(
				reservationId,
				requestId,
				nodeUrns
		);

		final Map<NodeUrn, SettableFuture<GetChannelPipelinesResponse.GetChannelPipelineResponse>>
				map = newHashMap();
		for (NodeUrn nodeUrn : nodeUrns) {
			map.put(nodeUrn, SettableFuture
					.<de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse>create()
			);
		}
		final SettableFutureMap<NodeUrn, GetChannelPipelinesResponse.GetChannelPipelineResponse>
				future =
				new SettableFutureMap<NodeUrn, de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse>(
						map
				);

		final Object eventBusListener = new Object() {

			@Subscribe
			public void onResponse(SingleNodeResponse response) {
				if (reservationId.equals(response.getReservationId()) && response.getRequestId() == requestId) {
					final
					SettableFuture<de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse>
							nodeFuture =
							map.get(new NodeUrn(response.getNodeUrn()));
					nodeFuture.setException(new RuntimeException(response.getErrorMessage()));
				}
			}

			@Subscribe
			public void onResponse(de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse response) {
				if (reservationId.equals(response.getReservationId()) && response.getRequestId() == requestId) {
					for (de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse p : response
							.getPipelinesList()) {

						final
						SettableFuture<de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse>
								nodeFuture =
								map.get(new NodeUrn(p.getNodeUrn()));
						nodeFuture.set(p);
					}
				}
			}
		};

		reservationEventBus.register(eventBusListener);
		reservationEventBus.post(request);

		try {

			final
			Map<NodeUrn, de.uniluebeck.itm.tr.iwsn.messages.GetChannelPipelinesResponse.GetChannelPipelineResponse>
					resultMap =
					future.get(30, TimeUnit.SECONDS);
			reservationEventBus.unregister(eventBusListener);
			return convert(resultMap);

		} catch (Exception e) {
			throw propagate(e);
		}
	}

}
