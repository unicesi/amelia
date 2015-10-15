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

import java.util.Arrays;

import org.pascani.deployment.amelia.util.Strings;

public class CompilationDescriptor extends CommandDescriptor {

	private final String sourceDirectory;

	private final String outputFile;

	private final String[] classpath;

	public CompilationDescriptor(final String sourceDirectory,
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(this.classpath);
		result = prime * result
				+ ((this.outputFile == null) ? 0 : this.outputFile.hashCode());
		result = prime
				* result
				+ ((this.sourceDirectory == null) ? 0 : this.sourceDirectory
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CompilationDescriptor other = (CompilationDescriptor) obj;
		if (!Arrays.equals(this.classpath, other.classpath))
			return false;
		if (this.outputFile == null) {
			if (other.outputFile != null)
				return false;
		} else if (!this.outputFile.equals(other.outputFile))
			return false;
		if (this.sourceDirectory == null) {
			if (other.sourceDirectory != null)
				return false;
		} else if (!this.sourceDirectory.equals(other.sourceDirectory))
			return false;
		return true;
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
