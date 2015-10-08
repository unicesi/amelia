package org.pascani.deployment.amelia.process;

import static net.sf.expectit.matcher.Matchers.regexp;

import java.util.concurrent.Callable;

import net.sf.expectit.Expect;
import net.sf.expectit.ExpectIOException;

import org.pascani.deployment.amelia.descriptors.ExecutionDescriptor;
import org.pascani.deployment.amelia.util.DeploymentException;
import org.pascani.deployment.amelia.util.ShellUtils;

public class Execution implements Callable<Integer>, Runnable {
	
	private final Expect expect;

	private final ExecutionDescriptor descriptor;

	public Execution(final Expect expect, final ExecutionDescriptor descriptor) {
		this.expect = expect;
		this.descriptor = descriptor;
	}
	
	public void run() {
		try {
			call();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Integer call() throws Exception {
		int PID = -1;

		// The Amelia prompt
		String prompt = ShellUtils.ameliaPromptRegexp();

		// Send the run command
		this.expect.sendLine(descriptor.toCommandString() + " &");
		String _pid = this.expect.expect(regexp("\\[\\d+\\] (\\d+)")).group(1);

		// Update the process ID
		PID = Integer.parseInt(_pid);

		try {
			// Expect a successful execution
			this.expect.expect(regexp("Press Ctrl\\+C to quit\\.\\.\\.|Call done!"));
			this.expect.sendLine();
			this.expect.expect(regexp(prompt));

		} catch (ExpectIOException ex) {

			// First, kill the remaining processes (if any)
			String criterion = descriptor.toCommandSearchString();
			this.expect.sendLine(ShellUtils.killCommand(criterion));

			throw new DeploymentException(
					"Cannot instantiate the FraSCAti factory!");
		}

		return PID;
	}

}
