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

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.sf.expectit.Expect;
import net.sf.expectit.ExpectIOException;

import org.pascani.deployment.amelia.descriptors.Execution;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.util.Log;
import org.pascani.deployment.amelia.util.ShellUtils;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Run extends Command<Integer> implements Callable<Integer> {

	public Run(final Host host, final Execution descriptor) {
		super(host, descriptor);
	}

	@SuppressWarnings("resource")
	public Integer call() throws Exception {

		int PID = -1;
		Host host = super.host;
		Execution descriptor = (Execution) super.descriptor;
		Expect expect = host.ssh().expect();

		// Send the run command
		expect.sendLine(descriptor.toCommandString() + " &");
		String _pid = expect.expect(regexp("\\[\\d+\\] (\\d+)")).group(1);

		// Detach the process
		expect.sendLine(ShellUtils.detachProcess());

		PID = Integer.parseInt(_pid);

		try {
			// Expect for a successful execution
			if (descriptor.timeout() == -1)
				expect = expect.withInfiniteTimeout();
			else if (descriptor.timeout() > 0)
				expect = expect.withTimeout(descriptor.timeout(),
						TimeUnit.MILLISECONDS);

			expect.expect(regexp(descriptor.stopRegexp()));
			Log.info(host, descriptor.doneMessage());

		} catch (ExpectIOException ex) {
			String message1 = "Cannot instantiate the FraSCAti factory";
			String message2 = "Operation timeout waiting for \""
					+ descriptor.stopRegexp() + "\" in host " + host;

			if (ex.getInputBuffer().contains(message1)) {
				Log.error(host, message1 + " in host " + host);
				throw ex;
			} else {
				throw new RuntimeException(message2);
			}
		}

		return PID;
	}

}
