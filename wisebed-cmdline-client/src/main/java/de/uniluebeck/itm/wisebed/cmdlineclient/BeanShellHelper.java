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

import de.uniluebeck.itm.tr.util.StringUtils;
import eu.wisebed.testbed.api.rs.v1.ConfidentialReservationData;
import eu.wisebed.testbed.api.rs.v1.Data;
import eu.wisebed.testbed.api.rs.v1.SecretReservationKey;
import eu.wisebed.testbed.api.snaa.v1.SecretAuthenticationKey;
import eu.wisebed.testbed.api.wsn.v211.GetInstance;
import eu.wisebed.testbed.api.wsn.v211.Message;
import eu.wisebed.testbed.api.wsn.v211.Program;
import eu.wisebed.testbed.api.wsn.v211.ProgramMetaData;
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

	public ConfidentialReservationData generateConfidentialReservationData(List<String> nodeURNs, Date from,
																		   int duration, TimeUnit durationUnit,
																		   String urnPrefix, String username) {

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
			reservationData.setUserData("demo-script");

			Data data = new Data();
			data.setUrnPrefix(urnPrefix);
			data.setUsername(username);

			reservationData.getData().add(data);

			return reservationData;

		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}

	}

	public List<SecretAuthenticationKey> generateFakeSNAAAuthentication(String urnPrefix, String username,
																		String secretAuthenticationKey) {
		List<SecretAuthenticationKey> secretAuthKeys = new ArrayList<SecretAuthenticationKey>();

		SecretAuthenticationKey key = new SecretAuthenticationKey();
		key.setSecretAuthenticationKey(secretAuthenticationKey);
		key.setUrnPrefix(urnPrefix);
		key.setUsername(username);

		secretAuthKeys.add(key);
		return secretAuthKeys;
	}

	public List<SecretReservationKey> generateFakeReservationKeys(String urnPrefix, String secretReservationKey) {
		List<SecretReservationKey> reservations = new ArrayList<SecretReservationKey>();

		SecretReservationKey key = new SecretReservationKey();
		key.setSecretReservationKey(secretReservationKey);
		key.setUrnPrefix(urnPrefix);

		reservations.add(key);

		return reservations;
	}

	public List<eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey> copySnaaToRs(
			List<SecretAuthenticationKey> snaaKeys) {
		List<eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey> secretAuthKeys =
				new ArrayList<eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey>();

		for (SecretAuthenticationKey snaaKey : snaaKeys) {
			eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey key =
					new eu.wisebed.testbed.api.rs.v1.SecretAuthenticationKey();
			key.setSecretAuthenticationKey(snaaKey.getSecretAuthenticationKey());
			key.setUrnPrefix(snaaKey.getUrnPrefix());
			key.setUsername(snaaKey.getUsername());

			secretAuthKeys.add(key);
		}

		return secretAuthKeys;
	}

	public List<eu.wisebed.testbed.api.wsn.v211.SecretReservationKey> copyRsToWsn(List<SecretReservationKey> keys) {
		List<eu.wisebed.testbed.api.wsn.v211.SecretReservationKey> newKeys =
				new ArrayList<eu.wisebed.testbed.api.wsn.v211.SecretReservationKey>();

		for (SecretReservationKey key : keys) {
			eu.wisebed.testbed.api.wsn.v211.SecretReservationKey newKey =
					new eu.wisebed.testbed.api.wsn.v211.SecretReservationKey();
			newKey.setSecretReservationKey(key.getSecretReservationKey());
			newKey.setUrnPrefix(key.getUrnPrefix());
			newKeys.add(newKey);
		}

		return newKeys;
	}

	public String toString(Message msg) {
		StringBuilder b = new StringBuilder();
		b.append("Source [");
		b.append(msg.getSourceNodeId());
		b.append("]");

		if (msg.getTextMessage() != null) {
			b.append(", Text [");
			b.append(msg.getTextMessage().getMsg());
			b.append("], Level[");
			b.append(msg.getTextMessage().getMessageLevel());
			b.append("]");
		}

		if (msg.getBinaryMessage() != null) {
			b.append(", BinaryType[");
			b.append(StringUtils.toHexString(msg.getBinaryMessage().getBinaryType()));
			b.append("], Binary [");
			b.append(StringUtils.toHexString(msg.getBinaryMessage().getBinaryData()));
			b.append("]");
		}

		b.append(", Time[");
		b.append(msg.getTimestamp().toString());
		b.append("]");

		return b.toString();
	}

	public static Vector<String> getExternalHostIps() {
		HashSet<String> ips = new HashSet<String>();
		Vector<String> external = new Vector<String>();

		try {
			InetAddress i;
			NetworkInterface iface = null;

			for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
				iface = (NetworkInterface) ifaces.nextElement();
				for (Enumeration ifaceips = iface.getInetAddresses(); ifaceips.hasMoreElements();) {
					i = (InetAddress) ifaceips.nextElement();
					if (i instanceof Inet4Address)
						ips.add(i.getHostAddress());
				}
			}

		} catch (Throwable t) {
			log.error("Unable to retrieve external ips: " + t, t);

			try {
				log.debug("Trying different lookup scheme");

				InetAddress li = InetAddress.getLocalHost();
				String strli = li.getHostName();
				InetAddress ia[] = InetAddress.getAllByName(strli);
				for (int i = 0; i < ia.length; i++)
					ips.add(ia[i].getHostAddress());
			} catch (Throwable t2) {
				log.error("Also unable to retrieve external ips: " + t2, t2);
			}

		}

		for (String ip : ips)
			if (isExternalIp(ip)) {
				log.debug("Found external ip: " + ip);
				external.add(ip);
			}

		return external;
	}
	
	/**
	 * 127.0.0.0   - 127.255.255.255 (localhost)
	 * 10.0.0.0    - 10.255.255.255  (10/8 prefix)
	 * 172.16.0.0  - 172.31.255.255  (172.16/12 prefix)
	 * 192.168.0.0 - 192.168.255.255 (192.168/16 prefix)
	 *
	 * @param ip
	 * @return
	 */
	public static boolean isExternalIp(String ip) {
		boolean external = true;

		if (ip == null)
			external = false;
		else if (ip.startsWith("127."))
			external = false;
		else if (ip.startsWith("10."))
			external = false;
		else if (ip.startsWith("192.168."))
			external = false;

		for (int i = 16; i <= 31; ++i)
			if (ip.startsWith("172." + i + "."))
				external = false;

		log.debug("IP " + ip + " is an " + (external ? "external" : "internal") + " address");
		return external;
	}

}
