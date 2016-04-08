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
package org.amelia.dsl.lib.util;

import java.util.UUID;

import org.amelia.dsl.lib.descriptors.CommandDescriptor;
import org.amelia.dsl.lib.descriptors.Host;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class ScheduledTask<T> implements CallableTask<T> {

	/**
	 * A unique identifier among all scheduled tasks
	 */
	protected final UUID internalId;
	
	/**
	 * The host in which the task will be executed
	 */
	protected final Host host;

	/**
	 * The descriptor of the scheduled task (command)
	 */
	protected final CommandDescriptor descriptor;

	public ScheduledTask(final Host host, final CommandDescriptor descriptor) {
		this.internalId = UUID.randomUUID();
		this.host = host;
		this.descriptor = descriptor;
	}
	
	@SuppressWarnings("unchecked")
	@Override public T call(Host host, String prompt) throws Exception {
		return (T) descriptor.callable().call(host, prompt);
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
		ScheduledTask<?> other = (ScheduledTask<?>) obj;
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
