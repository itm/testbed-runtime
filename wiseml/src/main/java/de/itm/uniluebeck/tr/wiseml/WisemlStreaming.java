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

import de.uniluebeck.itm.tr.util.StringUtils;
import org.w3c.tidy.Tidy;

import javax.xml.bind.JAXBException;
import java.io.*;

//TODO Do XML streaming validation (http://java.sun.com/developer/technicalArticles/xml/jaxp1-3/#Validate%20SAX%20Stream)

public class WisemlStreaming {

	private XmlTidy tidyThread;

	private BufferedWriter out;

	static class XmlTidy extends Thread {

		private final InputStream instream;

		private final OutputStream outstream;

		public XmlTidy(InputStream in, OutputStream out) {
			this.instream = in;
			this.outstream = out;
		}

		@Override
		public void run() {
			Tidy tidy = new Tidy();
			tidy.setXmlTags(true);
			tidy.setXmlOut(true);
			tidy.setSmartIndent(true);
			tidy.parse(instream, outstream);
		}
	}

	public WisemlStreaming(OutputStream outputStream) throws IOException {
		PipedInputStream pipeIn = new PipedInputStream();
		PipedOutputStream pipedout = new PipedOutputStream(pipeIn);

		out = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(pipedout)));

		tidyThread = new XmlTidy(pipeIn, outputStream);
		tidyThread.start();
	}

	public void addHeader() throws IOException {
		addFragment("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		addFragment(
				"<wiseml xmlns=\"http://wisebed.eu/ns/wiseml/1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://wisebed.eu/ns/wiseml/1.0\" version=\"1.0\">\n"
		);
	}

	public void addFooter() throws IOException {
		addFragment("</wiseml>");
	}

	public void addFragment(Object wisemlFragment) throws JAXBException, IOException {
		out.write(StringUtils.jaxbMarshalFragment(wisemlFragment));
	}

	public void addFragment(String xmlfragment) throws IOException {
		out.write(xmlfragment);
	}

	public void close() throws IOException {
		out.flush();
		out.close();
	}

}
