package org.pascani.deployment.amelia.process;

import static net.sf.expectit.matcher.Matchers.regexp;
import static org.pascani.deployment.amelia.util.Strings.ascii;

import java.util.concurrent.Callable;

import net.sf.expectit.Expect;
import net.sf.expectit.ExpectIOException;

import org.pascani.deployment.amelia.DeploymentException;
import org.pascani.deployment.amelia.descriptors.ExecutionDescriptor;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.util.Log;
import org.pascani.deployment.amelia.util.ShellUtils;

public class Run extends Command<Integer> implements Callable<Integer> {

	public Run(final Host host, final ExecutionDescriptor descriptor) {
		super(host, descriptor);
	}

	public Integer call() throws Exception {

		int PID = -1;
		ExecutionDescriptor descriptor = (ExecutionDescriptor) super.descriptor;
		Expect expect = this.host.ssh().expect();

		// The Amelia prompt
		String prompt = ShellUtils.ameliaPromptRegexp();

		// Send the run command
		expect.sendLine(descriptor.toCommandString() + " &");
		String _pid = expect.expect(regexp("\\[\\d+\\] (\\d+)")).group(1);

		// Detach the process
		expect.sendLine(ShellUtils.detachProcess());

		PID = Integer.parseInt(_pid);
		try {
			// Expect for a successful execution
			expect.expect(regexp("Press Ctrl\\+C to quit\\.\\.\\.|Call done!"));
			expect.sendLine();
			expect.expect(regexp(prompt));

		} catch (ExpectIOException ex) {
			String message = "Cannot instantiate the FraSCAti factory!";

			Log.info(super.host.toFixedString() + " " + ascii(10007) + " "
					+ message);
			throw new DeploymentException(message);
		}

		return PID;
	}

}
