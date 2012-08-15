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

package de.uniluebeck.itm.tr.iwsn.common;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import eu.wisebed.api.v3.common.Message;
import eu.wisebed.api.v3.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.v3.wsn.FlashProgramsConfiguration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;


public class WSNPreconditions {

	private CommonPreconditions commonPreconditions;

	public WSNPreconditions(Iterable<String> servedUrnPrefixes, Iterable<String> reservedNodeUrns) {
		this.commonPreconditions = new CommonPreconditions();
		this.commonPreconditions.addServedUrnPrefixes(servedUrnPrefixes);
		this.commonPreconditions.addKnownNodeUrns(reservedNodeUrns);
	}

	public void checkAreNodesAliveArguments(Collection<String> nodes) {
		commonPreconditions.checkNodesKnown(nodes);
	}

	public void checkFlashProgramsArguments(final List<FlashProgramsConfiguration> flashProgramsConfigurations) {

		checkNotNull(flashProgramsConfigurations);

		// check if there's at least on node to flash
		checkArgument(flashProgramsConfigurations.size() > 0);

		Set<String> nodeUrns = newHashSet();

		for (FlashProgramsConfiguration flashProgramsConfiguration : flashProgramsConfigurations) {

			commonPreconditions.checkNodesKnown(flashProgramsConfiguration.getNodeUrns());

			final Set<String> configNodeUrns = newHashSet(flashProgramsConfiguration.getNodeUrns());

			checkArgument(
					Sets.intersection(nodeUrns, configNodeUrns).isEmpty(),
					"Node URN sets of flashProgram configurations must be distinct!"
			);

			configNodeUrns.addAll(configNodeUrns);

			checkNotNull(
					flashProgramsConfiguration.getProgram(),
					"Image for node URNs " + Joiner.on(", ")
							.join(flashProgramsConfiguration.getNodeUrns()) + " is null!"
			);

			checkArgument(
					flashProgramsConfiguration.getProgram().length > 0,
					"Image for node URNs " + Joiner.on(", ")
							.join(flashProgramsConfiguration.getNodeUrns()) + " contains 0 bytes!"
			);
		}


	}

	public void checkSendArguments(List<String> nodeIds, Message message) {

		checkNotNull(nodeIds);
		checkNotNull(message);

		commonPreconditions.checkNodesKnown(nodeIds);

		checkNotNull(message.getSourceNodeUrn(), "Source node ID is missing.");
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
			throw new IllegalArgumentException(
					"The remoteServiceInstance argument (\"" + remoteServiceInstance + "\") is not a valid URL!"
			);
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

	public void checkSetChannelPipelineArguments(final List<String> nodes,
												 final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {

		checkNotNull(nodes);
		checkNotNull(channelHandlerConfigurations);

		commonPreconditions.checkNodesKnown(nodes);
	}
}