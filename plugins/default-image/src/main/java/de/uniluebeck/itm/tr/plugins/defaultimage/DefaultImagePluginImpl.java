package de.uniluebeck.itm.tr.plugins.defaultimage;

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTracker;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.messages.SingleNodeResponse;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.util.concurrent.ExecutorUtils;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.filterValues;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.union;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newFlashImagesRequest;

public class DefaultImagePluginImpl extends AbstractService implements DefaultImagePlugin {

	private static final Logger log = LoggerFactory.getLogger(DefaultImagePlugin.class);

	private static final String CONFIG_KEY_DEFAULT_IMAGE = "DefaultImagePlugin.defaultImage";

	private static final Predicate<DeviceConfig> DEVICE_CONFIG_HAS_DEFAULT_IMAGE = new Predicate<DeviceConfig>() {
		@Override
		public boolean apply(final DeviceConfig input) {
			return input.getNodeConfiguration().containsKey(CONFIG_KEY_DEFAULT_IMAGE);
		}
	};

	private final Random random = new Random();

	private final NodeStatusTracker nodeStatusTracker;

	private final PortalEventBus portalEventBus;

	private final DeviceDBService deviceDBService;

	private final ResponseTrackerFactory responseTrackerFactory;

	private final NodeStatusTrackerResourceService nodeStatusTrackerResourceService;

	private ScheduledExecutorService scheduler;

	private Runnable defaultImageRunnable = new Runnable() {
		@Override
		public void run() {

			try {
				nodeStatusTracker.run();
			} catch (Exception e) {
				log.error("Exception while updating NodeStatusTracker: ", e);
			}

			try {
				flashDefaultImages();
			} catch (Exception e) {
				log.error("Exception while flashing default images: ", e);
			}
		}
	};

	private ScheduledFuture<?> schedule;

	@Inject
	public DefaultImagePluginImpl(final NodeStatusTracker nodeStatusTracker,
								  final NodeStatusTrackerResourceService nodeStatusTrackerResourceService,
								  final PortalEventBus portalEventBus,
								  final DeviceDBService deviceDBService,
								  final ResponseTrackerFactory responseTrackerFactory) {
		this.nodeStatusTracker = checkNotNull(nodeStatusTracker);
		this.nodeStatusTrackerResourceService = checkNotNull(nodeStatusTrackerResourceService);
		this.portalEventBus = checkNotNull(portalEventBus);
		this.deviceDBService = checkNotNull(deviceDBService);
		this.responseTrackerFactory = checkNotNull(responseTrackerFactory);
	}

	@Override
	protected void doStart() {
		try {
			log.trace("DefaultImagePluginImpl.doStart()");
			nodeStatusTracker.startAndWait();
			nodeStatusTrackerResourceService.startAndWait();
			scheduler = Executors.newScheduledThreadPool(1,
					new ThreadFactoryBuilder().setNameFormat("DefaultImagePlugin %d").build()
			);
			schedule = scheduler.scheduleWithFixedDelay(defaultImageRunnable, 0, 1, TimeUnit.MINUTES);
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
			nodeStatusTrackerResourceService.stopAndWait();
			nodeStatusTracker.stopAndWait();
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	private void flashDefaultImages() {

		try {

			final Set<NodeUrn> nodesToPotentiallyFlash = getUnreservedNodeUrnsThatDoNotAlreadyRunDefaultImage();
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
				final Request request = newFlashImagesRequest(null, random.nextLong(), nodeUrns, image);

				log.debug("Flashing {} with default image...", nodeUrns);
				final ResponseTracker responseTracker = responseTrackerFactory.create(request, portalEventBus);
				portalEventBus.post(request);

				responseTracker.addProgressListener(new Runnable() {
					@Override
					public void run() {
						log.trace("Flashing progress: " + responseTracker.getProgress());
					}
				}, sameThreadExecutor()
				);

				responseTrackers.add(responseTracker);
			}

			// join
			for (ResponseTracker responseTracker : responseTrackers) {
				final Map<NodeUrn, SingleNodeResponse> responseMap = responseTracker.get();
				for (Map.Entry<NodeUrn, SingleNodeResponse> entry : responseMap.entrySet()) {

					// an error occurred
					if (entry.getValue().getStatusCode() < 0) {
						nodeStatusTracker.setFlashStatus(entry.getKey(), FlashStatus.UNKNOWN);
						log.trace("Flashing node \"{}\" did not work out: {}", entry.getKey(), entry.getValue());
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

	private Set<NodeUrn> getUnreservedNodeUrnsThatDoNotAlreadyRunDefaultImage() {

		final Set<NodeUrn> unknownUnreserved = nodeStatusTracker.getNodes(
				FlashStatus.UNKNOWN,
				ReservationStatus.UNRESERVED
		);

		final Set<NodeUrn> userImageUnreserved = nodeStatusTracker.getNodes(
				FlashStatus.USER_IMAGE,
				ReservationStatus.UNRESERVED
		);

		return union(unknownUnreserved, userImageUnreserved);
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
