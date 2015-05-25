package de.uniluebeck.itm.tr.iwsn.portal.api;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.tr.iwsn.messages.*;
import de.uniluebeck.itm.tr.iwsn.portal.ReservationEventBus;
import de.uniluebeck.itm.util.concurrent.SettableFutureMap;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.wsn.ChannelPipelinesMap;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.tr.iwsn.portal.api.soap.v3.Converters.convert;
import static java.util.Optional.empty;
import static java.util.Optional.of;

public class RequestHelper {

	private final MessageFactory messageFactory;

	@Inject
	public RequestHelper(MessageFactory messageFactory) {
		this.messageFactory = checkNotNull(messageFactory);
	}

	public List<ChannelPipelinesMap> getChannelPipelines(final Iterable<NodeUrn> nodeUrns,
														 final String reservationId,
														 final ReservationEventBus reservationEventBus) {

		checkNotNull(nodeUrns);
		checkNotNull(reservationId);
		checkNotNull(reservationEventBus);

		final GetChannelPipelinesRequest request = messageFactory.getChannelPipelinesRequest(
				of(reservationId),
				empty(),
				nodeUrns
		);

		final Map<NodeUrn, SettableFuture<GetChannelPipelinesResponse.GetChannelPipelineResponse>> map = newHashMap();

		for (NodeUrn nodeUrn : nodeUrns) {
			map.put(nodeUrn, SettableFuture.<GetChannelPipelinesResponse.GetChannelPipelineResponse>create());
		}

		final SettableFutureMap<NodeUrn, GetChannelPipelinesResponse.GetChannelPipelineResponse>
				future = new SettableFutureMap<>(map);

		final Object eventBusListener = new Object() {

			@Subscribe
			public void onResponse(SingleNodeResponse response) {

				if (responseMatchesRequest(response.getHeader(), request.getHeader())) {
					response.getHeader().getNodeUrnsList().stream().map(NodeUrn::new).forEach(nodeUrn -> {
						map.get(nodeUrn).setException(new RuntimeException(response.getErrorMessage()));
					});
				}
			}

			@Subscribe
			public void onResponse(GetChannelPipelinesResponse response) {

				if (responseMatchesRequest(response.getHeader(), request.getHeader())) {
					response.getPipelinesList().stream().forEach(p -> {
						map.get(new NodeUrn(p.getNodeUrn())).set(p);
					});
				}
			}
		};

		reservationEventBus.register(eventBusListener);
		reservationEventBus.post(request);

		try {

			final
			Map<NodeUrn, GetChannelPipelinesResponse.GetChannelPipelineResponse>
					resultMap =
					future.get(30, TimeUnit.SECONDS);
			reservationEventBus.unregister(eventBusListener);
			return convert(resultMap);

		} catch (Exception e) {
			throw propagate(e);
		}
	}

	private boolean responseMatchesRequest(RequestResponseHeader responseHeader, RequestResponseHeader requestHeader) {
		return responseHeader.getRequestId() == requestHeader.getRequestId() &&
				requestHeader.getReservationId().equals(responseHeader.getReservationId());
	}

}
