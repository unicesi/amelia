package org.pascani.deployment.amelia.process;

import static net.sf.expectit.matcher.Matchers.regexp;

import java.util.concurrent.Callable;

import net.sf.expectit.Expect;
import net.sf.expectit.ExpectIOException;

import org.pascani.deployment.amelia.DeploymentException;
import org.pascani.deployment.amelia.descriptors.ExecutionDescriptor;
import org.pascani.deployment.amelia.util.ShellUtils;

public class Run implements Callable<Integer> {
	
	private final SSHHandler handler;

	private final ExecutionDescriptor descriptor;

	public Run(final SSHHandler handler, final ExecutionDescriptor descriptor) {
		this.handler = handler;
		this.descriptor = descriptor;
	}

	public Integer call() throws Exception {
		int PID = -1;
		Expect expect = this.handler.expect();

		// The Amelia prompt
		String prompt = ShellUtils.ameliaPromptRegexp();

		// Send the run command
		expect.sendLine(descriptor.toCommandString() + " &");
		String _pid = expect.expect(regexp("\\[\\d+\\] (\\d+)")).group(1);

		// Update the process ID
		PID = Integer.parseInt(_pid);

		try {
			// Expect a successful execution
			expect.expect(regexp("Press Ctrl\\+C to quit\\.\\.\\.|Call done!"));
			expect.sendLine();
			expect.expect(regexp(prompt));

		} catch (ExpectIOException ex) {

			// First, kill the remaining processes (if any)
			String criterion = descriptor.toCommandSearchString();
			expect.sendLine(ShellUtils.killCommand(criterion));

			throw new DeploymentException(
					"Cannot instantiate the FraSCAti factory!");
		}

		return PID;
	}
	
	public ExecutionDescriptor descriptor() {
		return this.descriptor;
	}

}
