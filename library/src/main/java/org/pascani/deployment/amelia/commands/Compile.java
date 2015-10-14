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

import org.pascani.deployment.amelia.DeploymentException;
import org.pascani.deployment.amelia.descriptors.CompilationDescriptor;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.util.ShellUtils;

import net.sf.expectit.Expect;

public class Compile extends Command<Boolean> {

	public Compile(final Host host, final CompilationDescriptor descriptor) {
		super(host, descriptor);
	}

	public Boolean call() throws Exception {
		
		CompilationDescriptor descriptor = (CompilationDescriptor) super.descriptor;
		Expect expect = this.host.ssh().expect();

		// The Amelia prompt
		String prompt = ShellUtils.ameliaPromptRegexp();

		// Perform the compilation
		expect.sendLine(descriptor.toCommandString());
		String compile = expect.expect(regexp(prompt)).getBefore();

		if (compile.contains("No such file or directory"))
			throw new DeploymentException("No such file or directory \""
					+ descriptor.sourceDirectory() + "\"");

		return true;
	}

}
