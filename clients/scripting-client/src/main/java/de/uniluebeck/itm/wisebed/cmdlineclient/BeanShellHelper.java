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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import de.uniluebeck.itm.tr.util.StringUtils;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.controller.Status;
import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.Data;
import eu.wisebed.api.rs.SecretReservationKey;
import eu.wisebed.api.snaa.AuthenticationTriple;
import eu.wisebed.api.snaa.SecretAuthenticationKey;
import eu.wisebed.api.wsn.Program;
import eu.wisebed.api.wsn.ProgramMetaData;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class BeanShellHelper {

	private static final Logger log = LoggerFactory.getLogger(BeanShellHelper.class);

	@SuppressWarnings("unused")
	public static Program readProgram(String pathName, String name, final String other, final String platform,
									  final String version) throws Exception {

		final ProgramMetaData programMetaData = new ProgramMetaData();
		programMetaData.setName(name);
		programMetaData.setOther(other);
		programMetaData.setPlatform(platform);
		programMetaData.setVersion(version);

		Program program = new Program();
		File programFile = new File(pathName);

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

	@SuppressWarnings("unused")
	public static ConfidentialReservationData createReservationData(Date from, int duration, TimeUnit durationUnit,
																	String urnPrefix, String userName,
																	String... nodeUrns) {
		return generateConfidentialReservationData(
				Lists.newArrayList(nodeUrns),
				from,
				duration,
				durationUnit,
				urnPrefix,
				userName
		);
	}

	public static ConfidentialReservationData generateConfidentialReservationData(List<String> nodeURNs, Date from,
																				  int duration, TimeUnit durationUnit,
																				  List<String> urnPrefixes,
																				  List<String> userNames) {


		Preconditions.checkArgument(
				urnPrefixes.size() == userNames.size(),
				"The list of URN prefixes must have the same length as the list of user names and the list of passwords"
		);
		Preconditions.checkArgument(urnPrefixes.size() > 0, "At least the credentials of one testbed must be given.");

		try {

			DateTime dtFrom = new DateTime(from);
			DateTime dtUntil;
			switch (durationUnit) {
				case DAYS:
					dtUntil = dtFrom.plusDays(duration);
					break;
				case HOURS:
					dtUntil = dtFrom.plusHours(duration);
					break;
				case MINUTES:
					dtUntil = dtFrom.plusMinutes(duration);
					break;
				case SECONDS:
					dtUntil = dtFrom.plusSeconds(duration);
					break;
				default:
					dtUntil = dtFrom.plusMillis(duration);
					break;
			}


			DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();

			ConfidentialReservationData reservationData = new ConfidentialReservationData();
			reservationData.getNodeURNs().addAll(nodeURNs);
			reservationData.setFrom(datatypeFactory.newXMLGregorianCalendar(dtFrom.toGregorianCalendar()));
			reservationData.setTo(datatypeFactory.newXMLGregorianCalendar(dtUntil.toGregorianCalendar()));
			reservationData.setUserData(userNames.get(0));

			for (int i = 0; i < urnPrefixes.size(); i++) {

				Data data = new Data();

				data.setUrnPrefix(urnPrefixes.get(i));
				data.setUsername(userNames.get(i));

				reservationData.getData().add(data);

			}

			return reservationData;

		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	public static ConfidentialReservationData generateConfidentialReservationData(List<String> nodeURNs, Date from,
																				  int duration, TimeUnit durationUnit,
																				  String urnPrefix, String userName) {
		return generateConfidentialReservationData(
				nodeURNs,
				from,
				duration,
				durationUnit,
				Lists.newArrayList(urnPrefix),
				Lists.newArrayList(userName)
		);
	}

	@SuppressWarnings("unused")
	public static List<SecretAuthenticationKey> generateFakeSNAAAuthentication(String urnPrefix, String userName,
																			   String secretAuthenticationKey) {
		List<SecretAuthenticationKey> secretAuthKeys = new ArrayList<SecretAuthenticationKey>();

		SecretAuthenticationKey key = new SecretAuthenticationKey();
		key.setSecretAuthenticationKey(secretAuthenticationKey);
		key.setUrnPrefix(urnPrefix);
		key.setUsername(userName);

		secretAuthKeys.add(key);
		return secretAuthKeys;
	}

	@SuppressWarnings("unused")
	public static List<SecretReservationKey> generateFakeReservationKeys(String urnPrefix,
																		 String secretReservationKey) {
		List<SecretReservationKey> reservations = new ArrayList<SecretReservationKey>();

		SecretReservationKey key = new SecretReservationKey();
		key.setSecretReservationKey(secretReservationKey);
		key.setUrnPrefix(urnPrefix);

		reservations.add(key);

		return reservations;
	}

	@SuppressWarnings("unused")
	public static List<eu.wisebed.api.rs.SecretAuthenticationKey> copySnaaToRs(List<SecretAuthenticationKey> snaaKeys) {

		List<eu.wisebed.api.rs.SecretAuthenticationKey> secretAuthKeys =
				Lists.newArrayListWithCapacity(snaaKeys.size());

		for (SecretAuthenticationKey snaaKey : snaaKeys) {

			eu.wisebed.api.rs.SecretAuthenticationKey key = new eu.wisebed.api.rs.SecretAuthenticationKey();

			key.setSecretAuthenticationKey(snaaKey.getSecretAuthenticationKey());
			key.setUrnPrefix(snaaKey.getUrnPrefix());
			key.setUsername(snaaKey.getUsername());

			secretAuthKeys.add(key);
		}

		return secretAuthKeys;
	}

	@SuppressWarnings("unused")
	public static List<eu.wisebed.api.snaa.SecretAuthenticationKey> copyRsToSnaa(
			List<eu.wisebed.api.rs.SecretAuthenticationKey> snaaKeys) {

		List<SecretAuthenticationKey> secretAuthKeys = Lists.newArrayListWithCapacity(snaaKeys.size());

		for (eu.wisebed.api.rs.SecretAuthenticationKey snaaKey : snaaKeys) {

			SecretAuthenticationKey key = new SecretAuthenticationKey();

			key.setSecretAuthenticationKey(snaaKey.getSecretAuthenticationKey());
			key.setUrnPrefix(snaaKey.getUrnPrefix());
			key.setUsername(snaaKey.getUsername());

			secretAuthKeys.add(key);
		}

		return secretAuthKeys;
	}

	public static List<eu.wisebed.api.sm.SecretReservationKey> copyRsToWsn(List<SecretReservationKey> keys) {
		List<eu.wisebed.api.sm.SecretReservationKey> newKeys =
				new ArrayList<eu.wisebed.api.sm.SecretReservationKey>();

		for (SecretReservationKey key : keys) {
			eu.wisebed.api.sm.SecretReservationKey newKey =
					new eu.wisebed.api.sm.SecretReservationKey();
			newKey.setSecretReservationKey(key.getSecretReservationKey());
			newKey.setUrnPrefix(key.getUrnPrefix());
			newKeys.add(newKey);
		}

		return newKeys;
	}

	@SuppressWarnings("unused")
	public static List<eu.wisebed.api.sm.SecretReservationKey> parseSecretReservationKeys(String str) {
		String[] pairs = str.split(";");
		List<eu.wisebed.api.sm.SecretReservationKey> keys = Lists.newArrayList();
		for (String pair : pairs) {
			String urnPrefix = pair.split(",")[0];
			String secretReservationKeys = pair.split(",")[1];
			eu.wisebed.api.sm.SecretReservationKey key = new eu.wisebed.api.sm.SecretReservationKey();
			key.setUrnPrefix(urnPrefix);
			key.setSecretReservationKey(secretReservationKeys);
			keys.add(key);
		}
		return keys;
	}

	@SuppressWarnings("unused")
	public static String toString(Message msg, boolean legacyFormat) {

		if (!legacyFormat) {
			return toString(msg);
		}

		StringBuilder b = new StringBuilder();
		b.append("Source [");
		b.append(msg.getSourceNodeId());
		b.append("], ");
		b.append("Text [");
		b.append(new String(msg.getBinaryData(), 2, msg.getBinaryData().length - 2));
		b.append("], ");
		b.append("Level [");
		b.append(msg.getBinaryData()[1] == 0x00 ? "DEBUG" : "FATAL");
		b.append("], ");
		b.append("Time [");
		b.append(msg.getTimestamp().toXMLFormat());
		b.append("]");

		return b.toString();
	}

	public static String toString(Message msg) {
		StringBuilder b = new StringBuilder();
		b.append("Source [");
		b.append(msg.getSourceNodeId());
		b.append("]");

		if (msg.getBinaryData() != null) {
			b.append(", Binary [");
			b.append(StringUtils.toHexString(msg.getBinaryData()));
			b.append("]");
		}

		b.append(", Time[");
		b.append(msg.getTimestamp().toString());
		b.append("]");

		return b.toString();
	}

	@SuppressWarnings("unused")
	public static String toString(RequestStatus requestStatus) {
		StringBuilder b = new StringBuilder();
		b.append("RequestStatus [requestId=");
		b.append(requestStatus.getRequestId());
		b.append("] {");
		for (Status status : requestStatus.getStatus()) {
			b.append("(");
			b.append("nodeId=");
			b.append(status.getNodeId());
			b.append(";value=");
			b.append(status.getValue());
			b.append(";msg=\"");
			b.append(status.getMsg());
			b.append("),");
		}
		b.append("}");
		return b.toString();
	}

	@SuppressWarnings("unused")
	public static String toString(List<SecretReservationKey> secretReservationKeys) {
		StringBuilder b = new StringBuilder();
		for (Iterator<SecretReservationKey> secretReservationKeyIterator = secretReservationKeys.iterator();
			 secretReservationKeyIterator.hasNext(); ) {

			SecretReservationKey secretReservationKey = secretReservationKeyIterator.next();

			b.append(secretReservationKey.getUrnPrefix());
			b.append(",");
			b.append(secretReservationKey.getSecretReservationKey());

			if (secretReservationKeyIterator.hasNext()) {
				b.append("\\;");
			}
		}
		return b.toString();
	}

	public static String toString(Object object) {
		return object.toString();
	}

	@SuppressWarnings("unused")
	public static Vector<String> getExternalHostIps() {
		HashSet<String> ips = new HashSet<String>();
		Vector<String> external = new Vector<String>();

		try {
			InetAddress i;
			NetworkInterface networkInterface;

			for (Enumeration networkInterfaces = NetworkInterface.getNetworkInterfaces();
				 networkInterfaces.hasMoreElements(); ) {
				networkInterface = (NetworkInterface) networkInterfaces.nextElement();
				for (Enumeration addresses = networkInterface.getInetAddresses(); addresses.hasMoreElements(); ) {
					i = (InetAddress) addresses.nextElement();
					if (i instanceof Inet4Address) {
						ips.add(i.getHostAddress());
					}
				}
			}

		} catch (Throwable t) {
			log.error("Unable to retrieve external ips: " + t, t);

			try {
				log.debug("Trying different lookup scheme");

				InetAddress inetAddress = InetAddress.getLocalHost();
				String inetAddressHostName = inetAddress.getHostName();
				InetAddress[] inetAddresses = InetAddress.getAllByName(inetAddressHostName);
				for (InetAddress address : inetAddresses) {
					ips.add(address.getHostAddress());
				}
			} catch (Throwable t2) {
				log.error("Also unable to retrieve external ips: " + t2, t2);
			}

		}

		for (String ip : ips) {
			if (isExternalIp(ip)) {
				log.debug("Found external ip: " + ip);
				external.add(ip);
			}
		}

		return external;
	}

	/**
	 * Checks if the given IP address is an external (i.e. non-internal) IP address. Internal IP addresses lie within the
	 * following ranges:
	 * <p/>
	 * <ul> <li>127.0.0.0 - 127.255.255.255 (localhost)</li> <li>10.0.0.0 - 10.255.255.255  (10/8 prefix)</li>
	 * <li>172.16.0.0 - 172.31.255.255 (172.16/12 prefix)</li> <li>192.168.0.0 - 192.168.255.255 (192.168/16 prefix)</li>
	 * </ul>
	 *
	 * @param ip the IP address to check
	 *
	 * @return {@code true} if the given IP address is not private, {@code} false otherwise
	 */
	public static boolean isExternalIp(String ip) {

		if (ip == null) {
			return false;
		} else if (ip.startsWith("127.")) {
			return false;
		} else if (ip.startsWith("10.")) {
			return false;
		} else if (ip.startsWith("192.168.")) {
			return false;
		}

		for (int i = 16; i <= 31; ++i) {
			if (ip.startsWith("172." + i + ".")) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Get a List with all given node ids for use in a bean shell script
	 *
	 * @param nodes a list or node URNs
	 *
	 * @return List with nodes
	 */
	@SuppressWarnings("unused")
	public static List<String> getNodeList(String... nodes) {
		return Lists.newArrayList(nodes);
	}

	/**
	 * Generate a binary message to be send to a node with timestamp = now and src node id = 0xffff
	 *
	 * @param data Payload of the binary message
	 *
	 * @return the binary message
	 */
	@SuppressWarnings("unused")
	public static Message buildBinaryMessage(byte[] data) {
		Message msg = new Message();
		msg.setBinaryData(data);
		msg.setSourceNodeId("urn:wisebed:uzl1:0xffff");
		try {
			msg.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(
					(GregorianCalendar) GregorianCalendar.getInstance()
			)
			);
		} catch (DatatypeConfigurationException e) {
			log.error("Error creating timestamp " + e);
			e.printStackTrace();
		}

		return msg;
	}

	@SuppressWarnings("unused")
	public static List<AuthenticationTriple> createAuthData(final String urnPrefix, final String userName,
															final String password) {
		ArrayList<AuthenticationTriple> list = Lists.newArrayList();
		AuthenticationTriple authenticationTriple = new AuthenticationTriple();
		authenticationTriple.setUsername(userName);
		authenticationTriple.setPassword(password);
		authenticationTriple.setUrnPrefix(urnPrefix);
		return list;
	}
}
