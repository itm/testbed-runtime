package de.uniluebeck.itm.wisebed.cmdlineclient.wrapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import de.itm.uniluebeck.tr.wiseml.WiseMLHelper;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.JobResult;
import eu.wisebed.testbed.api.wsn.v211.WSN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;


public class WSNHelper {

	private static final Logger log = LoggerFactory.getLogger(WSNHelper.class);

	public static void disableAllPhysicalLinks(WSN wsn, String localControllerEndpointURL) {

		try {

			WSNAsyncWrapper wrapper = WSNAsyncWrapper.of(wsn, localControllerEndpointURL);
			List<String> nodeURNs = WiseMLHelper.getNodeUrns(wrapper.getNetwork().get());
			disableAllPhysicalLinks(wrapper, nodeURNs);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static void disableAllPhysicalLinks(WSN wsn, String localControllerEndpointURL, List<String> nodeURNs) {

		try {

			WSNAsyncWrapper wrapper = WSNAsyncWrapper.of(wsn, localControllerEndpointURL);
			disableAllPhysicalLinks(wrapper, nodeURNs);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static void disableAllPhysicalLinks(WSNAsyncWrapper wsn, List<String> nodeURNs) {

		try {

			List<Future<JobResult>> allFutures = Lists.newLinkedList();
			for (String nodeUrn : nodeURNs) {
				for (String otherNodeUrn : nodeURNs) {
					allFutures.add(wsn.disablePhysicalLink(nodeUrn, otherNodeUrn));
				}
			}
			for (Future<JobResult> future : allFutures) {
				JobResult jobResult = future.get();
				log.debug("{}", jobResult);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static void enableAllPhysicalLinks(WSN wsn, String localControllerEndpointURL, List<String> nodeUrns) {

		try {

			WSNAsyncWrapper wrapper = WSNAsyncWrapper.of(wsn, localControllerEndpointURL);
			enableAllPhysicalLinks(wrapper, nodeUrns);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static void enableAllPhysicalLinks(WSN wsn, String localControllerEndpointURL) {

		try {

			WSNAsyncWrapper wrapper = WSNAsyncWrapper.of(wsn, localControllerEndpointURL);
			List<String> nodeUrns = WiseMLHelper.getNodeUrns(wrapper.getNetwork().get());
			enableAllPhysicalLinks(wrapper, nodeUrns);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void enableAllPhysicalLinks(WSNAsyncWrapper wsn, List<String> nodeUrns) {

		try {

			List<Future<JobResult>> futures = Lists.newLinkedList();
			for (String nodeUrn : nodeUrns) {
				for (String otherNodeUrn : nodeUrns) {
					futures.add(wsn.enablePhysicalLink(nodeUrn, otherNodeUrn));
				}
			}
			for (Future<JobResult> future : futures) {
				JobResult jobResult = future.get();
				log.debug("{}", jobResult);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static void setVirtualLinks(WSN wsn, String localControllerEndpointURL, Multimap neighborhoodMap, String remoteServiceInstance) {
		setVirtualLinks(WSNAsyncWrapper.of(wsn, localControllerEndpointURL), neighborhoodMap, remoteServiceInstance);
	}

	public static void setVirtualLinks(WSNAsyncWrapper wsn, Multimap<String, String> neighborhoodMap,
									   String remoteServiceInstance) {

		try {

			List<Future<JobResult>> futures = Lists.newLinkedList();

			for (String sourceNodeURN : neighborhoodMap.keys()) {
				for (String targetNodeURN : neighborhoodMap.get(sourceNodeURN)) {
					futures.add(wsn.setVirtualLink(sourceNodeURN, targetNodeURN, remoteServiceInstance, null, null));
				}
			}

			for (Future<JobResult> future : futures) {
				JobResult jobResult = future.get();
				log.debug("{}", jobResult);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static void destroyVirtualLinks(WSN wsn, String localControllerEndpointURL, Multimap<String, String> neighborhoodMap) {
		destroyVirtualLinks(WSNAsyncWrapper.of(wsn, localControllerEndpointURL), neighborhoodMap);
	}

	public static void destroyVirtualLinks(WSNAsyncWrapper wsn, Multimap<String, String> neighborhoodMap) {

		try {

			List<Future<JobResult>> futures = Lists.newLinkedList();

			for (String sourceNodeURN : neighborhoodMap.keys()) {
				for (String targetNodeURN : neighborhoodMap.get(sourceNodeURN)) {
					futures.add(wsn.destroyVirtualLink(sourceNodeURN, targetNodeURN));
				}
			}

			for (Future<JobResult> future : futures) {
				JobResult jobResult = future.get();
				log.debug("{}", jobResult);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}
