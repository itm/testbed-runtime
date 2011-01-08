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

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.internal.Nullable;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import de.uniluebeck.itm.gtr.TestbedRuntime;


class WSNDeviceAppModule extends AbstractModule {

	static final String NAME_NODE_URN = "de.uniluebeck.itm.tr.runtime.wsnapp.WSNDeviceAppModule/NAME_NODE_URN";

	static final String NAME_NODE_TYPE = "de.uniluebeck.itm.tr.runtime.wsnapp.WSNDeviceAppModule/NAME_NODE_TYPE";

	static final String NAME_USB_CHIP_ID = "de.uniluebeck.itm.tr.runtime.wsnapp.WSNDeviceAppModule/NAME_USB_CHIP_ID";

	static final String NAME_SERIAL_INTERFACE =
			"de.uniluebeck.itm.tr.runtime.wsnapp.WSNDeviceAppModule/NAME_SERIAL_INTERFACE";

	static final String NAME_NODE_API_TIMEOUT =
			"de.uniluebeck.itm.tr.runtime.wsnapp.WSNDeviceAppModule/NAME_NODE_API_TIMEOUT";

	private String nodeUrn;

	private String type;

	private String serialInterface;

	private TestbedRuntime testbedRuntime;

	private Integer nodeAPITimeout;

	private String nodeUSBChipID;

	public WSNDeviceAppModule(String nodeUrn,
							  String type,
							  @Nullable String serialInterface,
							  @Nullable Integer nodeAPITimeout,
							  @Nullable String nodeUSBChipID,
							  TestbedRuntime testbedRuntime) {

		Preconditions.checkNotNull(nodeUrn);
		Preconditions.checkNotNull(type);
		Preconditions.checkNotNull(testbedRuntime);

		this.nodeUrn = nodeUrn;
		this.type = type;
		this.serialInterface = serialInterface;
		this.nodeAPITimeout = nodeAPITimeout;
		this.nodeUSBChipID = nodeUSBChipID;

		this.testbedRuntime = testbedRuntime;

	}

	@Override
	protected void configure() {

		bind(String.class).annotatedWith(Names.named(WSNDeviceAppModule.NAME_NODE_URN)).toInstance(nodeUrn);
		bind(String.class).annotatedWith(Names.named(WSNDeviceAppModule.NAME_NODE_TYPE)).toInstance(type);
		if (serialInterface == null) {
			bind(String.class).annotatedWith(Names.named(WSNDeviceAppModule.NAME_SERIAL_INTERFACE)).toProvider(
					Providers.of((String) null)
			);
		} else {
			bind(String.class).annotatedWith(Names.named(WSNDeviceAppModule.NAME_SERIAL_INTERFACE))
					.toInstance(serialInterface);
		}
		if (nodeAPITimeout == null) {
			bind(Integer.class).annotatedWith(Names.named(WSNDeviceAppModule.NAME_NODE_API_TIMEOUT)).toProvider(
					Providers.of((Integer) null)
			);
		} else {
			bind(Integer.class).annotatedWith(Names.named(WSNDeviceAppModule.NAME_NODE_API_TIMEOUT))
					.toInstance(nodeAPITimeout);
		}
		if (nodeUSBChipID == null) {
			bind(String.class).annotatedWith(Names.named(WSNDeviceAppModule.NAME_USB_CHIP_ID)).toProvider(
					Providers.of((String) null)
			);
		} else {
			bind(String.class).annotatedWith(Names.named(WSNDeviceAppModule.NAME_USB_CHIP_ID))
					.toInstance(nodeUSBChipID);
		}
		bind(TestbedRuntime.class).toInstance(testbedRuntime);

		bind(WSNDeviceApp.class).to(WSNDeviceAppImpl.class);

	}

}
