/*
 * Copyright © 2015 Universidad Icesi
 * 
 * This file is part of the Amelia library.
 * 
 * The Amelia library is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Amelia library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the Amelia library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.amelia.dsl.lib.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Configuration {
	
	/**
	 * The logger
	 */
	private static Logger logger = LogManager.getLogger(Configuration.class);
	
	/**
	 * Reads configuration properties
	 */
	public void setProperties() {
		if (System.getProperty("amelia.config") != null)
			return;

		System.setProperty("amelia.config", "true");
		Properties config = new Properties();
		InputStream input = null;

		try {
			input = Configuration.class.getClassLoader()
					.getResourceAsStream("amelia.properties");
			if (input != null)
				config.load(input);

		} catch (FileNotFoundException e) {
			logger.warn("No configuration file was found. Execution is started with default values");
		} catch (IOException e) {
			logger.error("Error loading configuration file. Execution is started with default values");
		} finally {
			String home = System.getProperty("user.home");

			// Set defaults
			if (!config.containsKey("identity"))
				config.put("identity", home + "/.ssh/id_rsa");
			if (!config.containsKey("known_hosts"))
				config.put("known_hosts", home + "/.ssh/known_hosts");
			if (!config.containsKey("connection_timeout"))
				config.put("connection_timeout", "10000"); // 0 for no
															// timeout
			if (!config.containsKey("execution_timeout"))
				config.put("execution_timeout", "15000");
			if (!config.containsKey("color_output"))
				config.put("color_output", "true");

			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					logger.error("Error closing stream of configuration file",
							e);
				}
			}
		}

		for (Object key : config.keySet()) {
			String name = (String) key;
			System.setProperty("amelia." + name, config.getProperty(name));
		}
	}

}
