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
package org.amelia.dsl.lib.commands;

import java.util.UUID;

import org.amelia.dsl.lib.descriptors.CommandDescriptor;
import org.amelia.dsl.lib.descriptors.Host;
import org.amelia.dsl.lib.util.CallableTask;

/**
 * @see CommandFactory
 * @author Miguel Jiménez - Initial contribution and API
 */
public abstract class Command<T> implements CallableTask<T> {

	public static class Simple extends Command<Void> {

		public Simple(final Host host, CommandDescriptor descriptor) {
			super(host, descriptor);
		}

		@Override public Void call(Host host, String prompt) throws Exception {
			super.descriptor.callable().call(host, prompt);
			return null;
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
