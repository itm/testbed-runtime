package de.uniluebeck.itm.tr.plugins.defaultimage;

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTracker;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.messages.FlashImagesRequest;
import de.uniluebeck.itm.tr.iwsn.messages.MessageFactory;
import de.uniluebeck.itm.tr.iwsn.messages.Response;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.tr.iwsn.portal.nodestatustracker.FlashStatus;
import de.uniluebeck.itm.tr.iwsn.portal.nodestatustracker.NodeStatusTracker;
import de.uniluebeck.itm.tr.rs.RSHelper;
import de.uniluebeck.itm.util.concurrent.ExecutorUtils;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.filterValues;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.*;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static java.util.Optional.empty;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MINUTES;

public class DefaultImagePluginImpl extends AbstractService implements DefaultImagePlugin {

	private static final Logger log = LoggerFactory.getLogger(DefaultImagePlugin.class);

	private static final String CONFIG_KEY_DEFAULT_IMAGE = "DefaultImagePlugin.defaultImage";

	private static final Predicate<DeviceConfig> DEVICE_CONFIG_HAS_DEFAULT_IMAGE = new Predicate<DeviceConfig>() {
		@Override
		public boolean apply(final DeviceConfig input) {
			return input.getNodeConfiguration().containsKey(CONFIG_KEY_DEFAULT_IMAGE);
		}
	};

	private static final ThreadFactory SCHEDULER_THREAD_FACTORY =
			new ThreadFactoryBuilder().setNameFormat("DefaultImagePlugin %d").build();

	private static final int SCHEDULER_THREAD_POOL_SIZE = 1;

	private static final int SCHEDULING_INTERVAL = 1;

	private static final Duration UNRESERVED_DURATION_THRESHOLD = Duration.standardHours(1);

	private final Random random = new Random();

	private final NodeStatusTracker nodeStatusTracker;

	private final PortalEventBus portalEventBus;

	private final DeviceDBService deviceDBService;

	private final ResponseTrackerFactory responseTrackerFactory;

	private final RSHelper rsHelper;
	private final MessageFactory messageFactory;

	private ScheduledExecutorService scheduler;

	private Runnable defaultImageRunnable = () -> {
		try {
			flashDefaultImages();
		} catch (Exception e) {
			log.error("Exception while flashing default images: ", e);
		}
	};

	private ScheduledFuture<?> schedule;

	@Inject
	public DefaultImagePluginImpl(final NodeStatusTracker nodeStatusTracker,
								  final PortalEventBus portalEventBus,
								  final DeviceDBService deviceDBService,
								  final ResponseTrackerFactory responseTrackerFactory,
								  final RSHelper rsHelper,
								  final MessageFactory messageFactory) {
		this.nodeStatusTracker = checkNotNull(nodeStatusTracker);
		this.portalEventBus = checkNotNull(portalEventBus);
		this.deviceDBService = checkNotNull(deviceDBService);
		this.responseTrackerFactory = checkNotNull(responseTrackerFactory);
		this.rsHelper = checkNotNull(rsHelper);
		this.messageFactory = checkNotNull(messageFactory);
	}

