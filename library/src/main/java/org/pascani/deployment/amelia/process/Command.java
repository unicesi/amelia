package org.pascani.deployment.amelia.process;

import static net.sf.expectit.matcher.Matchers.regexp;

import java.util.concurrent.Callable;

import org.pascani.deployment.amelia.DeploymentException;
import org.pascani.deployment.amelia.descriptors.CommandDescriptor;
import org.pascani.deployment.amelia.util.ShellUtils;

import net.sf.expectit.Expect;

public class Command implements Callable<Boolean> {

	private final Expect expect;
	
	private final CommandDescriptor descriptor;
	
	public Command(final Expect expect, final CommandDescriptor descriptor) {
		this.expect = expect;
		this.descriptor = descriptor;
	}

	public Boolean call() throws Exception {
		
		String prompt = ShellUtils.ameliaPromptRegexp();

		this.expect.sendLine(this.descriptor.toCommandString());
		String cd = this.expect.expect(regexp(prompt)).getBefore();

		if (cd.contains(this.descriptor.errorText()))
			throw new DeploymentException(this.descriptor.errorMessage());
		
		return true;
	}

}
