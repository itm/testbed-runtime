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
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.util;

import org.apache.log4j.*;


public class Logging {

	public static void setLoggingDefaults() {

		// configure logging defaults
		Appender appender = new ConsoleAppender(new PatternLayout("%-23d{yyyy-MM-dd HH:mm:ss,SSS} | %-25.25t | %-25.25C{1} | %-5p | %m%n"));

		Logger itmLogger = Logger.getLogger("de.uniluebeck.itm");
		Logger wisebedLogger = Logger.getLogger("eu.wisebed");
        Logger coaLogger = Logger.getLogger("com.coalesenses");

		if (!itmLogger.getAllAppenders().hasMoreElements()) {
			itmLogger.addAppender(appender);
			itmLogger.setLevel(Level.DEBUG);
		}

		if (!wisebedLogger.getAllAppenders().hasMoreElements()) {
			wisebedLogger.addAppender(appender);
			wisebedLogger.setLevel(Level.DEBUG);
		}

        if (!coaLogger.getAllAppenders().hasMoreElements()) {
			coaLogger.addAppender(appender);
			coaLogger.setLevel(Level.INFO);
		}
	}


        public static void setDebugLoggingDefaults() {
		PatternLayout patternLayout = new PatternLayout("%-13d{HH:mm:ss,SSS} | %-20.20C{3} | %-5p | %m%n");
                final Appender appender = new ConsoleAppender(patternLayout);
                Logger.getRootLogger().removeAllAppenders();
                Logger.getRootLogger().addAppender(appender);
                Logger.getRootLogger().setLevel(Level.DEBUG);
	}
}
