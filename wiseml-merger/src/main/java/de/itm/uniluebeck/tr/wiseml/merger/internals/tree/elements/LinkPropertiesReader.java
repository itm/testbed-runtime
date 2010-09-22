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
import de.itm.uniluebeck.tr.wiseml.merger.structures.LinkProperties;
import de.itm.uniluebeck.tr.wiseml.merger.structures.RSSI;

public class LinkPropertiesReader extends WiseMLStructureReader {

	public LinkPropertiesReader(
			final WiseMLTreeReader parent, 
			final LinkProperties linkProperties) {
		super(new Element(
				parent, 
				WiseMLTag.link, 
				null,
				createSubElementsFromProperties(linkProperties), 
				null));
	}
	
	public LinkPropertiesReader(
			final WiseMLTreeReader parent, 
			final String source,
			final String target,
			final LinkProperties linkProperties) {
		super(new Element(
				parent, 
				WiseMLTag.link, 
				new WiseMLAttribute[]{
						new WiseMLAttribute("source", source),
						new WiseMLAttribute("target", target)},
				createSubElementsFromProperties(linkProperties),
				null));
	}

	private static Element[] createSubElementsFromProperties(
			LinkProperties linkProperties) {
		List<Element> result = new ArrayList<Element>();
		if (linkProperties.getEncrypted() != null) {
			result.add(createPureTextElement(null, WiseMLTag.encrypted, 
					Conversions.writeBoolean(linkProperties.getEncrypted())));
		}
		if (linkProperties.getVirtual() != null) {
			result.add(createPureTextElement(null, WiseMLTag.virtual,
					Conversions.writeBoolean(linkProperties.getVirtual())));
		}
		if (linkProperties.getRssi() != null) {
			RSSI rssi = linkProperties.getRssi();
			result.add(new Element(null, WiseMLTag.rssi, 
					new WiseMLAttribute[]{
						new WiseMLAttribute(
								"datatype", rssi.getDataType().toString()),
						new WiseMLAttribute("unit", rssi.getUnit().toString()),
						new WiseMLAttribute("default", rssi.getDefaultValue()),
			}, null, null));
		}
		Collection<Capability> capabilities = linkProperties.getCapabilities();
		for (Capability capability : capabilities) {
			result.add(new CapabilityReader(null, capability).getTopElement());
		}
		return result.toArray(new Element[result.size()]);
	}

}
