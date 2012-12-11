package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfig;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfigDB;
import de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceEvent;
import de.uniluebeck.itm.wsn.deviceutils.observer.DeviceInfo;
import de.uniluebeck.itm.wsn.drivers.core.Device;
import de.uniluebeck.itm.wsn.drivers.core.MacAddress;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceType;
import eu.wisebed.api.v3.common.NodeUrn;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;

public class GatewayDeviceManagerImpl extends AbstractService implements GatewayDeviceManager {

	private static final Logger log = LoggerFactory.getLogger(GatewayDeviceManager.class);

	private final Map<NodeUrn, GatewayDevice> devices = newHashMap();

	private final GatewayEventBus gatewayEventBus;

	private final DeviceConfigDB deviceConfigDB;

	private final GatewayDeviceFactory gatewayDeviceFactory;

	private final DeviceFactory deviceFactory;

	private ExecutorService deviceDriverExecutorService;

	@Inject
	public GatewayDeviceManagerImpl(final GatewayEventBus gatewayEventBus,
									final DeviceConfigDB deviceConfigDB,
									final GatewayDeviceFactory gatewayDeviceFactory,
									final DeviceFactory deviceFactory) {

		this.gatewayEventBus = checkNotNull(gatewayEventBus);
		this.deviceConfigDB = checkNotNull(deviceConfigDB);
		this.gatewayDeviceFactory = checkNotNull(gatewayDeviceFactory);
		this.deviceFactory = checkNotNull(deviceFactory);
	}

	@Override
	protected void doStart() {
		try {
			final ThreadFactory tf = new ThreadFactoryBuilder().setNameFormat("DeviceDriverExecutor %d").build();
			deviceDriverExecutorService = Executors.newCachedThreadPool(tf);
			gatewayEventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			shutdownAllDevices();
			ExecutorUtils.shutdown(deviceDriverExecutorService, 10, TimeUnit.SECONDS);
			gatewayEventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onDeviceEvent(final DeviceEvent event) {
		switch (event.getType()) {
			case ATTACHED:
				onDeviceAttached(event.getDeviceInfo());
				break;
			case REMOVED:
				onDeviceDetached(event.getDeviceInfo());
				break;
		}
	}

	@Subscribe
	public synchronized void onDeviceObserverEvent(DeviceEvent deviceEvent) {

		String nodeMacAddressString = StringUtils.getUrnSuffix(configuration.getNodeUrn());
		MacAddress nodeMacAddress = new MacAddress(nodeMacAddressString);

		final MacAddress macAddress = deviceEvent.getDeviceInfo().getMacAddress();
		boolean eventHasSameMac = nodeMacAddress.equals(macAddress);
		final String nodeUSBChipID = configuration.getNodeUSBChipID();
		boolean eventHasSameUSBChipId = nodeUSBChipID != null &&
				nodeUSBChipID.equals(deviceEvent.getDeviceInfo().getReference());

		if (eventHasSameMac || eventHasSameUSBChipId) {

			log.info("{} => Received {}", configuration.getNodeUrn(), deviceEvent);

			switch (deviceEvent.getType()) {
				case ATTACHED:
					if (!isConnected()) {
						tryToConnect(
								deviceEvent.getDeviceInfo().getType(),
								deviceEvent.getDeviceInfo().getPort(),
								configuration.getNodeConfiguration()
						);
					}
					break;
				case REMOVED:
					sendNotification(configuration.getNodeUrn(),
							"Device " + configuration.getNodeUrn() + " was detached from the gateway."
					);
					disconnect();
					break;
			}
		}

	}

	private final Runnable assureConnectivityRunnable = new Runnable() {
		@Override
		public void run() {

			if (isConnected()) {
				return;
			}

			String nodeType = configuration.getNodeType();
			String nodeSerialInterface = configuration.getNodeSerialInterface();
			String nodeUrn = configuration.getNodeUrn();
			Map<String, String> nodeConfiguration = configuration.getNodeConfiguration();

			boolean isMockDevice = DeviceType.MOCK.toString().equalsIgnoreCase(nodeType);
			boolean hasSerialInterface = nodeSerialInterface != null;

			if (isMockDevice || hasSerialInterface) {

				if (!tryToConnect(nodeType, nodeSerialInterface, nodeConfiguration)) {
					log.warn("{} => Unable to connect to {} device at {}. Retrying in 30 seconds.",
							nodeUrn, nodeType, nodeSerialInterface
					);
				}

			} else {

				DeviceType deviceType = DeviceType.fromString(nodeType);
				MacAddress macAddress = new MacAddress(StringUtils.getUrnSuffix(nodeUrn));
				String nodeUSBChipID = configuration.getNodeUSBChipID();

				GatewayDeviceObserverRequest
						deviceRequest = new GatewayDeviceObserverRequest(deviceType, macAddress, nodeUSBChipID);

				deviceObserverEventBus.post(deviceRequest);

				if (deviceRequest.getResponse() != null && deviceRequest.getResponse().getMacAddress() != null) {
					try {
						tryToConnect(
								deviceRequest.getResponse().getType(),
								deviceRequest.getResponse().getPort(),
								configuration.getNodeConfiguration()
						);
					} catch (Exception e) {
						log.warn("{} => {}. Retrying in 30 seconds at the same serial interface.",
								configuration.getNodeUrn(),
								e.getMessage()
						);
					}
				} else {
					log.warn("{} => Could not retrieve serial interface from device observer for device. Retrying "
							+ "again in 30 seconds.",
							configuration.getNodeUrn()
					);
				}
			}
		}
	};

	private void onDeviceAttached(final DeviceInfo deviceInfo) {

		DeviceConfig deviceConfig = null;
		if (deviceInfo.getMacAddress() != null) {
			deviceConfig = deviceConfigDB.getByMacAddress(deviceInfo.getMacAddress());
		} else if (deviceInfo.getReference() != null) {
			deviceConfig = deviceConfigDB.getByUsbChipId(deviceInfo.getReference());
		}

		if (deviceConfig == null) {
			log.warn("Ignoring unknown device: {}", deviceInfo);
			return;
		}

		final NodeUrn nodeUrn = new NodeUrn(deviceConfig.getNodeUrn());

		synchronized (devices) {

			if (!devices.containsKey(nodeUrn)) {

				try {

					final Device device = deviceFactory.create(
							deviceDriverExecutorService,
							deviceConfig.getNodeType(),
							deviceConfig.getNodeConfiguration()
					);

					device.connect(deviceInfo.getPort());

					log.info("{} => Successfully connected to {} device on serial port {}",
							nodeUrn, deviceConfig.getNodeType(), deviceInfo.getPort()
					);

					final GatewayDevice gatewayDevice = gatewayDeviceFactory.create(deviceConfig, device);
					devices.put(nodeUrn, gatewayDevice);

					final NotificationEvent notificationEvent = NotificationEvent.newBuilder()
							.setNodeUrn(nodeUrn.toString())
							.setTimestamp(new DateTime().getMillis())
							.setMessage("Device " + nodeUrn + " was attached to the gateway.")
							.build();

					gatewayEventBus.post(notificationEvent);

				} catch (Exception e) {
					log.error("{} => Could not connect to {} device at {}.", e);
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void onDeviceDetached(final DeviceInfo deviceInfo) {
		// TODO implement
	}
}
