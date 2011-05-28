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

package de.uniluebeck.itm.tr.debuggingguiclient;

import java.util.Map;
import java.util.Properties;

public class WSNClientProperties {

	public static final String SNAA_CLIENT_ENDPOINTURL = "snaa.client.endpointurl";

	public static final String SNAA_CLIENT_CREDENTIALS = "snaa.client.credentials";

	public static final String SNAA_CLIENT_SECRETAUTHENTICATIONKEYS = "snaa.client.secretauthenticationkeys";

	public static final String SNAA_CLIENT_ACTION = "snaa.client.action";

	public static final String RS_CLIENT_ENDPOINTURL = "rs.client.endpointurl";

	public static final String RS_CLIENT_SECRETAUTHENTICATIONKEYS = "rs.client.secretauthenticationkeys";

	public static final String RS_CLIENT_DATEFROM = "rs.client.datefrom";

	public static final String RS_CLIENT_DATEUNTIL = "rs.client.dateuntil";

	public static final String RS_CLIENT_NODEURNS = "rs.client.nodeurns";

	public static final String CONTROLLER_CLIENT_ENDPOINTURL = "controller.client.endpointurl";

	public static final String CONTROLLER_SERVICE_ENDPOINTURL = "controller.service.endpointurl";

	public static final String SESSIONMANAGEMENT_CLIENT_ENDPOINTURL = "sessionmanagement.client.endpointurl";

	public static final String SESSIONMANAGEMENT_CLIENT_SECRETRESERVATIONKEYS =
			"sessionmanagement.client.secretreservationkeys";

	public static final String SESSIONMANAGEMENT_CLIENT_CONTROLLERENDPOINTURL =
			"sessionmanagement.client.controllerendpointurl";

	public static final String WSN_CLIENT_ENDPOINTURL = "wsn.client.endpointurl";

	public static final String WSN_SERVICE_ENDPOINTURL = "wsn.service.endpointurl";

	public static String readList(final Properties properties, final String propertyPrefix, final String defaultValue) {
		StringBuilder builder = new StringBuilder();
        boolean found = false;
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (((String) entry.getKey()).startsWith(propertyPrefix)) {
                found = true;
				builder.append((String) entry.getValue());
				builder.append("\n");
			}
		}
        return found ? builder.toString() : defaultValue;
	}
}
