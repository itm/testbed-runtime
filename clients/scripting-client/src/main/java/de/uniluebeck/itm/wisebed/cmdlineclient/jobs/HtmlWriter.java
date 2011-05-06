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

package de.uniluebeck.itm.wisebed.cmdlineclient.jobs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.Job.JobType;
import eu.wisebed.api.common.Message;

public class HtmlWriter implements JobResultListener {
	private static final Logger log = Logger.getLogger(HtmlWriter.class);

	private Writer out;


	public HtmlWriter(Writer out) throws IOException {
		this.out = new BufferedWriter(out);
		writeHeader();
	}

	@Override
	public synchronized void receiveMessage(Message msg) throws IOException {
		DateTimeFormatter timeFormatter = DateTimeFormat.mediumDateTime();

		write("<h1>Message from " + msg.getSourceNodeId() + "</h1>");
		write("<div>");
		write("<table>");

		write("	<tr>");
		write("		<td>Time</td>");
		write("		<td>&nbsp;</td>");
		write("		<td>" + timeFormatter.print(new DateTime(msg.getTimestamp().toGregorianCalendar())) + "</td>");
		write("	</tr>");

		if (msg.getBinaryData() != null) {
			write("	<tr>");
			write("		<td>" + StringUtils.toHexString(msg.getBinaryData()) + "</td>");
			write("	</tr>");
		}

		write("</table>");
		write("</div>");
		out.flush();
	}

	@Override
	public void timeout() {
		// nothing to do
	}

	@Override
	public synchronized void receiveJobResult(JobResult result) {
		Duration duration = new Duration(result.getStartTime(), result.getEndTime());
		JobType type = result.getJobType();
		String description = result.getDescription();

		try {
			write("<h1>" + type + (description != null && description.length() > 0 ? (": " + description) : "")
					+ "</h1>");
			write("<div>");

			write("<table>");
			write("	<tr>");
			write("		<td>Nodes: </td>");
			write("		<td>" + StringUtils.toString(result.getResults().keySet()) + "</td>");
			write("		<td>&nbsp;</td>");
			write("	</tr>");

			write("	<tr>");
			write("		<td>Duration: </td>");
			write("		<td>" + duration.getMillis() + "ms</td>");
			write("		<td>&nbsp;</td>");
			write("	</tr>");

			write("	<tr>");
			write("		<td>Success: </td>");
			write("		<td>" + result.getSuccessPercent() + "%</td>");
			write("		<td>&nbsp;</td>");
			write("	</tr>");

			write("	<tr>");
			write("		<td>Messages: </td>");
			write("		<td>&nbsp;</td>");
			write("		<td>&nbsp;</td>");
			write("	</tr>");

			for (Map.Entry<String, JobResult.Result> res : result.getResults().entrySet()) {
				write("<tr>");
				write("		<td>&nbsp;</td>");
				write("		<td>" + res.getKey() + "(" + (res.getValue().success ? "ok" : "failed") + ")</td>");
				write("		<td>" + res.getValue().message + "</td>");
				write("</tr>");
			}

			write("</table>");
			write("</div>");
			out.flush();
		} catch (IOException e) {
			log.warn("Unable to write HTML:" + e, e);
		}

	}

	public synchronized void close() throws IOException {
		writeFooter();
		out.flush();
	}

	private synchronized void write(String s) throws IOException {
		out.write(s);
		out.write('\n');
	}

	private synchronized void writeHeader() throws IOException {
		DateTimeFormatter timeFormatter = DateTimeFormat.mediumDateTime();

		write(" <html><head>");
		write("	<meta http-equiv=\"content-type\" content=\"text/html; charset=ISO-8859-1\">");
		write("<style type=\"text/css\">         body {                 font-family: Helvetica;                 font-size:12pt;                                 margin-left:0px;                 margin-right:0px;                 margin-top:0px;                 margin-bottom:0px         }          h1 {             font-family: Helvetica;             color:#666666;             font-size:15pt;             font-weight:bold;             margin-left:5px;             margin-bottom:0px;             margin-top:30px;         }          h2 {             font-family: Helvetica;             color:#000000;             font-weight:bold;             font-size:13pt;             margin-left:40px;             margin-bottom:0px;             margin-top:15px;         }          h3 {             font-family: Helvetica;             color:#666666;             font-weight:bold;             font-size:12pt;             margin-left:50px;             margin-bottom:0px;             margin-top:15px;         }          div {             margin-left:30px;             margin-right:200px;             margin-top:20px;             font-family: Helvetica;             color:#000000;         }          ul {             margin-left:55px;             margin-top:10px;             font-family: Helvetica;             color:#000000;         }          ol {             margin-left:55px;             margin-top:10px;             font-family: Helvetica;             color:#000000;         }          li {             margin-left:0px;             margin-top:0px;         }          a {             text-decoration: none;         }          a:link {             font-family: Helvetica;             color:#A50021;          }          a:visited {             font-family: Helvetica;             color:#A50021;          }          a:hover {             font-family: Helvetica;             color:#A50021;             text-decoration:underline;          }          a:active {             font-family: Helvetica;             color:#A50021;          }    a.link_active:hover {             text-decoration:underline;         }            </style>");
		write("</head>");

		write("<body topmargin=\"0\" leftmargin=\"0\" color=\"black\" bgcolor=\"white\" marginheight=\"0\" marginwidth=\"0\">");

		write("<hr noshade size=\"1\" width=\"90%\" align=\"left\">");
		write("<div style='margin-left:10px; margin-right:0px; margin-top:10px; margin-bottom: 0px;font-size: 40px;color: #A50021;'>WISEBED Experiment Trace</div>");
		write("<br/>");
		write("<div style='margin-left:10px; margin-right:0px; margin-top:0px; margin-bottom: 0px;'>");
		write("Started: " + timeFormatter.print(new DateTime()));
		write("</div>");
		write("<hr noshade size=\"1\" width=\"90%\" align=\"left\">");
	}

	private synchronized void writeFooter() throws IOException {
		write("</body>");
	}

}
