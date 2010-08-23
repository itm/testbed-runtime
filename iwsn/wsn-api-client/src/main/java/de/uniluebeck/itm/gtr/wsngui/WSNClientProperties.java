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

package de.uniluebeck.itm.gtr.wsngui;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class WSNClientProperties {

    public final static String KEY_CONTROLLER_CLIENT = "controller.client";
    public final static String KEY_CONTROLLER_SERVICE = "controller.service";
    public final static String KEY_SESSION_MANAGEMENT_CLIENT = "sessionmanagement.client";
    public final static String KEY_SESSION_MANAGEMENT_CLIENT_CONTROLLER = "sessionmanagement.client.controller";
    public final static String KEY_WSN_CLIENT = "wsn.client";
    public final static String KEY_WSN_SERVER_DUMMY = "wsn.server";
    public final static String KEY_RESERVATION_KEYS = "reservationkeys";
    public final static String KEY_ENDPOINT_URL = "endpoint.url";

    public static Map<String, String> getPropertyMap(String s) throws IOException {
        Map<String, String> propertyMap = new HashMap<String, String>();
        Properties props = new Properties();
        File file = new File(s);
        props.load(new FileReader(file));

        for (Object o : props.keySet()){
            String key = (String) o;
            if (key.startsWith(KEY_SESSION_MANAGEMENT_CLIENT + "." + KEY_RESERVATION_KEYS)){
                assertReservationKey(props.getProperty(key));
                propertyMap.put(key, props.getProperty(key));

            } else {
                propertyMap.put(key, props.getProperty(key));
            }
        }
        return propertyMap;
    }

    private static void assertReservationKey(String property) {
        if (property.split(",").length != 2) throw new RuntimeException("Reservation-Key " + property + " is no tuple!");
    }

}
