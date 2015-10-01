package org.pascani.deployment.amelia.util;

import static net.sf.expectit.matcher.Matchers.regexp;

import java.io.IOException;

import net.sf.expectit.Expect;
import net.sf.expectit.ExpectIOException;

import org.pascani.deployment.amelia.descriptors.FrascatiCompilation;
import org.pascani.deployment.amelia.descriptors.FrascatiExecution;

public class FraSCAti {

	public void compile(Expect e, FrascatiCompilation descriptor)
			throws DeploymentException, IOException {

		// The Amelia prompt
		String prompt = ShellUtils.ameliaPromptRegexp();

		// Perform the compilation
		e.sendLine(descriptor.toCommandString());
		String compile = e.expect(regexp(prompt)).getBefore();

		if (compile.contains("No such file or directory"))
			throw new DeploymentException("No such file or directory \""
					+ descriptor.sourceDirectory() + "\"");
	}

	public int run(Expect e, FrascatiExecution descriptor)
			throws DeploymentException, IOException {

		int PID = -1;

		// The Amelia prompt
		String prompt = ShellUtils.ameliaPromptRegexp();

		// Send the run command
		e.sendLine(descriptor.toCommandString() + " &");
		String _pid = e.expect(regexp("\\[\\d+\\] (\\d+)")).group(1);

		// Update the process ID
		PID = Integer.parseInt(_pid);

		try {
			// Expect a successful execution
			e.expect(regexp("Press Ctrl\\+C to quit\\.\\.\\.|Call done!"));
			e.sendLine();
			e.expect(regexp(prompt));

		} catch (ExpectIOException ex) {

			// First, kill the remaining processes (if any)
			String criterion = descriptor.toCommandSearchString();
			e.sendLine(ShellUtils.killCommand(criterion));

			throw new DeploymentException(
					"Cannot instantiate the FraSCAti factory!");
		}

		return PID;
	}

}
