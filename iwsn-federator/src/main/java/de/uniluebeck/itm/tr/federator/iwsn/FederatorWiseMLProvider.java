package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.base.Joiner;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.WisemlProvider;
import de.uniluebeck.itm.tr.federator.utils.FederatedEndpoints;
import de.uniluebeck.itm.util.Tuple;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.sm.SessionManagement;
import eu.wisebed.wiseml.Setup;
import eu.wisebed.wiseml.WiseMLHelper;
import eu.wisebed.wiseml.Wiseml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.google.common.collect.Lists.newArrayList;

public class FederatorWiseMLProvider implements WisemlProvider {

	private static final Logger log = LoggerFactory.getLogger(FederatorWiseMLProvider.class);

	private final FederatedEndpoints<SessionManagement> federatedEndpoints;

	@Inject
	public FederatorWiseMLProvider(
			final FederatedEndpoints<SessionManagement> federatedEndpoints) {
		this.federatedEndpoints = federatedEndpoints;
	}

	@Override
	public Wiseml get() {

		final BiMap<URI, Callable<String>> endpointUrlToCallableMap = HashBiMap.create();
		for (final FederatedEndpoints.Entry<SessionManagement> entry : federatedEndpoints.getEntries()) {
			endpointUrlToCallableMap.put(entry.endpointUrl, new Callable<String>() {
				@Override
				public String call() throws Exception {
					return entry.endpoint.getNetwork();
				}
			}
			);
		}

		final List<Wiseml> wiseMLs = newArrayList();
		final List<Tuple<URI, Future<String>>> jobs = newArrayList();

		// fork calls
		for (Map.Entry<URI, Callable<String>> entry : endpointUrlToCallableMap.entrySet()) {

			final URI endpointUrl = entry.getKey();
			final Callable<String> callable = entry.getValue();

			jobs.add(new Tuple<URI, Future<String>>(endpointUrl, MoreExecutors.sameThreadExecutor().submit(callable)));
		}

		// join calls
		for (Tuple<URI, Future<String>> job : jobs) {

			String serializedWiseML = null;

			try {
				serializedWiseML = job.getSecond().get();
			} catch (Exception e) {
				log.warn(
						"Ignoring federated testbed {} because it threw an exception when calling getNetwork(): {}",
						job.getFirst(),
						e.getMessage()
				);
			}

			if (serializedWiseML != null) {
				try {
					wiseMLs.add(WiseMLHelper.deserialize(serializedWiseML));
				} catch (Exception e) {
					log.warn(
							"Exception while validating serialized WiseML returned by the federated testbed {}: {}",
							job.getFirst(),
							e.getMessage()
					);
				}
			}

		}

		// merge WiseML documents (only the relevant parts)
		final Wiseml target = new Wiseml().withVersion("2.0").withSetup(
				new Setup().withDescription(
						"Federated testbed of " + Joiner.on(", ").join(endpointUrlToCallableMap.keySet()) + "."
				)
		);

		for (Wiseml current : wiseMLs) {
			for (Setup.Node node : current.getSetup().getNode()) {
				target.getSetup().getNode().add(node);
			}
		}

		return target;
	}

	@Override
	public Wiseml get(final Iterable<NodeUrn> nodeUrns) {
		final Wiseml wiseml = get();
		for (Iterator<Setup.Node> iterator = wiseml.getSetup().getNode().iterator(); iterator.hasNext(); ) {
			Setup.Node next = iterator.next();
			final NodeUrn currentNodeUrn = new NodeUrn(next.getId());
			if (!Iterables.contains(nodeUrns, currentNodeUrn)) {
				iterator.remove();
			}
		}
		return wiseml;
	}
}
