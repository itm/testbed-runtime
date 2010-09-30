package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.util.Collection;

import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.Transformer;
import de.itm.uniluebeck.tr.wiseml.merger.structures.Capability;
import de.itm.uniluebeck.tr.wiseml.merger.structures.NodeProperties;

public class NodePropertiesTransformer extends Transformer<NodeProperties> {

	private NodeProperties[] inputDefaultNodes;
	private NodeProperties outputDefaultNode;
	
	public NodePropertiesTransformer(
			final NodeProperties[] inputDefaultNodes,
			final NodeProperties outputDefaultNode) {
		this.inputDefaultNodes = inputDefaultNodes;
		this.outputDefaultNode = outputDefaultNode;
	}

	@Override
	public NodeProperties transform(
			final NodeProperties input, 
			final int inputIndex) {
		NodeProperties inputDefaultNode = inputDefaultNodes[inputIndex];
		
		// create blank output
		NodeProperties output = new NodeProperties();
		
		// apply default properties from original file
		if (inputDefaultNode != null) {
			addProperties(output, inputDefaultNode);
		}
		
		// overwrite with properties from node definition
		addProperties(output, input);
		
		// remove default properties
		if (outputDefaultNode != null) {
			removeEqualProperties(output, outputDefaultNode);
		}
		
		return output;
	}
	
	private static void addProperties(
			final NodeProperties dst, 
			final NodeProperties src) {
		if (src.getDescription() != null) {
			dst.setDescription(src.getDescription());
		}
		if (src.getGateway() != null) {
			dst.setGateway(src.getGateway());
		}
		if (src.getNodeType() != null) {
			dst.setNodeType(src.getNodeType());
		}
		if (src.getPosition() != null) {
			dst.setPosition(src.getPosition());
		}
		if (src.getProgramDetails() != null) {
			dst.setProgramDetails(src.getProgramDetails());
		}
		
		Collection<Capability> capabilities = src.getCapabilities();
		for (Capability capability : capabilities) {
			dst.setCapability(capability.getName(), capability);
		}
	}
	
	private static void removeEqualProperties(
			final NodeProperties dst, 
			final NodeProperties src) {
		if (equalsNonNull(src.getDescription(), dst.getDescription())) {
			src.setDescription(null);
		}
		if (equalsNonNull(src.getGateway(), dst.getGateway())) {
			src.setGateway(null);
		}
		if (equalsNonNull(src.getNodeType(), dst.getNodeType())) {
			src.setNodeType(null);
		}
		if (equalsNonNull(src.getPosition(), dst.getPosition())) {
			src.setPosition(null);
		}
		if (equalsNonNull(src.getProgramDetails(), dst.getProgramDetails())) {
			src.setProgramDetails(null);
		}
		
		Collection<Capability> srcCaps = dst.getCapabilities();
		for (Capability srcCap : srcCaps) {
			Capability dstCap = dst.getCapability(srcCap.getName());
			if (dstCap != null && srcCap.equals(dstCap)) {
				dst.setCapability(srcCap.getName(), null);
			}
		}
	}
	
	private static <T> boolean equalsNonNull(final T a, final T b) {
		if (a != null && b != null) {
			return a.equals(b);
		}
		return false;
	}

}
