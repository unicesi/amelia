/*
 * Copyright © 2015 Universidad Icesi
 * 
 * This file is part of the Amelia project.
 * 
 * The Amelia project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Amelia project is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the Amelia project. If not, see <http://www.gnu.org/licenses/>.
 */
package org.amelia.dsl.lib.util;

import java.util.ArrayList;
import java.util.List;

import org.amelia.dsl.lib.SubsystemGraph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Threads {

	/**
	 * The variable indicating whether any deployment is in trance of
	 * abortion
	 */
	private static volatile boolean globallyAborting = false;
	
	/**
	 * The list of identifiers corresponding to all hosts included in the
	 * current deployment
	 */
	private final static List<String> hostNames = new ArrayList<String>();
	
	/**
	 * The logger
	 */
	private static Logger logger = LogManager.getLogger(Threads.class);

	/**
	 * The uncaught exception handler for all threads
	 */
	private static Thread.UncaughtExceptionHandler exceptionHandler = 
			new Thread.UncaughtExceptionHandler() {
		public void uncaughtException(Thread t, Throwable e) {
			if (!globallyAborting) {
				globallyAborting = true;
				String message = "";
				if (e.getMessage() != null) {
					message = ": " + e.getMessage().replaceAll(
							"^((\\w)+(\\.\\w+)+:\\s)*", "");						
				}
				logger.error(message, e);
				Log.error("Stopping deployment" + message);
				SubsystemGraph.getInstance().shutdown(true);
			}
		}
	};

	private Threads() {
	}
	
	/**
	 * Gives default values to global variables
	 */
	public static void reset() {
		globallyAborting = false;
		hostNames.clear();
	}
	
	public static List<String> hostNames() {
		return hostNames;
	}
	
	public static boolean isAnySubsystemAborting() {
		return globallyAborting;
	}

	public static Thread.UncaughtExceptionHandler exceptionHandler() {
		return exceptionHandler;
	}

}
