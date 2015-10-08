package org.pascani.deployment.amelia.descriptors;

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
