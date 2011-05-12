/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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

package de.uniluebeck.itm.deviceobserver.util;

import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class is a Helper-Class to extract csv from the os-specific motelist-script
 *
 */
public class CSVMoteListExtractor {
	private final static String motelist_win32_exe = "motelist-win32.exe";
	private final static String motelist_linux = "motelist-linux";
	private final static String motelist_macos = "motelist-macos";

	private static File tmpFile;
	private final static Logger logger = LoggerFactory.getLogger(CSVMoteListExtractor.class);

	/**
	 *
	 * @return Filename of Motelist-script dependent on your os
	 */
	private static String getMotelistScriptFromOS() {
		String resource = "";
		if (SystemUtils.IS_OS_WINDOWS) {
			resource += motelist_win32_exe;
		} else if (SystemUtils.IS_OS_MAC_OSX) {
			resource += motelist_macos;
		} else if (SystemUtils.IS_OS_LINUX) {
			resource += motelist_linux;
		} else {
			throw new RuntimeException("Only Linux, Mac OS X and Windows are supported by this version!");
		}
		return resource;
	}

	/**
	 * Main-Method to be called from outside to retrieve the current connected devices
	 * Starts a process on the motelist-script returning the devices
	 * @return String-array of csv-strings for all connected devices;
	 */
	public static String[] getDeviceListAsCSV() {
		List<String> csvList = new ArrayList<String>();
		ProcessBuilder pb = null;
		try {
			copyScriptToTmpFile();
			//creating processbuilder on the motelist-script
			pb = new ProcessBuilder(tmpFile.getAbsolutePath(), "-c");
			Process p = pb.start();

			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				StringTokenizer tokenizer = new StringTokenizer(line, ",");

				if (tokenizer.countTokens() != 3) {
					logger.info(line);
				} else {
					csvList.add(line);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return csvList.toArray(new String[csvList.size()]);
	}

	/**
	 * helper-method to copy motelist-script to a tmp-file
	 * @return the motelist-script as an executable tmp-file
	 */
	private static File copyScriptToTmpFile() {
		if (tmpFile == null || !tmpFile.exists()) {
			try {
				InputStream from = CSVMoteListExtractor.class.getClassLoader().getResourceAsStream(getMotelistScriptFromOS());
				FileOutputStream to = null;
				try {
					tmpFile = File.createTempFile("motelist", "");
					to = new FileOutputStream(tmpFile);
					byte[] buffer = new byte[4096];
					int bytesRead;

					while ((bytesRead = from.read(buffer)) != -1) {
						to.write(buffer, 0, bytesRead);
					} // write

				} finally {
					if (from != null) {
						try {
							from.close();
						} catch (IOException e) {
							logger.debug("" + e, e);
						}
					}
					if (to != null) {
						try {
							to.close();
						} catch (IOException e) {
							logger.debug("" + e, e);
						}
					}
				}

				tmpFile.setExecutable(true);
				return tmpFile;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return tmpFile;

	}

}
