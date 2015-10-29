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
public class Execution extends CommandDescriptor {

	public static class Builder {
		
		private String compositeName;
		private String[] libpath;
		private String serviceName;
		private String methodName;
		private String[] arguments;
		private long timeout;
		private String stopRegexp;
		
		public Builder() {
			this.timeout = 0;
			this.stopRegexp = "Press Ctrl\\+C to quit\\.\\.\\.|Call done!";
		}

		public Builder withComposite(final String compositeName) {
			this.compositeName = compositeName;
			return this;
		}

		public Builder withLibpath(final String... libs) {
			this.libpath = libs;
			return this;
		}

		public Builder withService(final String serviceName) {
			this.serviceName = serviceName;
			return this;
		}

		public Builder withMethod(final String methodName) {
			this.methodName = methodName;
			return this;
		}

		public Builder withArguments(final String... arguments) {
			this.arguments = arguments;
			return this;
		}

		public Builder withTimeout(long timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder withoutTimeout() {
			this.timeout = -1;
			return this;
		}

		public Execution build() {
			return new Execution(this);
		}
	}

	private final String compositeName;
	private final String[] libpath;
	private final String serviceName;
	private final String methodName;
	private final String[] arguments;
	private final long timeout;

	private Execution(Builder builder) {
		super("frascati run " + builder.compositeName + " ...", null, null,
				builder.stopRegexp, builder.compositeName + " has started");
		this.compositeName = builder.compositeName;
		this.libpath = builder.libpath;
		this.serviceName = builder.serviceName;
		this.methodName = builder.methodName;
		this.arguments = builder.arguments;
		this.timeout = builder.timeout;
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

	public long timeout() {
		return this.timeout;
	}

}
