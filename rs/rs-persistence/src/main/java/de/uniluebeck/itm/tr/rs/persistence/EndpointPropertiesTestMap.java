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

package de.uniluebeck.itm.tr.rs.persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: rohwedder
 * Date: 27.04.2010
 * Time: 12:29:43
 * To change this template use File | Settings | File Templates.
 */
public class EndpointPropertiesTestMap {

	public static final Map<String, String> SNAAPropertiesMapWisebed1 = new HashMap<String, String>() {{

		put("config.port", "8080");
		put("config.snaas", "dummy1");

		put("dummy1.type", "dummy");
		put("dummy1.urnprefix", "urn:wisebed1:testbed1");
		put("dummy1.path", "/snaa/dummy1");
	}};

	public static final Map<String, String> SNAAPropertiesMapWisebed2 = new HashMap<String, String>() {{

		put("config.port", "8090");
		put("config.snaas", "dummy1");

		put("dummy1.type", "dummy");
		put("dummy1.urnprefix", "urn:wisebed2:testbed1");
		put("dummy1.path", "/snaa/dummy1");


		/*put("shib1.type","shibboleth");
				put("shib1.urnprefix","urn:wisebed:shib1");
				put("shib1.path","/snaa/shib1");
				put("shib2.type","shibboleth");
				put("shib2.urnprefix","urn:wisebed:shib2");
				put("shib2.path","/snaa/shib2");
				put("fed1.type","federator");
				put("fed1.path","/snaa/fed1");
				put("fed1.federates","shib1, shib2, dummy1, dummy2");
				put("fed1.shib1.urnprefixes","urn:wisebed:shib1");
				put("fed1.shib1.endpointurl","http://localhost:8080/snaa/shib1");
				put("fed1.shib2.urnprefixes","urn:wisebed:shib2");
				put("fed1.shib2.endpointurl","http://localhost:8080/snaa/shib2");
				put("fed1.dummy1.urnprefixes","urn:wisebed:testbed1");
				put("fed1.dummy1.endpointurl","http://localhost:8080/snaa/dummy1");
				put("fed1.dummy2.urnprefixes","urn:wisebed:testbed2");
				put("fed1.dummy2.endpointurl","http://localhost:8080/snaa/dummy2");
				put("jaas1.type","jaas");
				put("jaas1.urnprefix","urn:wisebed:jaas1");
				put("jaas1.path","/snaa/jaas1");
				put("jaas1.configfile","sample_config.jaas");
				put("jaas1.module","AlwaysTrue");
				put("jaas2.type","jaas");
				put("jaas2.urnprefix","urn:wisebed:jaas2");
				put("jaas2.path","/snaa/jaas2");
				put("jaas2.configfile","sample_config.jaas");
				put("jaas2.module","HtpasswdFile");*/
	}};

	public static final Map<String, String> RSPropertiesMap1 = new HashMap<String, String>() {{

		put("config.port", "9090");
		put("config.rsnames", "inmemory1, inmemory2, fed1");

		put("inmemory1.type", "singleurnprefix");
		put("inmemory1.snaaendpointurl", "http://localhost:8080/snaa/dummy1");
		put("inmemory1.persistence", "inmemory");
		put("inmemory1.urnprefix", "urn:wisebed1:testbed1");
		put("inmemory1.path", "/rs/inmemory1");

		put("inmemory2.type", "singleurnprefix");
		put("inmemory2.snaaendpointurl", "http://localhost:8090/snaa/dummy1");
		put("inmemory2.persistence", "inmemory");
		put("inmemory2.urnprefix", "urn:wisebed2:testbed1");
		put("inmemory2.path", "/rs/inmemory2");

		put("fed1.type", "federator");
		put("fed1.path", "/rs/fed1");
		put("fed1.federates", "inmemory1, inmemory2");
		put("fed1.inmemory1.urnprefixes", "urn:wisebed1:testbed1");
		put("fed1.inmemory1.endpointurl", "http://localhost:9090/rs/inmemory1");
		put("fed1.inmemory2.urnprefixes", "urn:wisebed2:testbed1");
		put("fed1.inmemory2.endpointurl", "http://localhost:9090/rs/inmemory2");

	}};

    public static final Map<String, String> SNAAPropertiesMapWisebed3 = new HashMap<String, String>(){{
         put("config.port", "8080");
         put("config.snaas", "shib1, shib2, fed1, wisebedfed1");

         put("shib1.type", "shibboleth");
         put("shib1.urnprefix", "urn:wisebed1:shib1");
         put("shib1.path", "/snaa/shib1");

         put("shib2.type", "shibboleth");
         put("shib2.urnprefix", "urn:wisebed1:shib2");
         put("shib2.path", "/snaa/shib2");

         put("fed1.type", "federator");
         put("fed1.path", "/snaa/fed1");
         put("fed1.federates","shib1, shib2");
         put("fed1.shib1.urnprefixes", "urn:wisebed1:shib1");
         put("fed1.shib1.endpointurl", "http://localhost:8080/snaa/shib1");
         put("fed1.shib2.urnprefixes", "urn:wisebed1:shib2");
         put("fed1.shib2.endpointurl", "http://localhost:8080/snaa/shib2");

         put("wisebedfed1.type", "wisebed-federator");
         put("wisebedfed1.path", "/snaa/wisebedfed1");
         put("wisebedfed1.secret_user_key_url","https://gridlab23.unibe.ch/portal/SNA/secretUserKey");
         put("wisebedfed1.federates","shib1");
         put("wisebedfed1.shib1.urnprefixes", "urn:wisebed1:shib1");
         put("wisebedfed1.shib1.endpointurl", "http://localhost:8080/snaa/shib1");
         put("wisebedfed1.shib2.urnprefixes", "urn:wisebed1:shib2");
         put("wisebedfed1.shib2.endpointurl", "http://localhost:8080/snaa/shib2");
     }};

     public static final Map<String, String> RSPropertiesMap2 = new HashMap<String, String>() {{

         put("config.port", "9090");
         put("config.rsnames", "inmemory1, inmemory2, fed1");

         put("inmemory1.type", "singleurnprefix");
         put("inmemory1.snaaendpointurl", "http://localhost:8080/snaa/shib1");
         put("inmemory1.persistence", "inmemory");
         put("inmemory1.urnprefix", "urn:wisebed1:shib1");
         put("inmemory1.path", "/rs/inmemory1");

         put("inmemory2.type", "singleurnprefix");
         put("inmemory2.snaaendpointurl", "http://localhost:8080/snaa/shib2");
         put("inmemory2.persistence", "inmemory");
         put("inmemory2.urnprefix", "urn:wisebed1:shib2");
         put("inmemory2.path", "/rs/inmemory2");

         put("fed1.type", "federator");
         put("fed1.path", "/rs/fed1");
         put("fed1.federates", "inmemory1, inmemory2");
         put("fed1.inmemory1.urnprefixes", "urn:wisebed1:testbed1");
         put("fed1.inmemory1.endpointurl", "http://localhost:9090/rs/inmemory1");
         put("fed1.inmemory2.urnprefixes", "urn:wisebed2:testbed1");
         put("fed1.inmemory2.endpointurl", "http://localhost:9090/rs/inmemory2");

     }};

}
