/*
 * Copyright © 2015 Universidad Icesi
 * 
 * This file is part of the Amelia project.
 * 
 * The Amelia project is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Amelia project is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the Amelia project. If not, see <http://www.gnu.org/licenses/>.
 */
package org.amelia.dsl.lib.descriptors;

import static net.sf.expectit.matcher.Matchers.regexp;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.amelia.dsl.lib.CallableTask;
import org.amelia.dsl.lib.util.Arrays;
import org.amelia.dsl.lib.util.Log;
import org.amelia.dsl.lib.util.ShellUtils;
import org.amelia.dsl.lib.util.Strings;

import com.google.common.collect.Iterables;

import net.sf.expectit.Expect;
import net.sf.expectit.ExpectIOException;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class CommandDescriptor extends Observable {
	
	public static class Builder {

		public String command;
		public String[] arguments;
		public String releaseRegexp;
		public long timeout;
		public String[] errorTexts;
		public String errorMessage;
		public String successMessage;
		public CallableTask<?> callable;
		public boolean execution;

		public Builder() {
			this.command = "";
			this.arguments = new String[0];
			this.releaseRegexp = ShellUtils.ameliaPromptRegexp();
			this.timeout = 0;
			this.errorTexts = new String[0];
			this.errorMessage = "";
			this.successMessage = "";
			this.execution = false;
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
		
		public Builder withErrorText(String... errorTexts) {
			this.errorTexts = errorTexts;
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
		
		public Builder withCallable(CallableTask<?> callable) {
			this.callable = callable;
			return this;
		}
		
		public Builder isExecution() {
			this.execution = true;
			return this;
		}	

		public CommandDescriptor build() {
			if (this.callable == null)
				this.callable = defaultCallableTask();
			return new CommandDescriptor(this);
		}
		
		protected CallableTask<Void> defaultCallableTask() {
			return new CallableTask<Void>() {
				@Override public Void call(Host host, String prompt)
						throws Exception {
					Expect expect = host.ssh().expect();	
					if (timeout == -1)
						expect = expect.withInfiniteTimeout();
					else if (timeout > 0)
						expect = expect.withTimeout(timeout, TimeUnit.MILLISECONDS);

					try {
						// Execute the command and expect for a successful execution
						String _command = command + " " + Arrays.join(arguments, " ");
						expect.sendLine(_command);
						String response = expect.expect(regexp(releaseRegexp)).getBefore();

						// Check non-zero error code
						expect.expect(regexp(prompt));
						expect.sendLine("echo $?");
						String returnCode = expect.expect(regexp("([0-9]+)")).group(1);
						
						if (Strings.containsAnyOf(response, errorTexts)) {
							Log.error(host, errorMessage);
							throw new RuntimeException(errorMessage);
						} else if (!returnCode.equals("0")) {
							throw new RuntimeException(
								String.format(
									"The command returned a non-zero error code (%s)",
									returnCode
								)
							);
						} else {
							Log.success(
								host,
								successMessage == null || successMessage.isEmpty()
									? _command : successMessage
							);
						}
					} catch(ExpectIOException e) {
						String response = e.getInputBuffer();
						if (Strings.containsAnyOf(response, errorTexts)) {
							Log.error(host, errorMessage);
							throw new Exception(errorMessage);
						} else {
							String regexp = releaseRegexp.equals(prompt)
									? "the amelia prompt" : String.format("\"%s\"", releaseRegexp);
							String message = String.format(
								"Operation timeout waiting for %s in host %s",
								regexp,
								host
							);
							throw new RuntimeException(message);
						}
					}
					return null;
				}
			};
		}
	}

	protected final UUID internalId;
	protected final String command;
	protected final String[] errorTexts;
	protected final String errorMessage;
	protected final String releaseRegexp;
	protected final String successMessage;
	protected final long timeout;
	protected CallableTask<?> callable;
	protected final boolean execution;
	private final List<CommandDescriptor> dependencies;
	private final List<Host> hosts;

	public CommandDescriptor(final Builder builder) {
		this.internalId = UUID.randomUUID();
		this.command = builder.command + (builder.arguments.length > 0
				? " " + Arrays.join(builder.arguments, " ") : "");
		this.releaseRegexp = builder.releaseRegexp;
		this.timeout = builder.timeout;
		this.errorTexts = builder.errorTexts;
		this.errorMessage = builder.errorMessage;
		this.successMessage = builder.successMessage;
		this.callable = builder.callable;
		this.execution = builder.execution;
		this.dependencies = new ArrayList<CommandDescriptor>();
		this.hosts = new ArrayList<Host>();
	}
	
	/**
	 * Augments this command by concatenating a command at the end. The commands
	 * are concatenated using the {@code &&} operator.
	 * 
	 * @param command
	 *            The descriptor providing the command to augment {@code this}
	 *            command.
	 * @return A new {@link CommandDescriptor} instance with the same
	 *         configuration as {@code this} command, but augmenting its command
	 *         with {@code command}.
	 */
	public CommandDescriptor augmentWith(CommandDescriptor command) {
		CommandDescriptor.Builder builder = new CommandDescriptor.Builder()
				.withCommand(toCommandString() + " && " + command.toCommandString())
				.withErrorMessage(errorMessage())
				.withErrorText(errorTexts())
				.withReleaseRegexp(releaseRegexp())
				.withSuccessMessage(successMessage())
				.withTimeout(timeout());
		if (isExecution())
			builder.isExecution();
		CommandDescriptor result = builder.build();
		result.dependsOn(dependencies().toArray(new CommandDescriptor[0]));
		result.runsOn(hosts().toArray(new Host[0]));
		return result;
	}

	public boolean isOk(String response) {
		return this.errorTexts == null
				|| Strings.containsAnyOf(response, this.errorTexts);
	}

	public void done(Host host) {
		setChanged();
	}

	public String doneMessage() {
		return successMessage == null || successMessage.isEmpty() ? toString()
				: successMessage;
	}

	public String failMessage() {
		return errorMessage == null || errorMessage.isEmpty() ? toString()
				: errorMessage;
	}

	public String toCommandString() {
		return this.command;
	}
	
	public boolean dependsOn(CommandDescriptor... dependencies) {
		boolean all = true;
		for (CommandDescriptor descriptor : dependencies) {
			if (descriptor.equals(this))
				throw new IllegalArgumentException("A command cannot depend on itself");
			if (this.dependencies.contains(descriptor)) {
				all = false;
				continue;
			}
			this.dependencies.add(descriptor);
		}
		return all;
	}
	
	public boolean dependsOn(Iterable<CommandDescriptor> dependencies) {
		return dependsOn(
				Iterables.toArray(dependencies, CommandDescriptor.class));
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
	
	public boolean runsOn(Iterable<Host> hosts) {
		return runsOn(Iterables.toArray(hosts, Host.class));
	}
	
	public List<Host> hosts() {
		return this.hosts;
	}
	
	public List<CommandDescriptor> dependencies() {
		return this.dependencies;
	}

	@Override
	public String toString() {
		return toCommandString();
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

	public String[] errorTexts() {
		return this.errorTexts;
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
	
	public CallableTask<?> callable() {
		return this.callable;
	}
	
	public boolean isExecution() {
		return this.execution;
	}
}
