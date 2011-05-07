package de.uniluebeck.itm.wisebed.cmdlineclient.wrapper;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.JobResult;


public class WSNHelper {

	private static final Logger log = LoggerFactory.getLogger(WSNHelper.class);

	public static boolean disableAllPhysicalLinks(WSNAsyncWrapper wsn, List<String> nodeURNs, int timeout,
												  TimeUnit timeUnit) throws TimeoutException {

		try {

			boolean allSuccessful = true;

			List<Future<JobResult>> allFutures = Lists.newLinkedList();
			for (String nodeUrn : nodeURNs) {
				for (String otherNodeUrn : nodeURNs) {
					allFutures.add(wsn.disablePhysicalLink(nodeUrn, otherNodeUrn, timeout, timeUnit));
				}
			}

			for (Future<JobResult> future : allFutures) {
				JobResult jobResult = future.get();
				allSuccessful = jobResult.getSuccessPercent() == 100 && allSuccessful;
				log.debug("{}", jobResult);
			}

			return allSuccessful;

		} catch (Exception e) {
			if (e instanceof ExecutionException && e.getCause() instanceof TimeoutException) {
				throw (TimeoutException) e.getCause();
			}
			throw new RuntimeException(e);
		}

	}

	public static boolean enableAllPhysicalLinks(WSNAsyncWrapper wsn, List<String> nodeUrns, int timeout,
												 TimeUnit timeUnit) throws
			TimeoutException {

		try {

			boolean allSuccessful = true;

			List<Future<JobResult>> futures = Lists.newLinkedList();

			for (String nodeUrn : nodeUrns) {
				for (String otherNodeUrn : nodeUrns) {
					futures.add(wsn.enablePhysicalLink(nodeUrn, otherNodeUrn, timeout, timeUnit));
				}
			}

			for (Future<JobResult> future : futures) {
				JobResult jobResult = future.get();
				allSuccessful = jobResult.getSuccessPercent() == 100 && allSuccessful;
				log.debug("{}", jobResult);
			}

			return allSuccessful;

		} catch (Exception e) {
			if (e instanceof ExecutionException && e.getCause() instanceof TimeoutException) {
				throw (TimeoutException) e.getCause();
			}
			throw new RuntimeException(e);
		}

	}

	public static boolean setVirtualLinks(WSNAsyncWrapper wsn, Multimap<String, String> neighborhoodMap,
										  String remoteServiceInstance, int timeout, TimeUnit timeUnit) throws TimeoutException {

		try {

			boolean allSuccessful = true;

			List<Future<JobResult>> futures = Lists.newLinkedList();

			for (String sourceNodeURN : neighborhoodMap.keys()) {
				for (String targetNodeURN : neighborhoodMap.get(sourceNodeURN)) {
					futures.add(
							wsn.setVirtualLink(sourceNodeURN, targetNodeURN, remoteServiceInstance, null, null, timeout,
									timeUnit
							)
					);
				}
			}

			for (Future<JobResult> future : futures) {
				JobResult jobResult = future.get();
				allSuccessful = jobResult.getSuccessPercent() == 100 && allSuccessful;
				log.debug("{}", jobResult);
			}

			return allSuccessful;

		} catch (Exception e) {
			if (e instanceof ExecutionException && e.getCause() instanceof TimeoutException) {
				throw (TimeoutException) e.getCause();
			}
			throw new RuntimeException(e);
		}

	}

	public static boolean destroyVirtualLinks(WSNAsyncWrapper wsn, Multimap<String, String> neighborhoodMap,
											  int timeout, TimeUnit timeUnit) throws TimeoutException {

		try {

			boolean allSuccessful = true;

			List<Future<JobResult>> futures = Lists.newLinkedList();

			for (String sourceNodeURN : neighborhoodMap.keys()) {
				for (String targetNodeURN : neighborhoodMap.get(sourceNodeURN)) {
					futures.add(wsn.destroyVirtualLink(sourceNodeURN, targetNodeURN, timeout, timeUnit));
				}
			}

			for (Future<JobResult> future : futures) {
				JobResult jobResult = future.get();
				allSuccessful = jobResult.getSuccessPercent() == 100 && allSuccessful;
				log.debug("{}", jobResult);
			}

			return allSuccessful;

		} catch (Exception e) {
			if (e instanceof ExecutionException && e.getCause() instanceof TimeoutException) {
				throw (TimeoutException) e.getCause();
			}
			throw new RuntimeException(e);
		}

	}

}
