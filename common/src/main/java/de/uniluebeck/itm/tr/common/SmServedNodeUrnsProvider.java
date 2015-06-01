package de.uniluebeck.itm.tr.common;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.sm.SessionManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;
import static eu.wisebed.wiseml.WiseMLHelper.getNodeUrns;

public class SmServedNodeUrnsProvider implements ServedNodeUrnsProvider {

	private static final Logger log = LoggerFactory.getLogger(SmServedNodeUrnsProvider.class);

	private final SessionManagement sessionManagement;

	private final TimeLimiter timeLimiter;

	@Inject
	public SmServedNodeUrnsProvider(final SessionManagement sessionManagement, final TimeLimiter timeLimiter) {
		this.sessionManagement = checkNotNull(sessionManagement);
		this.timeLimiter = checkNotNull(timeLimiter);
	}

	@Override
	public Set<NodeUrn> get() {
		try {

			log.trace("Retrieving node URNs from Session Management Service");
			return timeLimiter.callWithTimeout(new Callable<Set<NodeUrn>>() {
				@Override
				public Set<NodeUrn> call() throws Exception {
					return newHashSet(transform(getNodeUrns(sessionManagement.getNetwork()), NodeUrn::new));
				}
			}, 5, TimeUnit.SECONDS, true
			);

		} catch (Exception e) {
			log.error("Exception while loading node URNs from session management endpoint {}: {}", sessionManagement, e
			);
			throw propagate(e);
		}
	}
}