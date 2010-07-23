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
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package eu.wisebed.testbed.api.wsn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public class CommonPreconditions {
	private static final Logger log = LoggerFactory.getLogger(CommonPreconditions.class);

	private final Set<String> urnPrefixes = new HashSet<String>();

	public void addServedUrnPrefixes(String... servedUrnPrefixes) {

		for (String servedUrnPrefix : servedUrnPrefixes) {
			urnPrefixes.add(servedUrnPrefix);
		}

	}

	public void checkNodesServed(String... nodes) {
		boolean match;
		for (String node : nodes) {
			match = false;
			for (String urnPrefix : urnPrefixes) {
				checkNotNull(node, "A node supplied in the list of nodes is null");
				if (node.startsWith(urnPrefix)) {
					match = true;
					break;
				}
			}
			if (!match) {
				throw new IllegalArgumentException(
						"The node URN " + node + " is not served by this instance. Valid prefixes are: " + urnPrefixes
				);
			}
		}
		// TODO check if nodes are known
	}

	public void checkNodesServed(Collection<String> nodes) {
		checkNodesServed(nodes.toArray(new String[nodes.size()]));
		// TODO check if nodes are known
	}

	public void checkUrnPrefixesServed(Set<String> urnPrefixes) {
		checkArgument(this.urnPrefixes.containsAll(urnPrefixes),
				"One of the URN prefixes is not served by this instance!"
		);
	}

	public static void main(String[] args) {
		CommonPreconditions preconditions = new CommonPreconditions();
		preconditions.addServedUrnPrefixes("urn:wisebed:tubs:");
		preconditions.checkNodesServed(Arrays.asList("urn:wisebed:tubs:419"));
	}

	//check current reserved nodes from rs on nodeNames
	public void checkNodesReserved(List<String> nodeNames, Set<String> reservedNodes) {
		if (reservedNodes == null) return;
		for (String nodeName : nodeNames) {
			if (!reservedNodes.contains(nodeName))
				log.error("Tried to send Message to Node " + nodeName + "but Node not reserved! Execution aborted!");
		}

	}

	//check current reserved nodes from rs on NodeName
	public void checkNodeReserved(String nodeName, Set<String> reservedNodes) {
		if (reservedNodes == null) return;
		if (!reservedNodes.contains(nodeName))
			log.error("Tried to send Message to Node " + nodeName + "but Node not reserved! Execution aborted!");

	}


}
