package org.pascani.deployment.amelia.descriptors;

import static org.pascani.deployment.amelia.util.Strings.ascii;

import java.util.Observable;

import org.pascani.deployment.amelia.util.DependencyGraph;
import org.pascani.deployment.amelia.util.Log;

/**
 * @see DependencyGraph#addElement(CommandDescriptor, Host...)
 * @author Miguel Jiménez - Initial contribution and API
 */
public class CommandDescriptor extends Observable {

	protected final String command;

	protected final String errorText;

	protected final String errorMessage;

	protected final String successMessage;

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
		Log.info(host.toFixedString() + " " + doneMessage());
	}

	public void fail(Host host) {
		Log.error(host.toFixedString() + " " + failMessage());
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
		result = prime * result + ((command == null) ? 0 : command.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CommandDescriptor other = (CommandDescriptor) obj;
		if (command == null) {
			if (other.command != null)
				return false;
		} else if (!command.equals(other.command))
			return false;
		return true;
	}

	public String errorText() {
		return this.errorText;
	}

	public String errorMessage() {
		return this.errorMessage;
	}
}
