package de.uniluebeck.itm.tr.rs.singleurnprefix;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import eu.wisebed.api.sm.SessionManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ServedNodeUrnsProvider implements Provider<String[]> {

	private static final Logger log = LoggerFactory.getLogger(ServedNodeUrnsProvider.class);

	private final SessionManagement sessionManagement;

	private final TimeLimiter timeLimiter;

	private final Callable<String[]> fetchNodeUrnsFromSessionManagementCallable = new Callable<String[]>() {
		@Override
		public String[] call() throws Exception {
			final List<String> nodeUrns = WiseMLHelper.getNodeUrns(sessionManagement.getNetwork());
			return nodeUrns.toArray(new String[nodeUrns.size()]);
		}
	};

	@Inject
	public ServedNodeUrnsProvider(@Nullable final SessionManagement sessionManagement,
								  @Named("SingleUrnPrefixSOAPRS.timeLimiter") final TimeLimiter timeLimiter) {
		this.sessionManagement = sessionManagement;
		this.timeLimiter = timeLimiter;
	}

	@Override
	public String[] get() {
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