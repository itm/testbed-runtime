package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import java.util.Collection;

import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.Transformer;
import de.itm.uniluebeck.tr.wiseml.merger.structures.Capability;
import de.itm.uniluebeck.tr.wiseml.merger.structures.LinkProperties;

public class LinkPropertiesTransformer extends Transformer<LinkProperties> {
	
	private LinkProperties[] inputDefaultLinks;
	private LinkProperties outputDefaultLink;
	
	public LinkPropertiesTransformer(
			final LinkProperties[] inputDefaultLinks,
			final LinkProperties outputDefaultLink) {
		this.inputDefaultLinks = inputDefaultLinks;
		this.outputDefaultLink = outputDefaultLink;
	}

	@Override
	public LinkProperties transform(
			final LinkProperties input, 
			final int inputIndex) {
		LinkProperties inputDefaultLink = inputDefaultLinks[inputIndex];
		
		// create blank output
		LinkProperties output = new LinkProperties();
		
		// apply default properties from original file
		if (inputDefaultLink != null) {
			addProperties(output, inputDefaultLink);
		}
		
		// overwrite with properties from node definition
		addProperties(output, input);
		
		// remove default properties
		if (outputDefaultLink != null) {
			removeEqualProperties(output, outputDefaultLink);
		}
		
		return output;
	}
	
	private static void addProperties(
			final LinkProperties dst, 
			final LinkProperties src) {
		if (src.getEncrypted() != null) {
			dst.setEncrypted(src.getEncrypted());
		}
		if (src.getVirtual() != null) {
			dst.setVirtual(src.getVirtual());
		}
		if (src.getRssi() != null) {
			dst.setRssi(src.getRssi());
		}
		
		Collection<Capability> capabilities = src.getCapabilities();
		for (Capability capability : capabilities) {
			dst.setCapability(capability.getName(), capability);
		}
	}
	
	private static void removeEqualProperties(
			final LinkProperties dst, 
			final LinkProperties src) {
		if (equalsNonNull(src.getEncrypted(), dst.getEncrypted())) {
			src.setEncrypted(null);
		}
		if (equalsNonNull(src.getVirtual(), dst.getVirtual())) {
			src.setVirtual(null);
		}
		if (equalsNonNull(src.getRssi(), dst.getRssi())) {
			src.setRssi(null);
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
