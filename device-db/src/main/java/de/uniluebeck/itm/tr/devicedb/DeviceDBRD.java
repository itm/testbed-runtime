package de.uniluebeck.itm.tr.devicedb;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.DecoratedImpl;
import de.uniluebeck.itm.util.scheduler.SchedulerService;
import eu.smartsantander.rd.jaxb.ResourceDescription;
import eu.smartsantander.rd.rd3api.IRD3API;
import eu.smartsantander.rd.rd3api.QueryOptions;
import eu.smartsantander.rd.rd3api.RD3InteractorJ;
import eu.smartsantander.rd.rd3api.RDAPIException;
import eu.wisebed.api.v3.common.NodeUrn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;


public class DeviceDBRD extends AbstractService implements DeviceDBService {

	private static final Logger log = LoggerFactory.getLogger(DeviceDBRD.class);

	private final DeviceDBConfig deviceDBConfig;

	private final DeviceDBService deviceDBService;

	private final DeviceDBRDEventBrokerClient eventBrokerClient;

	private final SchedulerService schedulerService;

	private final Lock bootstrapScheduleLock = new ReentrantLock();

	private ScheduledFuture<?> bootstrapSchedule;

	private final Runnable bootstrapRunnable = new Runnable() {
		@Override
		public void run() {

			try {

				bootstrapResourcesFromRD();
				bootstrapScheduleLock.lock();
				try {
					bootstrapSchedule = null;
				} finally {
					bootstrapScheduleLock.unlock();
				}

			} catch (Exception e) {
				log.warn("Exception while bootstrapping DeviceDB resources from RD (trying again in 30 seconds): ", e);
				bootstrapScheduleLock.lock();
				try {
					bootstrapSchedule = schedulerService.schedule(this, 30, TimeUnit.SECONDS);
				} finally {
					bootstrapScheduleLock.unlock();
				}
				return;
			}

			eventBrokerClient.startAndWait();
		}
	};

	@Inject
	public DeviceDBRD(final DeviceDBConfig deviceDBConfig, @DecoratedImpl final DeviceDBService deviceDBService,
					  final DeviceDBRDEventBrokerClient eventBrokerClient,
					  final SchedulerService schedulerService) {
		this.deviceDBConfig = checkNotNull(deviceDBConfig);
		this.deviceDBService = checkNotNull(deviceDBService);
		this.eventBrokerClient = checkNotNull(eventBrokerClient);
		this.schedulerService = checkNotNull(schedulerService);
	}

	@Override
	public void add(final DeviceConfig deviceConfig) {
		deviceDBService.add(deviceConfig);
	}

	@Override
	public Iterable<DeviceConfig> getAll() {
		return deviceDBService.getAll();
	}

	@Override
	@Nullable
	public DeviceConfig getConfigByMacAddress(final long macAddress) {
		return deviceDBService.getConfigByMacAddress(macAddress);
	}

	@Override
	@Nullable
	public DeviceConfig getConfigByNodeUrn(final NodeUrn nodeUrn) {
		return deviceDBService.getConfigByNodeUrn(nodeUrn);
	}

	@Override
	@Nullable
	public DeviceConfig getConfigByUsbChipId(final String usbChipId) {
		return deviceDBService.getConfigByUsbChipId(usbChipId);
	}

	@Override
	public Map<NodeUrn, DeviceConfig> getConfigsByNodeUrns(
			final Iterable<NodeUrn> nodeUrns) {
		return deviceDBService.getConfigsByNodeUrns(nodeUrns);
	}

	@Override
	public void removeAll() {
		deviceDBService.removeAll();
	}

	@Override
	public boolean removeByNodeUrn(final NodeUrn nodeUrn) {
		return deviceDBService.removeByNodeUrn(nodeUrn);
	}

	@Override
	public void update(final DeviceConfig deviceConfig) {
		deviceDBService.update(deviceConfig);
	}

	@Override
	protected void doStart() {

		try {

			schedulerService.startAndWait();
			deviceDBService.startAndWait();
			schedulerService.execute(bootstrapRunnable);
			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		try {

			bootstrapScheduleLock.lock();
			try {
				if (bootstrapSchedule != null) {
					bootstrapSchedule.cancel(true);
					bootstrapSchedule = null;
				}
			} finally {
				bootstrapScheduleLock.unlock();
			}

			eventBrokerClient.stopAndWait();
			deviceDBService.stopAndWait();
			schedulerService.stopAndWait();
			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@SuppressWarnings("unchecked")
	private void bootstrapResourcesFromRD() throws RDAPIException, JAXBException {

		final String rdURL = deviceDBConfig.getSmartSantanderRDUri().toString();
		final IRD3API rd = RD3InteractorJ.getInstance(rdURL.endsWith("/") ? rdURL : rdURL + "/");
		final Optional<QueryOptions> options = Optional.absent();
		final List<ResourceDescription> resources = rd.getTestbedResourcesURN(
				deviceDBConfig.getSmartSantanderRDPortalId(),
				options
		);

		for (ResourceDescription res : resources) {

			if (log.isTraceEnabled()) {

				final StringWriter writer = new StringWriter();
				final JAXBContext jc = JAXBContext.newInstance(ResourceDescription.class.getPackage().getName());
				final Marshaller m = jc.createMarshaller();

				m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				m.marshal(
						new JAXBElement(new QName("", "resource-description"), ResourceDescription.class, res),
						writer
				);

				log.trace("{}", writer.toString());
			}

			try {
				final DeviceConfig deviceConfig = DeviceDBRDHelper.deviceConfigFromRDResource(res);
				if (deviceConfig != null) {
					this.add(deviceConfig);
				}
			} catch (Exception e) {
				log.error("Error while trying to create a device configuration based on" +
						" information fetched from the RD: ", e
				);
			}
		}
	}
}
