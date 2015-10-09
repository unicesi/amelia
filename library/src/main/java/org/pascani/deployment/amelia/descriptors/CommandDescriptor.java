package org.pascani.deployment.amelia.descriptors;

import static org.pascani.deployment.amelia.util.Strings.ascii;

import java.util.Observable;

public class CommandDescriptor extends Observable {

	private final String command;

	private final String errorText;

	private final String errorMessage;

	private final String successMessage;

	public CommandDescriptor(final String command, final String errorText,
			final String errorMessage, final String successMessage) {
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
		System.out.println(host + ": " + doneMessage());
	}

	public void fail(Host host) {
		System.out.println(host + ": " + failMessage());
	}

	public String doneMessage() {
		String message = ascii(128522) + "  "; // check mark: \u2713
		message += successMessage == null ? toString() : successMessage;

		return message;
	}

	public String failMessage() {
		String message = ascii(128557) + "  "; // X mark: \u2717
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

	public String errorText() {
		return this.errorText;
	}

	public String errorMessage() {
		return this.errorMessage;
	}
}
