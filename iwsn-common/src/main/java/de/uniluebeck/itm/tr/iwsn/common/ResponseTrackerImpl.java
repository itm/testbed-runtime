package de.uniluebeck.itm.tr.iwsn.common;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;
import de.uniluebeck.itm.tr.util.SettableFutureMap;
import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static de.uniluebeck.itm.tr.iwsn.messages.RequestHelper.extractNodeUrns;

@SuppressWarnings("NullableProblems")
class ResponseTrackerImpl implements ResponseTracker {

	private final Request request;

	private final EventBusService eventBusService;

	private final SettableFutureMap<NodeUrn, SingleNodeResponse> futureMap;

	@Inject
	public ResponseTrackerImpl(@Assisted final Request request, final EventBusService eventBusService) {

		this.request = checkNotNull(request);
		this.eventBusService = checkNotNull(eventBusService);

		final Set<NodeUrn> requestNodeUrns = extractNodeUrns(request);

		checkArgument(!requestNodeUrns.isEmpty());

		final Map<NodeUrn, ListenableFuture<SingleNodeResponse>> futureMapContent = newHashMap();

		for (NodeUrn nodeUrn : requestNodeUrns) {
			futureMapContent.put(nodeUrn, SettableFuture.<SingleNodeResponse>create());
		}

		this.futureMap = new SettableFutureMap<NodeUrn, SingleNodeResponse>(futureMapContent);
		this.eventBusService.register(this);
	}

	@Subscribe
	public void onSingleNodeResponse(final SingleNodeResponse response) {
		if (request.getRequestId() == response.getRequestId()) {
			final NodeUrn responseNodeUrn = new NodeUrn(response.getNodeUrn());
			((SettableFuture<SingleNodeResponse>) futureMap.get(responseNodeUrn)).set(response);
			if (futureMap.isDone()) {
				eventBusService.unregister(this);
			}
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
	public Set<Entry<NodeUrn,ListenableFuture<SingleNodeResponse>>> entrySet() {
		return futureMap.entrySet();
	}

	@Override
	public Map<NodeUrn,SingleNodeResponse> get() throws InterruptedException, ExecutionException {
		return futureMap.get();
	}

	@Override
	public ListenableFuture<SingleNodeResponse> get(final Object key) {
		return futureMap.get(key);
	}

	@Override
	public Map<NodeUrn,SingleNodeResponse> get(final long timeout, final TimeUnit unit)
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
	public ListenableFuture<SingleNodeResponse> put(final NodeUrn key,
													final ListenableFuture<SingleNodeResponse> value) {
		return futureMap.put(key, value);
	}

	@Override
	public void putAll(
			final Map<? extends NodeUrn, ? extends ListenableFuture<SingleNodeResponse>> m) {
		futureMap.putAll(m);
	}

	@Override
	public ListenableFuture<SingleNodeResponse> remove(final Object key) {
		return futureMap.remove(key);
	}

	@Override
	public int size() {
		return futureMap.size();
	}

	@Override
	public Collection<ListenableFuture<SingleNodeResponse>> values() {
		return futureMap.values();
	}
}
