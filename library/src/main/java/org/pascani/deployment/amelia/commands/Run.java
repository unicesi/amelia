/*
 * Copyright © 2015 Universidad Icesi
 * 
 * This file is part of the Amelia library.
 * 
 * The Amelia library is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Amelia library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the Amelia library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.pascani.deployment.amelia.commands;

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

		// Send the run command
		expect.sendLine(descriptor.toCommandString() + " &");
		String _pid = expect.expect(regexp("\\[\\d+\\] (\\d+)")).group(1);

		// Detach the process
		expect.sendLine(ShellUtils.detachProcess());

		PID = Integer.parseInt(_pid);

		try {
			// Expect for a successful execution
			expect.expect(regexp("Press Ctrl\\+C to quit\\.\\.\\.|Call done!"));

		} catch (ExpectIOException ex) {
			String message = "Cannot instantiate the FraSCAti factory!";
			if (ex.getInputBuffer().contains(message)) {
				Log.info(super.host.toFixedString() + " " + ascii(10007) + " "
						+ message);
				throw new DeploymentException(message);
			} else {
				throw new RuntimeException("Expect operation timeout: "
						+ ex.getInputBuffer());
			}
		}

		return PID;
	}

}
