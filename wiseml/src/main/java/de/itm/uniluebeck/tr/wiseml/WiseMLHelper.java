package de.itm.uniluebeck.tr.wiseml;

import com.google.common.collect.Lists;
import eu.wisebed.ns.wiseml._1.Setup;
import eu.wisebed.ns.wiseml._1.Wiseml;

import javax.xml.bind.JAXB;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;


public class WiseMLHelper {

	private WiseMLHelper() {
	}

	/**
	 * Parses the WiseML document that is passed in as String in {@code wiseML} and reads out all node URNs that are
	 * contained in the setup-part of the document.
	 *
	 * @param serializedWiseML a serialized WiseML document
	 *
	 * @return a List of node URNs
	 */
	public static List<String> getNodeUrns(String serializedWiseML) {
		return getNodeUrns(serializedWiseML, (String[]) null);
	}

	/**
	 * Parses the WiseML document that is passed in as String in {@code wiseML} and reads out all node URNs that are
	 * contained in the setup-part of the document.
	 *
	 * @param serializedWiseML a serialized WiseML document
	 * @param types			node types to include, e.g. "isense", "telosb" will include all iSense and all TelosB motes
	 *                         contained in the WiseML document
	 *
	 * @return a List of node URNs
	 */
	public static List<String> getNodeUrns(String serializedWiseML, String... types) {

		List<String> nodeTypes = types == null ? null : Lists.newArrayList(types);
		List<String> nodeUrns = new LinkedList<String>();

		Wiseml wiseml = deserialize(serializedWiseML);

		for (Setup.Node node : wiseml.getSetup().getNode()) {
			if (types == null || types.length == 0 || nodeTypes.contains(node.getNodeType())) {
				nodeUrns.add(node.getId());
			}
		}

		return nodeUrns;

	}

	public static Wiseml deserialize(String serializedWiseML) {
		return JAXB.unmarshal(new StringReader(serializedWiseML), Wiseml.class);
	}

	public static String serialize(Wiseml wiseML) {
		StringWriter writer = new StringWriter();
		JAXB.marshal(wiseML, writer);
		return writer.toString();
	}

	public static String prettyPrintWiseML(String serializedWiseML) {
		return serialize(deserialize(serializedWiseML));
	}

}
