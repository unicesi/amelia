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
package org.pascani.deployment.amelia.commands;

import static net.sf.expectit.matcher.Matchers.regexp;

import java.util.concurrent.Callable;

import net.sf.expectit.Expect;

import org.pascani.deployment.amelia.DeploymentException;
import org.pascani.deployment.amelia.descriptors.CommandDescriptor;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.util.DependencyGraph;
import org.pascani.deployment.amelia.util.ShellUtils;

/**
 * @see DependencyGraph#addElement(CommandDescriptor, Host...)
 * @author Miguel Jiménez - Initial contribution and API
 */
public abstract class Command<T> implements Callable<T> {

	public static class Simple extends Command<Boolean> {

		public Simple(Host host, CommandDescriptor descriptor) {
			super(host, descriptor);
		}

		public Boolean call() throws Exception {

			Expect expect = this.host.ssh().expect();
			String prompt = ShellUtils.ameliaPromptRegexp();

			expect.sendLine(this.descriptor.toCommandString());
			String response = expect.expect(regexp(prompt)).getBefore();

			if (!this.descriptor.isOk(response)) {
				this.descriptor.fail(this.host);
				throw new DeploymentException(this.descriptor.errorMessage());
			}

			return true;
		}
	}

	protected final Host host;

	protected final CommandDescriptor descriptor;

	public Command(final Host host, final CommandDescriptor descriptor) {
		this.host = host;
		this.descriptor = descriptor;
	}

	public abstract T call() throws Exception;

	public Host host() {
		return this.host;
	}

	public CommandDescriptor descriptor() {
		return this.descriptor;
	}

}
