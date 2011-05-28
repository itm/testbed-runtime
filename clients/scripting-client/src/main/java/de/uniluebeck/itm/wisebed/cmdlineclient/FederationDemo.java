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

package de.uniluebeck.itm.wisebed.cmdlineclient;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.jws.WebParam;
import javax.xml.datatype.DatatypeFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.Triple;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.AsyncJobObserver;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.HtmlWriter;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.Job;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.wsn.Program;
import eu.wisebed.api.wsn.ProgramMetaData;
import eu.wisebed.api.wsn.WSN;
import eu.wisebed.testbed.api.rs.RSServiceHelper;
import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.RS;
import eu.wisebed.api.rs.SecretReservationKey;
import eu.wisebed.testbed.api.snaa.helpers.SNAAServiceHelper;
import eu.wisebed.api.snaa.AuthenticationTriple;
import eu.wisebed.api.snaa.SNAA;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;

public class FederationDemo {

	private static final Logger log = Logger.getLogger(FederationDemo.class);

	/**
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws Exception {

		Logger.getRootLogger().setLevel(Level.DEBUG);
		final BeanShellHelper helper = new BeanShellHelper();

		//--------------------------------------------------------------------------
		//Configuration
		//--------------------------------------------------------------------------
		String localControllerEndpoint = "http://localhost:6060/controller";

		String reservationEndpoint = "http://localhost:30001/rs";
		String snaaEndpoint = "http://localhost:20001/snaa";
		String sessionManagementEndpoint = "http://localhost:10001/sessions";

		String htmlLogFile = "federationdemo.html";

		Triple<String, String, String>[] authenticationTriples = new Triple[]{
				new Triple("testbeduzl1", "testbeduzl1", "urn:wisebed:uzl1:"),
				new Triple("testbeduzl2", "testbeduzl2", "urn:wisebed:uzl2:")
		};

		List nodes = Arrays.asList(new Object[]{
				"urn:wisebed:uzl1:1",
				"urn:wisebed:uzl2:2"
		}
		);

		System.out.println("=== AUTHENTICATION ===");
		waitForUser();

		//--------------------------------------------------------------------------
		//Authentication
		//--------------------------------------------------------------------------
		List authenticationData = new ArrayList();
		for (int i = 0; i < authenticationTriples.length; ++i) {
			AuthenticationTriple a = new AuthenticationTriple();
			a.setUsername(authenticationTriples[i].getFirst());
			a.setPassword(authenticationTriples[i].getSecond());
			a.setUrnPrefix(authenticationTriples[i].getThird());
			authenticationData.add(a);
		}

		SNAA snaa = SNAAServiceHelper.getSNAAService(snaaEndpoint);
		List secretAuthKeys = snaa.authenticate(authenticationData);
		System.out.println("Authenticated: " + StringUtils.jaxbMarshal(secretAuthKeys));

		System.out.println("=== RESERVATION ===");
		waitForUser();

		//--------------------------------------------------------------------------
		//Reservation
		//--------------------------------------------------------------------------
		RS rs = RSServiceHelper.getRSService(reservationEndpoint);

		DateTime reserveFrom = new DateTime();
		reserveFrom = reserveFrom.plusSeconds(3);//now, one second in the future
		DateTime reserveUntil = new DateTime(reserveFrom.plusHours(3)); //now + 3 hours

		ConfidentialReservationData res = new ConfidentialReservationData();
		res.setFrom(DatatypeFactory.newInstance().newXMLGregorianCalendar(reserveFrom.toGregorianCalendar()));
		res.setTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(reserveUntil.toGregorianCalendar()));
		//Todo: Set authenticationData
		res.getNodeURNs().addAll(nodes);

		List<SecretReservationKey> reservation = rs.makeReservation(helper.copySnaaToRs(secretAuthKeys), res);
		System.out.println("Got a reservation: " + StringUtils.jaxbMarshal(reservation));

		Thread.sleep(3000); // wait one second so that reservation time slot has definitely come

		System.out.println("=== EXPERIMENTATION: Get Instance ===");
		waitForUser();

		//--------------------------------------------------------------------------
		//Create a local controller
		//--------------------------------------------------------------------------
		final AsyncJobObserver jobs =
				new AsyncJobObserver(1, TimeUnit.MINUTES); //Timeout for join until unfinished jobs are removed

		HtmlWriter htmlLogWriter = null;

		if (htmlLogFile != null) {
			htmlLogWriter = new HtmlWriter(new FileWriter(new File(htmlLogFile), false));
			jobs.addListener(htmlLogWriter);
		}

		final HtmlWriter finalHtmlLogWriter = htmlLogWriter;

		Controller myController = new Controller() {

			@Override
			public void receive(@WebParam(name = "msg", targetNamespace = "") final List<Message> msgs) {
				String s = helper.toString(msgs);
				log.debug("Received message: " + s);
				if (finalHtmlLogWriter != null) {
					try {
						for (Message msg : msgs) {
							finalHtmlLogWriter.receiveMessage(msg);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void receiveStatus(
					@WebParam(name = "status", targetNamespace = "") final List<RequestStatus> status) {
				jobs.receive(status);
			}

			@Override
			public void receiveNotification(@WebParam(name = "msg", targetNamespace = "") final List<String> msgs) {
				log.info("Received notification(s): " + Arrays.toString(msgs.toArray()));
			}

			@Override
			public void experimentEnded() {
				log.info("Experiment ended!");
			}
		};

		DelegatingController delegator = new DelegatingController(myController);
		delegator.publish(localControllerEndpoint);

		//--------------------------------------------------------------------------
		// Get WSN API instance URL --> call getInstance()
		//--------------------------------------------------------------------------
		SessionManagement sm = WSNServiceHelper.getSessionManagementService(sessionManagementEndpoint);
		String wsnApiEndpoint = sm.getInstance(helper.copyRsToWsn(reservation), localControllerEndpoint);
		WSN wsn = WSNServiceHelper.getWSNService(wsnApiEndpoint);

		System.out.println("Successfully received iWSN endpoint URL: " + wsnApiEndpoint);

		//--------------------------------------------------------------------------
		// Experiment control using the WSN API
		//--------------------------------------------------------------------------

		System.out.println("\n=== EXPERIMENTATION: AreNodesAlive ===");
		waitForUser();

		jobs.submit(new Job("nodes alive test", wsn.areNodesAlive(nodes), nodes, Job.JobType.areNodesAlive));
		jobs.join();

		System.out.println("\n=== EXPERIMENTATION: FlashPrograms ===");
		waitForUser();

		final List<Integer> programIndices = new ArrayList<Integer>();
		programIndices.add(0);
		programIndices.add(1);

		final List<Program> programs = new ArrayList<Program>();
		programs.add(readProgram(
				"scripts/federationdemo/WISEBEDApplication.jn5139r1.bin",
				"iSerAerial",
				"",
				"iSense",
				"1.0"
		));
		programs.add(readProgram(
				"scripts/federationdemo/WISEBEDApplication.jn5139r1.bin",
				"iSerAerial",
				"",
				"iSense",
				"1.0"
		));

		jobs.submit(new Job("flash nodes", wsn.flashPrograms(nodes, programIndices, programs), nodes,
				Job.JobType.flashPrograms
		)
		);
		jobs.join();

		System.out.println("\n=== EXPERIMENTATION: ResetNodes ===");
		waitForUser();

		jobs.submit(new Job("reset nodes", wsn.resetNodes(nodes), nodes, Job.JobType.resetNodes));
		jobs.join();


		//--------------------------------------------------------------------------
		// Clean up and exit
		//--------------------------------------------------------------------------
		System.out.println("Demo complete!");
		waitForUser();

		if (htmlLogWriter != null) {
			htmlLogWriter.close();
		}
		System.exit(0);

	}

	private static Program readProgram(String pathname, String name, final String other, final String platform,
									   final String version) throws Exception {

		final ProgramMetaData programMetaData = new ProgramMetaData();
		programMetaData.setName(name);
		programMetaData.setOther(other);
		programMetaData.setPlatform(platform);
		programMetaData.setVersion(version);

		Program program = new Program();
		File programFile = new File(pathname);

		FileInputStream fis = new FileInputStream(programFile);
		BufferedInputStream bis = new BufferedInputStream(fis);
		DataInputStream dis = new DataInputStream(bis);

		long length = programFile.length();
		byte[] binaryData = new byte[(int) length];
		dis.readFully(binaryData);

		program.setProgram(binaryData);
		program.setMetaData(programMetaData);

		return program;

	}

	private static void waitForUser() throws IOException {
		System.out.println("Please press any key to continue...");
		System.in.read();
	}

}
