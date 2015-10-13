package org.pascani.deployment.amelia.descriptors;

import java.util.Arrays;

import org.pascani.deployment.amelia.util.Strings;

public class ExecutionDescriptor extends CommandDescriptor {

	private final String compositeName;

	private final String[] libpath;

	private String serviceName;

	private String methodName;

	private String[] arguments;

	public ExecutionDescriptor(final String compositeName,
			final String[] libpath) {
		super(null, null, null, compositeName + " has started");
		this.compositeName = compositeName;
		this.libpath = libpath;
	}

	public ExecutionDescriptor(final String compositeName,
			final String[] libpath, final String serviceName,
			final String methodName) {

		this(compositeName, libpath);
		this.serviceName = serviceName;
		this.methodName = methodName;
	}

	public ExecutionDescriptor(final String compositeName,
			final String[] libpath, final String serviceName,
			final String methodName, final String[] arguments) {

		this(compositeName, libpath, serviceName, methodName);
		this.arguments = arguments;
	}

	public String toCommandSearchString() {
		StringBuilder sb = new StringBuilder();

		sb.append(compositeName + " ");
		sb.append("-libpath ");
		sb.append(Strings.join(this.libpath, ":"));

		return sb.toString();
	}

	@Override
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(this.arguments);
		result = prime
				* result
				+ ((this.compositeName == null) ? 0 : this.compositeName
						.hashCode());
		result = prime * result + Arrays.hashCode(this.libpath);
		result = prime * result
				+ ((this.methodName == null) ? 0 : this.methodName.hashCode());
		result = prime
				* result
				+ ((this.serviceName == null) ? 0 : this.serviceName.hashCode());
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
		ExecutionDescriptor other = (ExecutionDescriptor) obj;
		if (!Arrays.equals(this.arguments, other.arguments))
			return false;
		if (this.compositeName == null) {
			if (other.compositeName != null)
				return false;
		} else if (!this.compositeName.equals(other.compositeName))
			return false;
		if (!Arrays.equals(this.libpath, other.libpath))
			return false;
		if (this.methodName == null) {
			if (other.methodName != null)
				return false;
		} else if (!this.methodName.equals(other.methodName))
			return false;
		if (this.serviceName == null) {
			if (other.serviceName != null)
				return false;
		} else if (!this.serviceName.equals(other.serviceName))
			return false;
		return true;
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
