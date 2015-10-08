package org.pascani.deployment.amelia.descriptors;

import java.util.Arrays;
import java.util.Observable;

import org.pascani.deployment.amelia.util.Strings;

public class CompilationDescriptor  extends Observable {

	private final String sourceDirectory;

	private final String outputFile;

	private final String[] classpath;

	public CompilationDescriptor(final String sourceDirectory,
			final String outputFile, final String... classpath) {

		this.sourceDirectory = sourceDirectory;
		this.outputFile = outputFile;
		this.classpath = classpath;
	}

	public String toCommandString() {
		StringBuilder sb = new StringBuilder();

		sb.append("frascati ");
		sb.append("compile ");
		sb.append(this.sourceDirectory + " ");
		sb.append(this.outputFile + " ");
		sb.append(Strings.join(this.classpath, ":") + " ");

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