	@Override
	protected void doStart() {
		try {
			log.trace("DefaultImagePluginImpl.doStart()");
			scheduler = newScheduledThreadPool(SCHEDULER_THREAD_POOL_SIZE, SCHEDULER_THREAD_FACTORY);
			schedule = scheduler.scheduleWithFixedDelay(defaultImageRunnable, 0, SCHEDULING_INTERVAL, MINUTES);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			log.trace("DefaultImagePluginImpl.doStop()");
			schedule.cancel(false);
			ExecutorUtils.shutdown(scheduler, 1, TimeUnit.SECONDS);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	private void flashDefaultImages() {

		try {

			final Set<NodeUrn> nodesToPotentiallyFlash = getUnreservedNodeUrnsWithoutDefaultImage();
			log.trace("nodesToPotentiallyFlash: {}", nodesToPotentiallyFlash);

			if (nodesToPotentiallyFlash.isEmpty()) {
				log.trace("All nodes are either reserved or already flashed with default image. Nothing to do.");
				return;
			}

			final Map<Set<NodeUrn>, byte[]> nodeUrnsToImageMapping = getNodesToImageMap(nodesToPotentiallyFlash);
			log.trace("nodeUrnsToImageMapping: {}", nodeUrnsToImageMapping);

			if (nodeUrnsToImageMapping.isEmpty()) {
				log.trace("No default images seem to be configured. Nothing to do.");
				return;
			}

			final List<ResponseTracker> responseTrackers = newArrayList();

			// fork
			for (Map.Entry<Set<NodeUrn>, byte[]> entry : nodeUrnsToImageMapping.entrySet()) {

				final Set<NodeUrn> nodeUrns = entry.getKey();
				final byte[] image = entry.getValue();
				FlashImagesRequest request = messageFactory.flashImagesRequest(empty(), empty(), nodeUrns, image);

				log.debug("Flashing {} with default image...", nodeUrns);
				final ResponseTracker responseTracker = responseTrackerFactory.create(
						request.getHeader(),
						portalEventBus
				);
				portalEventBus.post(request);

				responseTracker.addProgressListener(() -> {
					log.trace("Flashing progress: " + responseTracker.getProgress());
				}, MoreExecutors.directExecutor());

				responseTrackers.add(responseTracker);
			}

			// join
			for (ResponseTracker responseTracker : responseTrackers) {
				final Map<NodeUrn, Response> responseMap = responseTracker.get();
				for (Map.Entry<NodeUrn, Response> entry : responseMap.entrySet()) {

					// an error occurred
					if (entry.getValue().getStatusCode() < 0) {
						nodeStatusTracker.setFlashStatus(entry.getKey(), FlashStatus.UNKNOWN);
						log.trace("Flashing node \"{}\" failed: {}", entry.getKey(), entry.getValue());
					} else {
						nodeStatusTracker.setFlashStatus(entry.getKey(), FlashStatus.DEFAULT_IMAGE);
						log.trace("Flashing node \"{}\" worked just fine :)", entry.getKey(), entry.getValue());
					}
				}
			}

		} catch (Exception e) {
			log.error("Exception while flashing default images: ", e);
		}
	}

	private Set<NodeUrn> getUnreservedNodeUrnsWithoutDefaultImage() {

		final Set<NodeUrn> unknownImage = nodeStatusTracker.getNodes(FlashStatus.UNKNOWN);
		final Set<NodeUrn> userImage = nodeStatusTracker.getNodes(FlashStatus.USER_IMAGE);
		final Set<NodeUrn> unreserved = rsHelper.getUnreservedNodes(UNRESERVED_DURATION_THRESHOLD);

		return intersection(union(unknownImage, userImage), unreserved);
	}

	private Map<Set<NodeUrn>, byte[]> getNodesToImageMap(final Set<NodeUrn> potentialNodeUrns) throws IOException {
		final Map<NodeUrn, DeviceConfig> configsWithDefaultImage =
				filterValues(
						deviceDBService.getConfigsByNodeUrns(potentialNodeUrns),
						DEVICE_CONFIG_HAS_DEFAULT_IMAGE
				);

		final Multimap<URI, NodeUrn> imageUriToNodeUrnsMapping = HashMultimap.create();

		for (Map.Entry<NodeUrn, DeviceConfig> entry : configsWithDefaultImage.entrySet()) {

			final Map<String, String> nodeConfiguration = entry.getValue().getNodeConfiguration();
			final URI defaultImageUri = URI.create(nodeConfiguration.get(CONFIG_KEY_DEFAULT_IMAGE));
			imageUriToNodeUrnsMapping.put(defaultImageUri, entry.getKey());
		}

		log.trace("imageUriToNodeUrnsMapping: {}", imageUriToNodeUrnsMapping);

		final Map<Set<NodeUrn>, byte[]> nodeUrnsToImageMapping = newHashMap();

		for (URI uri : imageUriToNodeUrnsMapping.keySet()) {

			final Collection<NodeUrn> nodeUrns = imageUriToNodeUrnsMapping.get(uri);
			final byte[] imageBytes;

			if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {

				imageBytes = ByteStreams.toByteArray(uri.toURL().openStream());

			} else if ("file".equals(uri.getScheme())) {

				final File file = new File(uri.getSchemeSpecificPart());
				try {
					imageBytes = Files.toByteArray(file);
				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error("{} while reading default image from file \"{}\": {}",
								e.getClass(),
								file.getAbsolutePath(),
								getStackTraceAsString(e)
						);
					}
					continue;
				}

			} else {
				if (log.isErrorEnabled()) {
					log.error("URI \"{}\" scheme configured as default image for node "
									+ "\"{}\" is unsupported. Supported schemes: \"http\", \"https\" and \"file\""
					);
				}
				continue;
			}

			nodeUrnsToImageMapping.put(newHashSet(nodeUrns), imageBytes);
		}

		return nodeUrnsToImageMapping;
	}
}
