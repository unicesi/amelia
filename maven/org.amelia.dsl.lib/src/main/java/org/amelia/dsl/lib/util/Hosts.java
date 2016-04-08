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

import org.amelia.dsl.lib.descriptors.Host;

/**
 * @author Miguel Jiménez - Initial contribution and API
 * 
 * TODO: document this class
 */
public class Hosts {

	public static Host host(final String hostname, final int ftpPort,
			final int sshPort, final String username, final String password,
			final String identifier) {
		return new Host(hostname, ftpPort, sshPort, username, password,
				identifier);
	}

	public static Host host(final String hostname, final String username,
			final String password, final String identifier) {
		return new Host(hostname, 21, 22, username, password, identifier);
	}

	public static Host host(final String hostname, final String username,
			final String password) {
		return new Host(hostname, 21, 22, username, password);
	}

}
