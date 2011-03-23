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

package de.itm.uniluebeck.tr.wiseml;

import eu.wisebed.ns.wiseml._1.Data;
import eu.wisebed.ns.wiseml._1.Trace;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.util.GregorianCalendar;

public class TestMain {

	/**
	 * @param args
	 *
	 * @throws IOException
	 * @throws JAXBException
	 */
	public static void main(String[] args) throws IOException, JAXBException, DatatypeConfigurationException {
		WisemlStreaming wise = new WisemlStreaming(System.out);

		wise.addHeader();

		wise.addFragment("<setup>\n");
		wise.addFragment("<node id=\"\"/>\n");
		wise.addFragment("</setup>\n");

		DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();

		Trace t = new Trace();
		t.setId("testid");

		// add timestamp
		t.getTraceItem()
				.add(datatypeFactory.newXMLGregorianCalendar((GregorianCalendar) GregorianCalendar.getInstance())
						.toXMLFormat()
				);

		// add node temperature reading
		Trace.Node node = new Trace.Node();
		node.setId("urn:wisebed:uzl:0xfe80");
		Data data = new Data();
		data.setKey("temperature");
		data.setContent("15");
		node.getData().add(data);

		node = new Trace.Node();
		node.setId("urn:wisebed:uzl:0xfe81");
		data = new Data();
		data.setKey("temperature");
		data.setContent("17");
		node.getData().add(data);

		t.getTraceItem().add(node);

		t.getTraceItem()
				.add(datatypeFactory.newXMLGregorianCalendar((GregorianCalendar) GregorianCalendar.getInstance())
						.toXMLFormat()
				);
		Trace.Link link = new Trace.Link();
		link.setSource("urn:wisebed:uzl:0xfe80");
		link.setTarget("urn:wisebed:uzl:0xfe81");
		t.getTraceItem().add(link);

		wise.addFragment(t);


		wise.close();

	}
}
