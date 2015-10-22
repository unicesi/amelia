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
import net.sf.expectit.Expect;
import net.sf.expectit.Result;

import org.pascani.deployment.amelia.DeploymentException;
import org.pascani.deployment.amelia.descriptors.ChangeDirectory;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.util.Log;
import org.pascani.deployment.amelia.util.ShellUtils;
import org.pascani.deployment.amelia.util.Strings;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Cd extends Command<Boolean> {

	private String previousDirectory;

	public Cd(Host host, ChangeDirectory descriptor) {
		super(host, descriptor);
	}

	@Override
	public Boolean call() throws Exception {
		goTo(this.descriptor.toCommandString(), false);
		return true;
	}

	private void goTo(String cdCommand, boolean rollback) throws Exception {
		ChangeDirectory descriptor = (ChangeDirectory) super.descriptor;
		Host host = super.host;

		Expect expect = host.ssh().expect();
		String prompt = ShellUtils.ameliaPromptRegexp();

		if (!rollback) {
			this.previousDirectory = pwd(expect, prompt);
		}

		expect.sendLine(cdCommand);
		String response = expect.expect(regexp(prompt)).getBefore();

		String[] _404 = { "No existe el fichero o el directorio",
				"No such file or directory" };
		String[] _denied = { "Permission denied", "Permiso denegado" };

		if (Strings.containsAnyOf(response, _404)) {
			String message = "No such file or directory '"
					+ descriptor.directory() + "'";

			Log.error(host, message);
			throw new DeploymentException(message);
		} else if (Strings.containsAnyOf(response, _denied)) {
			String message = "Permission denied to access '"
					+ descriptor.directory() + "'";

			Log.error(host, message);
			throw new DeploymentException(message);
		}
	}

	private String pwd(Expect expect, String prompt) throws Exception {
		expect.sendLine("pwd");
		Result pwd = expect.expect(regexp(prompt));

		return pwd.getBefore().trim();
	}

	@Override
	public void rollback() throws Exception {
		goTo(this.previousDirectory, true);

		Expect expect = host.ssh().expect();
		String prompt = ShellUtils.ameliaPromptRegexp();

		String workingDirectory = pwd(expect, prompt);

		if (workingDirectory.equals(this.previousDirectory))
			Log.info(super.host, "Working directory: " + this.previousDirectory);
		else
			Log.warning(super.host, "Could not change working directory to "
					+ this.previousDirectory);
	}

}
