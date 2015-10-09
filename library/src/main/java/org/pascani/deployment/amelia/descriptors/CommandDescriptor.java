package org.pascani.deployment.amelia.descriptors;

import java.util.Observable;

public class CommandDescriptor extends Observable {

	private final String command;

	private final String errorText;

	private final String errorMessage;

	public CommandDescriptor(final String command, final String errorText,
			final String errorMessage) {
		this.command = command;
		this.errorText = errorText;
		this.errorMessage = errorMessage;
	}
	
	public CommandDescriptor(final String command) {
		this(command, null, null);
	}
	
	public boolean isOk(String response) {
		return this.errorText == null || !response.contains(this.errorText);
	}
	
	public void done() {
		setChanged();
	}

	public String toCommandString() {
		return this.command;
	}

	@Override
	public String toString() {
		return "[" + this.command + "]";
	}

	public String errorText() {
		return this.errorText;
	}

	public String errorMessage() {
		return this.errorMessage;
	}
}
