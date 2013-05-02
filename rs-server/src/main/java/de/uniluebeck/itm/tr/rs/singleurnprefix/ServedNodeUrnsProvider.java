package de.uniluebeck.itm.tr.rs.singleurnprefix;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.wiseml.WiseMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ServedNodeUrnsProvider implements Provider<NodeUrn[]> {

	private static final Logger log = LoggerFactory.getLogger(ServedNodeUrnsProvider.class);

	private final SessionManagement sessionManagement;

	private final TimeLimiter timeLimiter;

	private final Callable<NodeUrn[]> fetchNodeUrnsFromSessionManagementCallable = new Callable<NodeUrn[]>() {
		@Override
		public NodeUrn[] call() throws Exception {
			final List<String> nodeUrns = WiseMLHelper.getNodeUrns(sessionManagement.getNetwork());
			final NodeUrn[] array = new NodeUrn[nodeUrns.size()];
			for (int i = 0; i < array.length; i++) {
				array[i] = new NodeUrn(nodeUrns.get(i));
			}
			return array;
		}
	};

	@Inject
	public ServedNodeUrnsProvider(@Nullable final SessionManagement sessionManagement,
								  final TimeLimiter timeLimiter) {
		this.sessionManagement = sessionManagement;
		this.timeLimiter = timeLimiter;
	}

	@Override
	public NodeUrn[] get() {
		try {

			log.debug("Retrieving node URNs from Session Management Service");
			return timeLimiter.callWithTimeout(fetchNodeUrnsFromSessionManagementCallable, 5, TimeUnit.SECONDS, true);

		} catch (Exception e) {

			if (e instanceof UncheckedTimeoutException) {
				throw Throwables.propagate(e);
			}

			log.warn("Exception while loading node URNs from session management endpoint {}!", sessionManagement);
			throw new RuntimeException(e);
		}
	}
}