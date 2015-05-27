package de.uniluebeck.itm.tr.iwsn.common;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.common.EventBusService;
import de.uniluebeck.itm.tr.iwsn.messages.RequestResponseHeader;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeProgress;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;
import de.uniluebeck.itm.util.concurrent.ProgressListenableFuture;
import de.uniluebeck.itm.util.concurrent.ProgressSettableFuture;
import de.uniluebeck.itm.util.concurrent.ProgressSettableFutureMap;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Maps.newHashMap;

@SuppressWarnings("NullableProblems")
class ResponseTrackerImpl implements ResponseTracker {

	private final static Logger log = LoggerFactory.getLogger(ResponseTrackerImpl.class);

	private final RequestResponseHeader requestHeader;

	private final EventBusService eventBusService;

	private final ProgressSettableFutureMap<NodeUrn, SingleNodeResponse> futureMap;

	@Inject
	public ResponseTrackerImpl(@Assisted final RequestResponseHeader requestHeader,
							   @Assisted final EventBusService eventBusService) {

		this.requestHeader = checkNotNull(requestHeader);
		this.eventBusService = checkNotNull(eventBusService);

		checkArgument(!requestHeader.getNodeUrnsList().isEmpty());

		final Map<NodeUrn, ProgressListenableFuture<SingleNodeResponse>> futureMapContent = newHashMap();

		for (NodeUrn nodeUrn : transform(requestHeader.getNodeUrnsList(), NodeUrn::new)) {
			futureMapContent.put(nodeUrn, ProgressSettableFuture.<SingleNodeResponse>create());
		}

		this.futureMap = new ProgressSettableFutureMap<>(futureMapContent);
		this.eventBusService.register(this);
	}

	@Override
	public RequestResponseHeader getRequestHeader() {
		return requestHeader;
	}

	@Subscribe
	public void onSingleNodeResponse(final SingleNodeResponse response) {

		log.trace("ResponseTrackerImpl.onSingleNodeResponse({})", response);

		final boolean reservationIdEquals = ((requestHeader.hasReservationId() && response.getHeader().hasReservationId()) ||
				(!requestHeader.hasReservationId() && !response.getHeader().hasReservationId())) &&
				requestHeader.getReservationId().equals(response.getHeader().getReservationId());

		final boolean requestIdEquals = requestHeader.getRequestId() == response.getHeader().getRequestId();
		final boolean forMe = reservationIdEquals && requestIdEquals;

		if (forMe) {

			response.getHeader().getNodeUrnsList().stream().map(NodeUrn::new).forEach(responseNodeUrn -> {

				final ProgressSettableFuture<SingleNodeResponse> future =
						(ProgressSettableFuture<SingleNodeResponse>) futureMap.get(responseNodeUrn);

				if (future.isDone()) {
					log.warn(
							"Received multiple responses for node URN \"{}\", reservationId \"{}\" and requestId \"{}\". Ignoring subsequent responses...",
							responseNodeUrn, requestHeader.getReservationId(), requestHeader.getRequestId()
					);
				} else {
					future.set(response);
					if (futureMap.isDone()) {
						eventBusService.unregister(this);
					}
				}
			});
		}
	}

	@Subscribe
	public void onSingleNodeProgress(final SingleNodeProgress progress) {

		log.trace("ResponseTrackerImpl.onSingleNodeProgress({})", progress);

		final boolean reservationIdEquals =
				((requestHeader.hasReservationId() && progress.getHeader().hasReservationId()) ||
						(!requestHeader.hasReservationId() && !progress.getHeader().hasReservationId())) &&
						requestHeader.getReservationId().equals(progress.getHeader().getReservationId());
		final boolean requestIdEquals = requestHeader.getRequestId() == progress.getHeader().getRequestId();
		final boolean forMe = reservationIdEquals && requestIdEquals;

		if (forMe) {

			progress.getHeader().getNodeUrnsList().stream().map(NodeUrn::new).forEach(progressNodeUrn -> {
				((ProgressSettableFuture<SingleNodeResponse>) futureMap.get(progressNodeUrn)).setProgress(
						(float) progress.getProgressInPercent() / 100.0f
				);
			});
		}
	}

	@Override
	public void addListener(final Runnable listener, final Executor executor) {
		futureMap.addListener(listener, executor);
	}

	@Override
	public boolean cancel(final boolean mayInterruptIfRunning) {
		eventBusService.unregister(this);
		return futureMap.cancel(mayInterruptIfRunning);
	}

	@Override
	public void clear() {
		futureMap.clear();
	}

	@Override
	public boolean containsKey(final Object key) {
		return futureMap.containsKey(key);
	}

	@Override
	public boolean containsValue(final Object value) {
		return futureMap.containsValue(value);
	}

	@Override
	public Set<Entry<NodeUrn, ProgressListenableFuture<SingleNodeResponse>>> entrySet() {
		return futureMap.entrySet();
	}

	@Override
	public Map<NodeUrn, SingleNodeResponse> get() throws InterruptedException, ExecutionException {
		return futureMap.get();
	}

	@Override
	public ProgressListenableFuture<SingleNodeResponse> get(final Object key) {
		return futureMap.get(key);
	}

	@Override
	public Map<NodeUrn, SingleNodeResponse> get(final long timeout, final TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return futureMap.get(timeout, unit);
	}

	@Override
	public boolean isCancelled() {
		return futureMap.isCancelled();
	}

	@Override
	public boolean isDone() {
		return futureMap.isDone();
	}

	@Override
	public boolean isEmpty() {
		return futureMap.isEmpty();
	}

	@Override
	public Set<NodeUrn> keySet() {
		return futureMap.keySet();
	}

	@Override
	public ProgressListenableFuture<SingleNodeResponse> put(final NodeUrn key,
															final ProgressListenableFuture<SingleNodeResponse> value) {
		return futureMap.put(key, value);
	}

	@Override
	public void putAll(
			final Map<? extends NodeUrn, ? extends ProgressListenableFuture<SingleNodeResponse>> m) {
		futureMap.putAll(m);
	}

	@Override
	public ProgressListenableFuture<SingleNodeResponse> remove(final Object key) {
		return futureMap.remove(key);
	}

	@Override
	public int size() {
		return futureMap.size();
	}

	@Override
	public Collection<ProgressListenableFuture<SingleNodeResponse>> values() {
		return futureMap.values();
	}

	@Override
	public float getProgress() {
		return futureMap.getProgress();
	}

	@Override
	public void addProgressListener(final Runnable runnable, final Executor executor) {
		futureMap.addProgressListener(runnable, executor);
	}

	@Override
	public String toString() {
		return "ResponseTrackerImpl[requestId=" + requestHeader.getRequestId() + ",reservationId=" + requestHeader
				.getReservationId() + "]@" + Integer.toHexString(hashCode());
	}
}
