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
package org.pascani.deployment.amelia.descriptors;

import org.pascani.deployment.amelia.util.ANSI;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class ChangeDirectory extends CommandDescriptor {

	private final String directory;

	public ChangeDirectory(final String directory) {
		super("cd " + directory, "", "Could not change working directory to "
				+ ANSI.YELLOW.format(directory), "Working directory: "
				+ ANSI.YELLOW.format(directory));

		this.directory = directory;
	}

	public String directory() {
		return this.directory;
	}

}
