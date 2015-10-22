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

import org.pascani.deployment.amelia.util.Strings;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Compilation extends CommandDescriptor {

	private final String sourceDirectory;

	private final String outputFile;

	private final String[] classpath;

	public Compilation(final String sourceDirectory,
			final String outputFile, final String... classpath) {
		super(null, null, null, outputFile + ".jar has been generated");
		this.sourceDirectory = sourceDirectory;
		this.outputFile = outputFile;
		this.classpath = classpath;
	}

	@Override
	public String toCommandString() {
		StringBuilder sb = new StringBuilder();

		sb.append("frascati ");
		sb.append("compile ");
		sb.append(this.sourceDirectory + " ");
		sb.append(this.outputFile + " ");
		sb.append(Strings.join(this.classpath, ":"));

		return sb.toString();
	}

	@Override
	public String toString() {
		return "[compile " + this.sourceDirectory + "]";
	}

	public String sourceDirectory() {
		return this.sourceDirectory;
	}

	public String outputFile() {
		return this.outputFile;
	}

	public String[] classpath() {
		return this.classpath;
	}

}
