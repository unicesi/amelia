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
package org.amelia.dsl.lib.descriptors;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.UUID;

import org.amelia.dsl.lib.commands.CommandFactory;
import org.amelia.dsl.lib.util.ShellUtils;
import org.amelia.dsl.lib.util.Strings;

/**
 * @see CommandFactory
 * @author Miguel Jiménez - Initial contribution and API
 */
public class CommandDescriptor extends Observable {
	
	public static class Builder {

		private String command;
		private String[] arguments;
		private String releaseRegexp;
		private long timeout;
		private String errorText;
		private String errorMessage;
		private String successMessage;

		public Builder() {
			this.command = "";
			this.arguments = new String[]{};
			this.releaseRegexp = ShellUtils.ameliaPromptRegexp();
			this.timeout = 0;
			this.errorMessage = "";
			this.successMessage = "";
		}
		
		public Builder withCommand(final String command) {
			this.command = command;
			return this;
		}

		public Builder withArguments(final String... arguments) {
			this.arguments = arguments;
			return this;
		}
		
		public Builder withReleaseRegexp(final String regularExpression) {
			this.releaseRegexp = regularExpression;
			return this;
		}

		public Builder withTimeout(final long timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder withoutTimeout() {
			this.timeout = -1;
			return this;
		}
		
		public Builder withErrorText(String errorText) {
			this.errorText = errorText;
			return this;
		}
		
		public Builder withErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
			return this;
		}
		
		public Builder withSuccessMessage(String successMessage) {
			this.successMessage = successMessage;
			return this;
		}

		public CommandDescriptor build() {
			return new CommandDescriptor(this);
		}
	}

	protected final UUID internalId;
	protected final String command;
	protected final String errorText;
	protected final String errorMessage;
	protected final String releaseRegexp;
	protected final String successMessage;
	protected final long timeout;
	protected final List<CommandDescriptor> dependencies;
	protected final List<Host> hosts;

	public CommandDescriptor(final Builder builder) {
		this.internalId = UUID.randomUUID();
		this.command = builder.command + " "
				+ Strings.join(builder.arguments, " ");
		this.releaseRegexp = builder.releaseRegexp;
		this.timeout = builder.timeout;
		this.errorText = builder.errorText;
		this.errorMessage = builder.errorMessage;
		this.successMessage = builder.successMessage;
		this.dependencies = new ArrayList<CommandDescriptor>();
		this.hosts = new ArrayList<Host>();
	}

	public boolean isOk(String response) {
		return this.errorText == null || !response.contains(this.errorText);
	}

	public void done(Host host) {
		setChanged();
	}

	public String doneMessage() {
		return successMessage == null ? toString() : successMessage;
	}

	public String failMessage() {
		return errorMessage == null ? toString() : errorMessage;
	}

	public String toCommandString() {
		return this.command;
	}
	
	public boolean dependsOn(CommandDescriptor... dependencies) {
		boolean all = true;
		for (CommandDescriptor descriptor : dependencies) {
			if (this.dependencies.contains(dependencies)) {
				all = false;
				continue;
			}
			this.dependencies.add(descriptor);
		}
		return all;
	}
	
	public boolean runsOn(Host... hosts) {
		boolean all = true;
		for (Host host : hosts) {
			if (this.hosts.contains(host)) {
				all = false;
				continue;
			}
			this.hosts.add(host);
		}
		return all;
	}
	
	public List<Host> hosts() {
		return this.hosts;
	}
	
	public List<CommandDescriptor> dependencies() {
		return this.dependencies;
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
	
	public long timeout() {
		return this.timeout;
	}

	public String errorText() {
		return this.errorText;
	}

	public String errorMessage() {
		return this.errorMessage;
	}

	public String releaseRegexp() {
		return this.releaseRegexp;
	}

	public String successMessage() {
		return this.successMessage;
	}
}
