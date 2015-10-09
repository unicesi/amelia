package org.pascani.deployment.amelia.process;

import static net.sf.expectit.matcher.Matchers.regexp;

import java.util.concurrent.Callable;

import org.pascani.deployment.amelia.DeploymentException;
import org.pascani.deployment.amelia.descriptors.CommandDescriptor;
import org.pascani.deployment.amelia.util.DependencyGraph;
import org.pascani.deployment.amelia.util.ShellUtils;

import net.sf.expectit.Expect;

/**
 * 
 * New child classes need to add a case in:
 * {@link DependencyGraph#addElement(java.util.Observable, SSHHandler)}
 * 
 * @see DependencyGraph#addElement(java.util.Observable, SSHHandler)
 * 
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Command implements Callable<Boolean> {

	private final SSHHandler handler;

	private final CommandDescriptor descriptor;

	public Command(final SSHHandler handler, final CommandDescriptor descriptor) {
		this.handler = handler;
		this.descriptor = descriptor;
	}

	public Boolean call() throws Exception {

		Expect expect = this.handler.expect();
		String prompt = ShellUtils.ameliaPromptRegexp();

		expect.sendLine(this.descriptor.toCommandString());
		String response = expect.expect(regexp(prompt)).getBefore();

		if (!this.descriptor.isOk(response)) {
			this.descriptor.fail(this.handler.host());
			throw new DeploymentException(this.descriptor.errorMessage());
		}

		return true;
	}

}
