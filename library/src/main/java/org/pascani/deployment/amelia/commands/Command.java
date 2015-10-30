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

import java.util.UUID;
import java.util.concurrent.Callable;

import net.sf.expectit.Expect;

import org.pascani.deployment.amelia.descriptors.CommandDescriptor;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.util.Log;

/**
 * @see CommandFactory
 * @author Miguel Jiménez - Initial contribution and API
 */
public abstract class Command<T> implements Callable<T> {

	public static class Simple extends Command<Boolean> {

		public Simple(Host host, CommandDescriptor descriptor) {
			super(host, descriptor);
		}

		public Boolean call() throws Exception {

			Host host = super.host;
			CommandDescriptor descriptor = super.descriptor;

			Expect expect = host.ssh().expect();
			String expression = descriptor.stopRegexp();

			expect.sendLine(descriptor.toCommandString());
			String response = expect.expect(regexp(expression)).getBefore();

			if (!descriptor.isOk(response)) {
				Log.error(host, descriptor.failMessage());
				throw new Exception(descriptor.errorMessage());
			} else {
				Log.info(host, descriptor.doneMessage());
			}

			return true;
		}
	}

	protected final UUID internalId;

	protected final Host host;

	protected final CommandDescriptor descriptor;

	public Command(final Host host, final CommandDescriptor descriptor) {
		this.internalId = UUID.randomUUID();
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((this.internalId == null) ? 0 : this.internalId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Command<?> other = (Command<?>) obj;
		if (this.internalId == null) {
			if (other.internalId != null) {
				return false;
			}
		} else if (!this.internalId.equals(other.internalId)) {
			return false;
		}
		return true;
	}

}
