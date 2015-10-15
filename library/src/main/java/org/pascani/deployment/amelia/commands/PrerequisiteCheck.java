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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.expectit.Expect;
import net.sf.expectit.Result;
import net.sf.expectit.matcher.Matchers;

import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.descriptors.PrerequisitesDescriptor;
import org.pascani.deployment.amelia.util.ShellUtils;

public class PrerequisiteCheck extends Command<Boolean> {

	public PrerequisiteCheck(Host host, PrerequisitesDescriptor descriptor) {
		super(host, descriptor);
	}

	@Override
	public Boolean call() throws Exception {

		boolean ok = true;

		String prompt = ShellUtils.ameliaPromptRegexp();

		PrerequisitesDescriptor descriptor = (PrerequisitesDescriptor) super.descriptor;
		Expect expect = super.host.ssh().expect();

		// Check Java and FraSCAti are installed
		expect.sendLine("frascati --help");
		Result frascati = expect.expect(Matchers.regexp(prompt));

		if (!frascati.getBefore().contains("Usage: frascati"))
			throw new RuntimeException(super.host
					+ " does not have installed the FraSCAti middleware");

		// Check the FraSCAti version
		expect.sendLine("frascati --version");
		Result frascatiVersion = expect.expect(Matchers.regexp(prompt));

		Pattern fpattern = Pattern.compile("OW2 FraSCAti version (([0-9]|\\.)*)");
		Matcher fmatcher = fpattern.matcher(frascatiVersion.getBefore());

		if (fmatcher.find()
				&& !fmatcher.group(1).equals(descriptor.frascatiVersion()))
			throw new RuntimeException("FraSCAti version in host " + super.host
					+ " is " + fmatcher.group(1) + " instead of "
					+ descriptor.frascatiVersion());

		// Check the Java version
		expect.sendLine("java -version");
		Result javaVersion = expect.expect(Matchers.regexp(prompt));

		Pattern jpattern = Pattern.compile("java version \"(.*)\"");
		Matcher jmatcher = jpattern.matcher(javaVersion.getBefore());

		if (jmatcher.find()
				&& !jmatcher.group(1).equals(descriptor.javaVersion()))
			throw new RuntimeException("Java version in host " + super.host
					+ " is " + jmatcher.group(1) + " instead of "
					+ descriptor.javaVersion());

		return ok;
	}

}
