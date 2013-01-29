package de.uniluebeck.itm.tr.iwsn.devicedb;

import eu.wisebed.wiseml.Setup;
import eu.wisebed.wiseml.Wiseml;

import java.util.List;

public abstract class WiseMLConverter {

	/**
	 * Formats the data from a given List of DeviceConfigs to a WiseML representation.
	 *
	 * @param configs An {@link Iterable} with {@link DeviceConfig}
	 * @return A WiseML object from
	 */
	public static Wiseml convertToWiseML(Iterable<DeviceConfig> configs) {

		Setup setup = new Setup();
		List<Setup.Node> nodes = setup.getNode();
		for ( DeviceConfig config : configs ) {
			Setup.Node node = new Setup.Node();

			node.setNodeType(config.getNodeType());
			node.setGateway(config.isGatewayNode());
			node.setId(config.getNodeUrn().toString());
			node.setDescription(config.getDescription());
			node.setPosition(config.getPosition());

			// TODO what's that?
			//node.getProgramDetails(config.getProgramDetails());
			// TODO capabilities

			nodes.add(node);
		}

		/* TODO how to set these properties?
		setup.setCoordinateType(coordinateType);
		setup.setDescription(description);
		setup.setInterpolation(interpolation);
		setup.setOrigin(origin);
		setup.setTimeinfo(timeinfo);
		*/

		Wiseml ml = new Wiseml();
		ml.setSetup(setup);

		return ml;
	}

}
