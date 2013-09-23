package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.WisemlProvider;
import de.uniluebeck.itm.tr.common.WisemlProviderConfig;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.*;

import java.util.List;

public class DeviceDBWisemlProvider implements WisemlProvider {

	private final DeviceDBService deviceDBService;

	private final WisemlProviderConfig config;

	@Inject
	public DeviceDBWisemlProvider(final DeviceDBService deviceDBService,
								  final WisemlProviderConfig config) {
		this.deviceDBService = deviceDBService;
		this.config = config;
	}

	@Override
	public Wiseml get() {
		return convertDeviceConfigToWiseml(deviceDBService.getAll());
	}

	@Override
	public Wiseml get(final Iterable<NodeUrn> nodeUrns) {
		return convertDeviceConfigToWiseml(deviceDBService.getConfigsByNodeUrns(nodeUrns).values());
	}

	private Wiseml convertDeviceConfigToWiseml(final Iterable<DeviceConfig> configs) {

		final Setup setup = new Setup();
		final List<Setup.Node> nodes = setup.getNode();

		for (final DeviceConfig config : configs) {

			final Setup.Node node = new Setup.Node();

			node.setNodeType(config.getNodeType());
			node.setGateway(config.isGatewayNode());
			node.setId(config.getNodeUrn().toString());
			node.setDescription(config.getDescription());
			node.setPosition(config.getPosition());

			if (config.getCapabilities() != null) {
				node.getCapability().addAll(config.getCapabilities());
			}

			nodes.add(node);
		}

		setup.setCoordinateType(config.getCoordinateType());
		setup.setDescription(config.getDescription());

		if (config.getInterpolation() != null && !"".equals(config.getInterpolation())) {
			setup.setInterpolation(Interpolation.fromValue(config.getInterpolation()));
		}

		final OutdoorCoordinatesType origin = new OutdoorCoordinatesType();
		origin.setLatitude(config.getOriginLatitude());
		origin.setLongitude(config.getOriginLongitude());
		origin.setPhi(config.getOriginPhi());
		origin.setTheta(config.getOriginTheta());
		origin.setRho(config.getOriginRho());
		origin.setX(config.getOriginX());
		origin.setY(config.getOriginY());
		origin.setZ(config.getOriginZ());

		final Coordinate originCoordinate = new Coordinate();
		originCoordinate.setType(CoordinateType.OUTDOOR);
		originCoordinate.setOutdoorCoordinates(origin);
		setup.setOrigin(originCoordinate);

		final Wiseml wiseml = new Wiseml();
		wiseml.setSetup(setup);

		return wiseml;
	}
}
