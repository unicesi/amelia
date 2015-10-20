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

import static org.pascani.deployment.amelia.util.Strings.ascii;

import java.util.Observable;
import java.util.UUID;

import org.pascani.deployment.amelia.commands.CommandFactory;
import org.pascani.deployment.amelia.util.Log;

/**
 * @see CommandFactory
 * @author Miguel Jiménez - Initial contribution and API
 */
public class CommandDescriptor extends Observable {
	
	protected final UUID internalId;

	protected final String command;

	protected final String errorText;

	protected final String errorMessage;

	protected final String successMessage;

	public CommandDescriptor(final String command, final String errorText,
			final String errorMessage, final String successMessage) {
		this.internalId = UUID.randomUUID();
		this.command = command;
		this.errorText = errorText;
		this.errorMessage = errorMessage;
		this.successMessage = successMessage;
	}

	public CommandDescriptor(final String command, final String errorText,
			final String errorMessage) {
		this(command, errorText, errorMessage, null);
	}

	public boolean isOk(String response) {
		return this.errorText == null || !response.contains(this.errorText);
	}

	public void done(Host host) {
		setChanged();
		Log.info(host.toFixedString() + " " + doneMessage());
	}

	public void fail(Host host) {
		Log.info(host.toFixedString() + " " + failMessage());
	}

	public String doneMessage() {
		String message = ascii(10003) + " ";
		message += successMessage == null ? toString() : successMessage;

		return message;
	}

	public String failMessage() {
		String message = ascii(10007) + " ";
		message += errorMessage == null ? toString() : errorMessage;

		return message;
	}

	public String toCommandString() {
		return this.command;
	}

	@Override
	public String toString() {
		return "[" + this.command + "]";
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
		CommandDescriptor other = (CommandDescriptor) obj;
		if (this.internalId == null) {
			if (other.internalId != null) {
				return false;
			}
		} else if (!this.internalId.equals(other.internalId)) {
			return false;
		}
		return true;
	}

	public String errorText() {
		return this.errorText;
	}

	public String errorMessage() {
		return this.errorMessage;
	}
}
