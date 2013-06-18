package de.uniluebeck.itm.tr.wsn.federator;

import com.google.common.collect.BiMap;
import de.uniluebeck.itm.util.Tuple;
import eu.wisebed.wiseml.WiseMLHelper;
import eu.wisebed.wiseml.merger.config.MergerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.google.common.collect.Lists.newArrayList;

public abstract class FederatorWiseMLMerger {

	private static final Logger log = LoggerFactory.getLogger(FederatorWiseMLMerger.class);

	public static String merge(final BiMap<URI, Callable<String>> endpointUrlToCallableMap,
							   final ExecutorService executorService) {

		List<String> serializedWiseMLs = newArrayList();

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
				serializedWiseMLs.add(serializedWiseML);

			} catch (Exception e) {
				log.warn(
						"Ignoring federated testbed {} because it threw an exception when calling getNetwork(): {}",
						job.getFirst(),
						e.getMessage()
				);
			}

			final boolean callSuccessful = serializedWiseML != null;

			if (callSuccessful) {
				try {
					WiseMLHelper.deserialize(serializedWiseML);
					serializedWiseMLs.add(serializedWiseML);
				} catch (Exception e) {
					log.warn(
							"Exception while validating serialized WiseML returned by the federated testbed {}: {}",
							job.getFirst(),
							e.getMessage()
					);
				}
			}

		}

		// Merger configuration (default)
		MergerConfiguration config = new MergerConfiguration();

		// return merged network definitions
		try {
			return eu.wisebed.wiseml.merger.WiseMLMergerHelper.mergeFromStrings(config, serializedWiseMLs);
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}

	}

}
