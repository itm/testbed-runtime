package de.uniluebeck.itm.tr.plugins.defaultimage;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.uniluebeck.itm.tr.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.devicedb.DeviceDBService;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTracker;
import de.uniluebeck.itm.tr.iwsn.common.ResponseTrackerFactory;
import de.uniluebeck.itm.tr.iwsn.messages.Request;
import de.uniluebeck.itm.tr.iwsn.portal.PortalEventBus;
import de.uniluebeck.itm.util.concurrent.ExecutorUtils;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.rs.PublicReservationData;
import eu.wisebed.api.v3.rs.RS;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.devicedb.DeviceConfig.TO_NODE_URN_FUNCTION;
import static de.uniluebeck.itm.tr.iwsn.messages.MessagesHelper.newFlashImagesRequest;

public class DefaultImagePluginImpl extends AbstractService implements DefaultImagePlugin {

	private final Random random = new Random();

	private final RS rs;

	private final PortalEventBus portalEventBus;

	private final DeviceDBService deviceDBService;

	private final ResponseTrackerFactory responseTrackerFactory;

	private ScheduledExecutorService scheduler;

	private Runnable defaultImageRunnable = new Runnable() {
		@Override
		public void run() {
			try {

				final Set<NodeUrn> nextHourReservedNodeUrns = newHashSet();
				final List<PublicReservationData> nextHourReservations =
						rs.getReservations(DateTime.now(), DateTime.now().plusHours(1), null, null);
				for (PublicReservationData reservation : nextHourReservations) {
					nextHourReservedNodeUrns.addAll(reservation.getNodeUrns());
				}

				final Set<DeviceConfig> allDeviceConfigs = newHashSet(deviceDBService.getAll());
				final Set<NodeUrn> allNodeUrns = newHashSet(transform(allDeviceConfigs, TO_NODE_URN_FUNCTION));

				final Set<NodeUrn> nextHourUnreservedNodeUrns = difference(allNodeUrns, nextHourReservedNodeUrns);

				final Request request = newFlashImagesRequest(
						null,
						random.nextLong(),
						nextHourUnreservedNodeUrns,
						new byte[]{}
				);

				final ResponseTracker responseTracker = responseTrackerFactory.create(request, portalEventBus);
				portalEventBus.post(request);

				responseTracker.addProgressListener(new Runnable() {
					@Override
					public void run() {
						System.out.println("Flashing progress: " + responseTracker.getProgress());
					}
				}, MoreExecutors.sameThreadExecutor());

				responseTracker.get(2, TimeUnit.MINUTES);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private ScheduledFuture<?> schedule;

	public DefaultImagePluginImpl(final RS rs,
								  final PortalEventBus portalEventBus,
								  final DeviceDBService deviceDBService,
								  final ResponseTrackerFactory responseTrackerFactory) {
		this.rs = rs;
		this.portalEventBus = portalEventBus;
		this.deviceDBService = deviceDBService;
		this.responseTrackerFactory = responseTrackerFactory;
	}

	@Override
	protected void doStart() {
		try {
			System.out.println("DefaultImagePluginImpl.doStart()");
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
			System.out.println("DefaultImagePluginImpl.doStop()");
			schedule.cancel(false);
			ExecutorUtils.shutdown(scheduler, 1, TimeUnit.SECONDS);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
