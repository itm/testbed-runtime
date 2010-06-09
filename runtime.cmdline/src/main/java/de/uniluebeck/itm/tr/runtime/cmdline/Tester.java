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

package de.uniluebeck.itm.tr.runtime.cmdline;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import de.uniluebeck.itm.gtr.TestbedRuntime;
import de.uniluebeck.itm.gtr.application.TestbedApplication;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNNodeMessageReceiver;
import de.uniluebeck.itm.tr.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: bimschas Date: 27.04.2010 Time: 16:26:55 TODO change
 */
public class Tester {

	private static final Logger log = LoggerFactory.getLogger(Tester.class);

	public static void main(String[] args) throws Exception {

		Tuple<TestbedRuntime, ImmutableList<TestbedApplication>> node1 =
				Main.start(new String[]{"-n", "1", "-f", "democonfig-automated.xml"});
//		Tuple<TestbedRuntime, ImmutableList<TestbedApplication>> node2 =
//				Main.start(new String[]{"-n", "2", "-f", "democonfig-automated.xml"});
//		Tuple<TestbedRuntime, ImmutableList<TestbedApplication>> node3 =
//				Main.start(new String[]{"-n", "3", "-f", "democonfig-automated.xml"});

		WSNApp wsnAppNode1 = getWsnApp(node1.getSecond());

		final Object syncLock = new Object();

		boolean executeflashNode = true;
		boolean executeSendMessage = true;
		boolean executeAreNodesAlive = true;
		boolean executeResetNodes = true;
		
		Set<String> nodeUrns = new HashSet<String>();
		nodeUrns.add("3a");

		// =============================== register as listener for all node outputs ===============================

		wsnAppNode1.addNodeMessageReceiver(new WSNNodeMessageReceiver() {
			@Override
			public void receive(WSNAppMessages.Message message) {
				log.info("------ Received message from node: " + message);
			}
		});

		log.debug("()()() Waiting until all receivers are registered...");
		Thread.sleep(5000);


		// =============================== flash node ===============================

		if (executeflashNode) {
			log.debug("### About to execute SEND FLASH PROGRAM OPERATION");
			Thread.sleep(1000);

			File programFile = new File("jn5139r1-iseraerial.bin");
			FileInputStream programFileReader = new FileInputStream(programFile);
			byte[] programBytes = new byte[(int) programFile.length()];
			programFileReader.read(programBytes, 0, programBytes.length);

			Map<String, WSNAppMessages.Program> programs = new HashMap<String, WSNAppMessages.Program>();
			WSNAppMessages.Program program = WSNAppMessages.Program.newBuilder()
					.setProgram(ByteString.copyFrom(programBytes))
					.build();
			programs.put("3a", program);

			wsnAppNode1.flashPrograms(programs, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					log.info("Received request status for FLASH PROGRAM OPERATION: {}", requestStatus);

					if (requestStatus.getStatus().getValue() < 0 || requestStatus.getStatus().getValue() >= 100) {
						synchronized (syncLock) {
							syncLock.notifyAll();
						}
					}

				}

				@Override
				public void failure(Exception exception) {
					log.error("Error for FLASH PROGRAM OPERATION: " + exception, exception);
					synchronized (syncLock) {
						syncLock.notifyAll();
					}
				}
			}
			);
			synchronized (syncLock) {
				syncLock.wait();
			}
		}

		// =============================== send message ===============================

		if (executeSendMessage) {
			log.debug("### About to execute SEND MESSAGE OPERATION");

			Thread.sleep(1000);

			int binaryType = 0x00;
			byte[] binaryData = {0x03};

			WSNAppMessages.Message.BinaryMessage.Builder builder = WSNAppMessages.Message.BinaryMessage.newBuilder()
					.setBinaryType(binaryType)
					.setBinaryData(ByteString.copyFrom(binaryData));

			WSNAppMessages.Message message = WSNAppMessages.Message.newBuilder()
					.setBinaryMessage(builder)
					.setSourceNodeId("1a")
					.setTimestamp("abc").build();

			wsnAppNode1.send(nodeUrns, message, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					log.info("Received request status for SEND MESSAGE OPERATION: {}", requestStatus);
					synchronized (syncLock) {
						syncLock.notifyAll();
					}
				}

				@Override
				public void failure(Exception exception) {
					log.error("Error for SEND MESSAGE OPERATION: " + exception, exception);
					synchronized (syncLock) {
						syncLock.notifyAll();
					}
				}
			}
			);
		}

		// =============================== are nodes alive? ===============================

		if (executeAreNodesAlive) {
			
			log.debug("### About to execute ARE NODES ALIVE OPERATION");
			Thread.sleep(1000);

			wsnAppNode1.areNodesAlive(nodeUrns, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					log.info("Received request status for ARE NODES ALIVE OPERATION: {}", requestStatus);
					synchronized (syncLock) {
						syncLock.notifyAll();
					}
				}

				@Override
				public void failure(Exception exception) {
					log.error("Error for ARE NODES ALIVE OPERATION: " + exception, exception);
					synchronized (syncLock) {
						syncLock.notifyAll();
					}
				}
			}
			);
		}

		// =============================== reset nodes ===============================

		if (executeResetNodes) {
			
			log.debug("### About to execute RESET NODES OPERATION");
			Thread.sleep(1000);

			wsnAppNode1.resetNodes(nodeUrns, new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					log.info("Received request status for RESET NODES OPERATION: {}", requestStatus);
					synchronized (syncLock) {
						syncLock.notifyAll();
					}
				}

				@Override
				public void failure(Exception exception) {
					log.error("Error for RESET NODES OPERATION: " + exception, exception);
					synchronized (syncLock) {
						syncLock.notifyAll();
					}
				}
			}
			);
		}


	}

	private static WSNApp getWsnApp(ImmutableList<TestbedApplication> applications) throws Exception {
		for (TestbedApplication application : applications) {
			if (application instanceof WSNApp) {
				return (WSNApp) application;
			}
		}
		throw new Exception("WSNApp not found");
	}

}
