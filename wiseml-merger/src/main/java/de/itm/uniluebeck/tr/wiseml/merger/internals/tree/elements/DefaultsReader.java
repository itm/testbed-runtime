package de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLStructureReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.LinkProperties;
import de.itm.uniluebeck.tr.wiseml.merger.structures.NodeProperties;

public class DefaultsReader extends WiseMLStructureReader {

	public DefaultsReader(
			final WiseMLTreeReader parent, 
			final NodeProperties nodeProperties, 
			final LinkProperties linkProperties) {
		super(new Element(
				parent, 
				WiseMLTag.defaults, 
				null, 
				createSubElementsFromReaders(
						((nodeProperties != null)?
							new NodePropertiesReader(null, nodeProperties)
						:
							null),
						((linkProperties != null)?
							new LinkPropertiesReader(null, linkProperties)
						:
							null)),
				null));
	}

}
