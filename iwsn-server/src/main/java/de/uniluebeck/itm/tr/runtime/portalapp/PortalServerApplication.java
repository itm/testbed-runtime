/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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

package de.uniluebeck.itm.tr.runtime.portalapp;

import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.SessionManagementPreconditions;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.iwsn.overlay.application.TestbedApplication;
import de.uniluebeck.itm.tr.runtime.portalapp.xml.Portalapp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppFactory;


public class PortalServerApplication implements TestbedApplication {

	private SessionManagementService service;

	private final TestbedRuntime testbedRuntime;

	private final SessionManagementServiceConfig config;

	private final SessionManagementPreconditions preconditions;

	private final WSNApp wsnApp;

	private final DeliveryManager deliveryManager;

	private SessionManagementSoapService soapService;

	public PortalServerApplication(final TestbedRuntime testbedRuntime, final Portalapp portalAppConfig) {

		this.testbedRuntime = testbedRuntime;

		this.config = new SessionManagementServiceConfig(portalAppConfig);

		this.preconditions = new SessionManagementPreconditions();
		this.preconditions.addServedUrnPrefixes(this.config.getUrnPrefix());
		this.preconditions.addKnownNodeUrns(this.config.getNodeUrnsServed());

		this.wsnApp = WSNAppFactory.create(testbedRuntime, config.getNodeUrnsServed());

		this.deliveryManager = new DeliveryManager();
	}

	@Override
	public String getName() {
		return PortalServerApplication.class.getSimpleName();
	}

	@Override
	public void start() throws Exception {

		service = new SessionManagementServiceImpl(testbedRuntime, config, preconditions, wsnApp, deliveryManager);
		service.start();

		soapService = new SessionManagementSoapService(service, config);
		soapService.start();
	}

	@Override
	public void stop() throws Exception {
		if (service != null) {
			service.stop();
		}
	}
}
