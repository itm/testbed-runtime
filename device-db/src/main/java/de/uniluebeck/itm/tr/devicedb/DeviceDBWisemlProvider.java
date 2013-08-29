package de.uniluebeck.itm.tr.devicedb;

import com.google.inject.Inject;
import de.uniluebeck.itm.tr.common.WisemlProvider;
import de.uniluebeck.itm.tr.common.WisemlProviderConfig;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.wiseml.Coordinate;
import eu.wisebed.wiseml.Interpolation;
import eu.wisebed.wiseml.Setup;
import eu.wisebed.wiseml.Wiseml;

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

		final Coordinate origin = new Coordinate();

		if (config.getOriginPhi() != null) {
			origin.setPhi(config.getOriginPhi());
		}
		if (config.getOriginTheta() != null) {
			origin.setTheta(config.getOriginTheta());
		}
		if (config.getOriginX() != null) {
			origin.setX(config.getOriginX());
		}
		if (config.getOriginZ() != null) {
			origin.setY(config.getOriginY());
		}
		if (config.getOriginZ() != null) {
			origin.setZ(config.getOriginZ());
		}

		setup.setOrigin(origin);

		final Wiseml wiseml = new Wiseml();
		wiseml.setSetup(setup);

		return wiseml;
	}
}
