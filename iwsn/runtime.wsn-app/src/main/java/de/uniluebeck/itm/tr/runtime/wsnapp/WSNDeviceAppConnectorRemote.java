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

package de.uniluebeck.itm.tr.runtime.wsnapp;

import de.uniluebeck.itm.tr.util.AbstractListenable;
import de.uniluebeck.itm.gtr.common.SchedulerService;
import de.uniluebeck.itm.wsn.devicedrivers.generic.MessagePacket;


public class WSNDeviceAppConnectorRemote extends AbstractListenable<WSNDeviceAppConnector.NodeOutputListener>
		implements WSNDeviceAppConnector {

	private final String nodeUrn;

	private final String nodeType;

	private final Integer nodeAPITimeout;

	private final SchedulerService schedulerService;

	private final Integer timeoutReset;

	private final Integer timeoutFlash;

	public WSNDeviceAppConnectorRemote(final String nodeUrn, final String nodeType, final Integer nodeAPITimeout,
									   final SchedulerService schedulerService, final Integer timeoutReset,
									   final Integer timeoutFlash) {
		this.nodeUrn = nodeUrn;
		this.nodeType = nodeType;
		this.nodeAPITimeout = nodeAPITimeout;
		this.schedulerService = schedulerService;
		this.timeoutReset = timeoutReset;
		this.timeoutFlash = timeoutFlash;
	}

	@Override
	public void enablePhysicalLink(final long nodeB, final Callback listener) {
		// TODO implement
	}

	@Override
	public void disablePhysicalLink(final long nodeB, final Callback listener) {
		// TODO implement
	}

	@Override
	public void enableNode(final Callback listener) {
		// TODO implement
	}

	@Override
	public void disableNode(final Callback listener) {
		// TODO implement
	}

	@Override
	public void destroyVirtualLink(final long targetNode, final Callback listener) {
		// TODO implement
	}

	@Override
	public void setVirtualLink(final long targetNode, final Callback listener) {
		// TODO implement
	}

	@Override
	public void sendMessage(final byte[] binaryMessage, final Callback listener) {
		// TODO implement
	}

	@Override
	public void resetNode(final Callback listener) {
		// TODO implement
	}

	@Override
	public void isNodeAlive(final Callback listener) {
		// TODO implement
	}

	@Override
	public void flashProgram(final WSNAppMessages.Program program, final FlashProgramCallback listener) {
		// TODO implement
	}

	@Override
	public void start() throws Exception {
		// TODO implement
	}

	@Override
	public void stop() {
		// TODO implement
	}
}
