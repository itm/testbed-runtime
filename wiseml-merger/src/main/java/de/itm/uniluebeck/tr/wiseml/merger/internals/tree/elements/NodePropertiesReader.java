package de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.itm.uniluebeck.tr.wiseml.merger.internals.Conversions;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLAttribute;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLStructureReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.Capability;
import de.itm.uniluebeck.tr.wiseml.merger.structures.NodeProperties;

public class NodePropertiesReader extends WiseMLStructureReader {

	public NodePropertiesReader(
			final WiseMLTreeReader parent, 
			final NodeProperties nodeProperties) {
		super(new Element(
				parent, 
				WiseMLTag.node, 
				null,
				createSubElementsFromProperties(nodeProperties), 
				null));
	}
	
	public NodePropertiesReader(
			final WiseMLTreeReader parent, 
			final String nodeID, 
			final NodeProperties nodeProperties) {
		super(new Element(
				parent, 
				WiseMLTag.node, 
				new WiseMLAttribute[]{new WiseMLAttribute("id", nodeID)},
				createSubElementsFromProperties(nodeProperties),
				null));
	}

	private static Element[] createSubElementsFromProperties(
			NodeProperties nodeProperties) {
		List<Element> result = new ArrayList<Element>();
		if (nodeProperties.getPosition() != null) {
			result.add(new CoordinateReader(
					null, WiseMLTag.position, 
					nodeProperties.getPosition()).getTopElement());
		}
		if (nodeProperties.getGateway() != null) {
			result.add(createPureTextElement(
					null, WiseMLTag.gateway, 
					Conversions.writeBoolean(nodeProperties.getGateway())));
		}
		if (nodeProperties.getProgramDetails() != null) {
			result.add(createPureTextElement(
					null, WiseMLTag.programDetails,
					nodeProperties.getProgramDetails()));
		}
		if (nodeProperties.getNodeType() != null) {
			result.add(createPureTextElement(
					null, WiseMLTag.nodeType,
					nodeProperties.getNodeType()));
		}
		if (nodeProperties.getDescription() != null) {
			result.add(createPureTextElement(
					null, WiseMLTag.description,
					nodeProperties.getDescription()));
		}
		Collection<Capability> capabilities = nodeProperties.getCapabilities();
		for (Capability capability : capabilities) {
			result.add(new CapabilityReader(null, capability).getTopElement());
		}
		return result.toArray(new Element[result.size()]);
	}

}
