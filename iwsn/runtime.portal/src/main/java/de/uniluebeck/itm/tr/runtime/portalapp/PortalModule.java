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

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;


public class PortalModule extends AbstractModule {

	public static final String NAME_WSN_INSTANCE_BASE_URL =
			"de.uniluebeck.itm.tr.runtime.portalapp.PortalModule/NAME_WSN_INSTANCE_BASE_URL";

	public static final String NAME_RESERVATION_ENDPOINT_URL =
			"de.uniluebeck.itm.tr.runtime.portalapp.PortalModule/NAME_RESERVATION_ENDPOINT_URL";

	public static final String NAME_URN_PREFIX = "de.uniluebeck.itm.tr.runtime.portalapp.PortalModule/NAME_URN_PREFIX";

	public static final String NAME_WISEML = "de.uniluebeck.itm.tr.runtime.portalapp.PortalModule/NAME_WISEML";

	public static final String NAME_SESSION_MANAGEMENT_ENDPOINT_URL =
			"de.uniluebeck.itm.tr.runtime.portalapp.PortalModule/NAME_SESSION_MANAGEMENT_ENDPOINT_URL";

	private String urnPrefix;

	private String wsnInstanceBaseUrl;

	private WSNApp wsnApp;

	private String wiseML;

	private String reservationEndpointUrl;

	private String sessionManagementEndpointUrl;

	public PortalModule(String urnPrefix, String sessionManagementEndpointUrl, String wsnInstanceBaseUrl,
						String reservationEndpointUrl, WSNApp wsnApp, final String wiseML) {

		this.urnPrefix = urnPrefix;
		this.sessionManagementEndpointUrl = sessionManagementEndpointUrl;
		this.wsnInstanceBaseUrl = wsnInstanceBaseUrl;
		this.reservationEndpointUrl = reservationEndpointUrl;
		this.wsnApp = wsnApp;
		this.wiseML = wiseML;
	}

	@Override
	protected void configure() {

		bind(String.class).annotatedWith(Names.named(NAME_URN_PREFIX)).toInstance(urnPrefix);
		bind(String.class).annotatedWith(Names.named(NAME_SESSION_MANAGEMENT_ENDPOINT_URL)).toInstance(
				sessionManagementEndpointUrl
		);
		bind(String.class).annotatedWith(Names.named(NAME_WSN_INSTANCE_BASE_URL)).toInstance(wsnInstanceBaseUrl);
		bind(String.class).annotatedWith(Names.named(NAME_RESERVATION_ENDPOINT_URL))
				.toProvider(Providers.of(reservationEndpointUrl));
		bind(String.class).annotatedWith(Names.named(NAME_WISEML)).toInstance(wiseML);

		bind(SessionManagementService.class).to(SessionManagementServiceImpl.class);
		bind(ControllerService.class).to(ControllerServiceImpl.class);

		bind(WSNApp.class).toInstance(wsnApp);

	}

}
