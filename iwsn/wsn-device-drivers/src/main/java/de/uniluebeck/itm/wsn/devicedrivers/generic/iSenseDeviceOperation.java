/**********************************************************************************************************************
 * Copyright (c) 2010, coalesenses GmbH                                                                               *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the coalesenses GmbH nor the names of its contributors may be used to endorse or promote     *
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

package de.uniluebeck.itm.wsn.devicedrivers.generic;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dp
 */
public abstract class iSenseDeviceOperation extends Thread {
	/**
	 * Logging
	 */
	private final Logger log = LoggerFactory.getLogger(getClass());

	/** */
	private boolean cancelled = false;

	/** */
	private boolean done = false;

	/** */
	private iSenseDeviceImpl device;

	/** */
	protected String logIdentifier;

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public abstract Operation getOperation();

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public abstract void run();

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public iSenseDeviceOperation(iSenseDeviceImpl device) {
		this.device = device;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public final void cancelOperation() {
		cancelled = true;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public boolean isCancelled() {
		return cancelled;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	public boolean isDone() {
		return done;
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	protected void operationDone(Object result) {
		log.debug("Operation is done " + getOperation() + ", result: " + result);
		done = true;
		device.operationDone(getOperation(), result);
	}

	// -------------------------------------------------------------------------

	/**
	 *
	 */
	protected iSenseDeviceImpl getDevice() {
		return device;
	}

	public void setLogIdentifier(String logIdentifier) {
		this.logIdentifier = logIdentifier == null ? null : logIdentifier.endsWith(" ") ? logIdentifier : logIdentifier + " ";
	}

	protected void logDebug(String format, Object... args) {
		if (log.isDebugEnabled()) {
			if (logIdentifier != null) {
				log.debug(logIdentifier + format, args);
			} else {
				Object[] newArgs = new Object[2 + args.length];
				newArgs[0] = device.getClass().getSimpleName();
				newArgs[1] = device.getSerialPort();
				System.arraycopy(args, 0, newArgs, 2, args.length);
				log.debug("[{},{}] " + format, newArgs);
			}
		}
	}

	protected void logTrace(String format, Object... args) {
		if (log.isTraceEnabled()) {
			if (logIdentifier != null) {
				log.trace(logIdentifier + format, args);
			} else {
				Object[] newArgs = new Object[2 + args.length];
				newArgs[0] = device.getClass().getSimpleName();
				newArgs[1] = device.getSerialPort();
				System.arraycopy(args, 0, newArgs, 2, args.length);
				log.trace("[{},{}] " + format, newArgs);
			}
		}
	}

	protected void logInfo(String format, Object... args) {
		if (log.isInfoEnabled()) {
			if (logIdentifier != null) {
				log.info(logIdentifier + format, args);
			} else {
				Object[] newArgs = new Object[2 + args.length];
				newArgs[0] = device.getClass().getSimpleName();
				newArgs[1] = device.getSerialPort();
				System.arraycopy(args, 0, newArgs, 2, args.length);
				log.info("[{},{}] " + format, newArgs);
			}
		}
	}

	protected void logWarn(String format, Object... args) {
		if (log.isWarnEnabled()) {
			if (logIdentifier != null) {
				log.warn(logIdentifier + format, args);
			} else {
				Object[] newArgs = new Object[2 + args.length];
				newArgs[0] = device.getClass().getSimpleName();
				newArgs[1] = device.getSerialPort();
				System.arraycopy(args, 0, newArgs, 2, args.length);
				log.warn("[{},{}] " + format, newArgs);
			}
		}
	}

	protected void logError(String format, Object... args) {
		if (log.isErrorEnabled()) {
			if (logIdentifier != null) {
				log.error(logIdentifier + format, args);
			} else {
				Object[] newArgs = new Object[2 + args.length];
				newArgs[0] = device.getClass().getSimpleName();
				newArgs[1] = device.getSerialPort();
				System.arraycopy(args, 0, newArgs, 2, args.length);
				log.error("[{},{}] " + format, newArgs);
			}
		}
	}

}
