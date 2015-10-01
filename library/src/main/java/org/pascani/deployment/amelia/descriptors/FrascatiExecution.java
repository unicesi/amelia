package org.pascani.deployment.amelia.descriptors;

import org.pascani.deployment.amelia.util.Strings;

public class FrascatiExecution {

	private final String compositeName;

	private final String[] libpath;

	private String serviceName;

	private String methodName;

	private String[] arguments;

	public FrascatiExecution(final String compositeName, final String[] libpath) {
		this.compositeName = compositeName;
		this.libpath = libpath;
	}

	public FrascatiExecution(final String compositeName,
			final String[] libpath, final String serviceName,
			final String methodName) {

		this(compositeName, libpath);
		this.serviceName = serviceName;
		this.methodName = methodName;
	}

	public FrascatiExecution(final String compositeName,
			final String[] libpath, final String serviceName,
			final String methodName, final String[] arguments) {

		this(compositeName, libpath, serviceName, methodName);
		this.arguments = arguments;
	}

	public String toCommandString() {
		StringBuilder sb = new StringBuilder();

		sb.append("frascati ");
		sb.append("run ");
		sb.append(compositeName + " ");
		sb.append("-libpath ");
		sb.append(Strings.join(this.libpath, ":") + " ");

		if (this.serviceName != null && this.methodName != null) {
			sb.append("-s ");
			sb.append(this.serviceName + " ");
			sb.append("-m ");
			sb.append(this.methodName + " ");
		}

		if (this.arguments != null) {
			sb.append("-p ");
			sb.append(Strings.join(this.arguments, " "));
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return "[run " + this.compositeName + "]";
	}

	public String compositeName() {
		return this.compositeName;
	}

	public String[] libpath() {
		return this.libpath;
	}

	public String serviceName() {
		return this.serviceName;
	}

	public String methodName() {
		return this.methodName;
	}

	public String[] arguments() {
		return this.arguments;
	}

}
