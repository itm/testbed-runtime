package de.uniluebeck.itm.tr.federator.iwsn;

import com.google.common.base.Joiner;
import com.google.common.collect.BiMap;
import de.uniluebeck.itm.util.Tuple;
import eu.wisebed.wiseml.Setup;
import eu.wisebed.wiseml.WiseMLHelper;
import eu.wisebed.wiseml.Wiseml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.google.common.collect.Lists.newArrayList;

public abstract class FederatorWiseMLMerger {

	private static final Logger log = LoggerFactory.getLogger(FederatorWiseMLMerger.class);

	public static Wiseml merge(final BiMap<URI, Callable<String>> endpointUrlToCallableMap,
							   final ExecutorService executorService) {

		List<Wiseml> wiseMLs = newArrayList();

		List<Tuple<URI, Future<String>>> jobs = newArrayList();

		// for calls
		for (Map.Entry<URI, Callable<String>> entry : endpointUrlToCallableMap.entrySet()) {

			final URI endpointUrl = entry.getKey();
			final Callable<String> callable = entry.getValue();

			jobs.add(new Tuple<URI, Future<String>>(endpointUrl, executorService.submit(callable)));
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

}
