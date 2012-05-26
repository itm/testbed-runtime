/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.gtr.routing;

import com.google.common.collect.ImmutableMap;
import de.uniluebeck.itm.tr.util.Service;

/**
 * A simple service that provides a routing table that allows to get the name of the next hop on
 * the way to the destination node.
 */
public interface RoutingTableService extends Service {

	/**
	 * Returns the node name of the next hop on the way to {@code destinationNodeName}.
	 *
	 * @param destinationNodeName the name of the destination node
	 * @return the node name of the next hop or {@code null} if there's no route to {@code destinationNodeName}
	 */
	String getNextHop(String destinationNodeName);

	/**
	 * Sets the next hop node name for the destination node name.
	 *
	 * @param destinationNodeName the name of the destination node
	 * @param nextHopNodeName	 the name of the next hop for the destination node name
	 */
	void setNextHop(String destinationNodeName, String nextHopNodeName);

	/**
	 * Removes the entry for the destination node named {@code destinationNodeName}.
	 *
	 * @param destinationNodeName the name of the destination node who's entry is to be removed
	 */
	void removeNextHop(String destinationNodeName);

	/**
	 * Returns a map containing all entries of the routing table as that maps destination node name to next hop node
	 * name.
	 *
	 * @return a map containing all entries of the routing table as that maps destination node name to next hop node
	 *         name
	 */
	ImmutableMap<String, String> getEntries();
}
