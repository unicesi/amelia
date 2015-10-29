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

	private final static String DEFAULT_STOP_REGEX = "Press Ctrl\\+C to quit\\.\\.\\.|Call done!";

	private final String compositeName;

	private final String[] libpath;

	private String serviceName;

	private String methodName;

	private String[] arguments;

	public Execution(final String compositeName, final String[] libpath,
			final String stopRegexp) {
		super("frascati run " + compositeName + " ...", null, null, stopRegexp,
				compositeName + " has started");
		this.compositeName = compositeName;
		this.libpath = libpath;
	}

	public Execution(final String compositeName, final String[] libpath) {
		this(compositeName, libpath, DEFAULT_STOP_REGEX);
	}

	public Execution(final String compositeName, final String[] libpath,
			final String serviceName, final String methodName,
			final String stopRegexp) {

		this(compositeName, libpath, stopRegexp);
		this.serviceName = serviceName;
		this.methodName = methodName;
	}

	public Execution(final String compositeName, final String[] libpath,
			final String serviceName, final String methodName) {
		this(compositeName, libpath, serviceName, methodName,
				DEFAULT_STOP_REGEX);
	}

	public Execution(final String compositeName, final String[] libpath,
			final String serviceName, final String methodName,
			final String[] arguments, final String stopRegexp) {

		this(compositeName, libpath, serviceName, methodName, stopRegexp);
		this.arguments = arguments;
	}

	public Execution(final String compositeName, final String[] libpath,
			final String serviceName, final String methodName,
			final String[] arguments) {
		this(compositeName, libpath, serviceName, methodName, arguments,
				DEFAULT_STOP_REGEX);
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

}
