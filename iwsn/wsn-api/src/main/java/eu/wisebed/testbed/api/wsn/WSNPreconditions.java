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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import eu.wisebed.api.common.Message;
import eu.wisebed.api.wsn.Program;


public class WSNPreconditions {

	private CommonPreconditions commonPreconditions;

	public WSNPreconditions() {
		this.commonPreconditions = new CommonPreconditions();
	}

	public void addKnownNodeUrns(String... reservedNodeUrns) {
		commonPreconditions.addKnownNodeUrns(reservedNodeUrns);
	}

	public void addServedUrnPrefixes(String... servedUrnPrefixes) {
		commonPreconditions.addServedUrnPrefixes(servedUrnPrefixes);
	}

	public void checkAreNodesAliveArguments(Collection<String> nodes) {
		commonPreconditions.checkNodesKnown(nodes);
	}

	public void checkFlashProgramsArguments(List<String> nodeIds, List<Integer> programIndices,
											List<Program> programs) {

		checkNotNull(nodeIds);
		checkNotNull(programIndices);
		checkNotNull(programs);

		// check if there's at least on node to flash
		checkArgument(nodeIds.size() > 0);

		commonPreconditions.checkNodesKnown(nodeIds);

		// check if for every node there's a corresponding program index and the program exists
		checkArgument(nodeIds.size() == programIndices.size(),
				"For every node URN there must be a corresponding program index."
		);

		for (int i = 0; i < programIndices.size(); i++) {
			checkArgument(programIndices.get(i) < programs.size(), "There is no program for index %s for node URN %s.",
					i, nodeIds.get(i)
			);
		}

	}

	public void checkSendArguments(List<String> nodeIds, Message message) {

		checkNotNull(nodeIds);
		checkNotNull(message);

		commonPreconditions.checkNodesKnown(nodeIds);

		checkNotNull(message.getSourceNodeId(), "Source node ID is missing.");
		checkArgument(message.getBinaryData() != null, "A message to a node must contain binary data.");

	}

	public void checkResetNodesArguments(List<String> nodes) {
		checkNotNull(nodes);
		commonPreconditions.checkNodesKnown(nodes);
	}

	@SuppressWarnings("unused")
	public void checkSetVirtualLinkArguments(String sourceNode, String targetNode, String remoteServiceInstance,
											 List<String> parameters, List<String> filters) {

		checkNotNull(sourceNode);
		checkNotNull(targetNode);
		checkNotNull(remoteServiceInstance);

		commonPreconditions.checkNodesKnown(sourceNode);

		try {
			new URL(remoteServiceInstance);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("The remoteServiceInstance argument (\""+remoteServiceInstance+"\") is not a valid URL!");
		}

	}

	public void checkDestroyVirtualLinkArguments(String sourceNode, String targetNode) {

		checkNotNull(sourceNode);
		checkNotNull(targetNode);

		commonPreconditions.checkNodeUrnsPrefixesServed(sourceNode);

	}

	public void checkDisableNodeArguments(String node) {

		checkNotNull(node);

		commonPreconditions.checkNodesKnown(node);
	}

	public void checkDisablePhysicalLinkArguments(String nodeA, String nodeB) {

		checkNotNull(nodeA);
		checkNotNull(nodeB);

		commonPreconditions.checkNodesKnown(nodeA, nodeB);
	}

	public void checkEnableNodeArguments(String node) {

		checkNotNull(node);

		commonPreconditions.checkNodesKnown(node);
	}

	public void checkEnablePhysicalLinkArguments(String nodeA, String nodeB) {

		checkNotNull(nodeA);
		checkNotNull(nodeB);

		commonPreconditions.checkNodesKnown(nodeA, nodeB);
	}

}
