package de.uniluebeck.itm.motelist;

import com.google.common.collect.Multimap;


public interface MoteList {

	/**
	 * Returns a {@link com.google.common.collect.Multimap} instance that contains a mapping from node types to a set of
	 * ports (e.g. /dev/ttyUSB0).
	 *
	 * @return mapping type -> {port}
	 */
	Multimap<MoteType, MoteData> getMoteList();

	/**
	 * Returns the port (e.g. /dev/ttyUSB0) of the a device of type {@code type} (e.g. iSense) that has the MAC-address
	 * {@code macAddress}. ATTENTION: currently this only works for iSense and Pacemate nodes. Using this method with a
	 * TelosB node will throw a {@link RuntimeException}.
	 *
	 * @param type	   the type of the node to find the port for
	 * @param macAddress the MAC address of the node to find the port for
	 *
	 * @return the motes port or {@code null} if none is found
	 */
	String getMotePort(MoteType type, long macAddress);

}
