package de.uniluebeck.itm.tr.iwsn.common;

import com.google.common.base.Joiner;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.common.EventBusService;
import de.uniluebeck.itm.tr.iwsn.messages.Header;
import de.uniluebeck.itm.tr.iwsn.messages.Progress;
import de.uniluebeck.itm.tr.iwsn.messages.Response;
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

	private final Header requestHeader;

	private final EventBusService eventBusService;

	private final ProgressSettableFutureMap<NodeUrn, Response> futureMap;

	@Inject
	public ResponseTrackerImpl(@Assisted final Header requestHeader,
							   @Assisted final EventBusService eventBusService) {

		this.requestHeader = checkNotNull(requestHeader);
		this.eventBusService = checkNotNull(eventBusService);

		checkArgument(!requestHeader.getNodeUrnsList().isEmpty());

		final Map<NodeUrn, ProgressListenableFuture<Response>> futureMapContent = newHashMap();

		for (NodeUrn nodeUrn : transform(requestHeader.getNodeUrnsList(), NodeUrn::new)) {
			futureMapContent.put(nodeUrn, ProgressSettableFuture.<Response>create());
		}

		this.futureMap = new ProgressSettableFutureMap<>(futureMapContent);
		this.eventBusService.register(this);
	}

	@Override
	public Header getRequestHeader() {
		return requestHeader;
	}

	@Subscribe
	public void onResponse(final Response responseMsg) {

		Header request = requestHeader;
		Header response = responseMsg.getHeader();

		boolean bothDoNotHave = (!request.hasSerializedReservationKey() || "".equals(request.getSerializedReservationKey())) && (!response.hasSerializedReservationKey() || "".equals(response.getSerializedReservationKey()));
		boolean bothHave = request.hasSerializedReservationKey() && response.hasSerializedReservationKey();
		boolean sameReservation = bothDoNotHave || (bothHave && request.getSerializedReservationKey().equals(response.getSerializedReservationKey()));
		boolean sameCorrelationId = requestHeader.getCorrelationId() == response.getCorrelationId();
		boolean match = sameReservation && sameCorrelationId;

		if (log.isTraceEnabled()) {
			log.trace("ResponseTracker[\"{}\",{}].onResponse(\"{}\",{}) => match={} for node URNs {}",
					requestHeader.getSerializedReservationKey(),
					requestHeader.getCorrelationId(),
					response.getSerializedReservationKey(),
					response.getCorrelationId(),
					match,
					"[" + Joiner.on(",").join(response.getNodeUrnsList()) + "]"
			);
		}

		if (match) {

			response.getNodeUrnsList().stream().map(NodeUrn::new).forEach(responseNodeUrn -> {

				final ProgressSettableFuture<Response> future =
						(ProgressSettableFuture<Response>) futureMap.get(responseNodeUrn);

				if (future.isDone()) {
					log.warn(
							"Received multiple responses for node URN \"{}\", reservationId \"{}\" and requestId \"{}\". Ignoring subsequent responses...",
							responseNodeUrn, requestHeader.getSerializedReservationKey(), requestHeader.getCorrelationId()
					);
				} else {
					log.trace("ResponseTrackerImpl.onResponse() setting response for {}", responseNodeUrn);
					future.set(responseMsg);
					if (futureMap.isDone()) {
						eventBusService.unregister(this);
					}
				}
			});
		}
	}

	@Subscribe
	public void onSingleNodeProgress(final Progress progress) {

		log.trace("ResponseTracker[\"{}\",{}].onSingleNodeProgress({})", requestHeader.getSerializedReservationKey(), requestHeader.getCorrelationId(), progress);

		final boolean reservationIdEquals =
				((requestHeader.hasSerializedReservationKey() && progress.getHeader().hasSerializedReservationKey()) ||
						(!requestHeader.hasSerializedReservationKey() && !progress.getHeader().hasSerializedReservationKey())) &&
						requestHeader.getSerializedReservationKey().equals(progress.getHeader().getSerializedReservationKey());
		final boolean requestIdEquals = requestHeader.getCorrelationId() == progress.getHeader().getCorrelationId();
		final boolean forMe = reservationIdEquals && requestIdEquals;

		if (forMe) {

			progress.getHeader().getNodeUrnsList().stream().map(NodeUrn::new).forEach(progressNodeUrn -> {
				((ProgressSettableFuture<Response>) futureMap.get(progressNodeUrn)).setProgress(
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
	public Set<Entry<NodeUrn, ProgressListenableFuture<Response>>> entrySet() {
		return futureMap.entrySet();
	}

	@Override
	public Map<NodeUrn, Response> get() throws InterruptedException, ExecutionException {
		return futureMap.get();
	}

	@Override
	public ProgressListenableFuture<Response> get(final Object key) {
		return futureMap.get(key);
	}

	@Override
	public Map<NodeUrn, Response> get(final long timeout, final TimeUnit unit)
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
	public ProgressListenableFuture<Response> put(final NodeUrn key,
												  final ProgressListenableFuture<Response> value) {
		return futureMap.put(key, value);
	}

	@Override
	public void putAll(
			final Map<? extends NodeUrn, ? extends ProgressListenableFuture<Response>> m) {
		futureMap.putAll(m);
	}

	@Override
	public ProgressListenableFuture<Response> remove(final Object key) {
		return futureMap.remove(key);
	}

	@Override
	public int size() {
		return futureMap.size();
	}

	@Override
	public Collection<ProgressListenableFuture<Response>> values() {
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
		return "ResponseTrackerImpl[" +
				"correlationId=" + requestHeader.getCorrelationId() +
				",reservationId=" + requestHeader.getSerializedReservationKey() +
				"]@" + Integer.toHexString(hashCode());
	}
}
