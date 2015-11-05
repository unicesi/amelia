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
import org.pascani.deployment.amelia.descriptors.Prerequisites;
import org.pascani.deployment.amelia.util.Log;
import org.pascani.deployment.amelia.util.ShellUtils;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class PrerequisiteCheck extends Command<Boolean> {

	public PrerequisiteCheck(Host host, Prerequisites descriptor) {
		super(host, descriptor);
	}

	@Override
	public Boolean call() throws Exception {

		boolean ok = true;
		Host host = super.host;
		String prompt = ShellUtils.ameliaPromptRegexp();

		Prerequisites descriptor = (Prerequisites) super.descriptor;
		Expect expect = host.ssh().expect();

		// Check environment variables
		verifyFrascatiVariable(expect, prompt);

		// Check Java and FraSCAti are installed
		verifyFrascati(expect, prompt);

		// Check programs' versions
		verifyFrascatiVersion(expect, prompt, descriptor);
		verifyJavaVersion(expect, prompt, descriptor);

		Log.ok(host, descriptor.doneMessage());

		return ok;
	}

	private void verifyFrascatiVariable(final Expect expect, final String prompt)
			throws Exception {

		expect.sendLine("echo $FRASCATI_HOME");
		Result frascatiHome = expect.expect(Matchers.regexp(prompt));

		if (frascatiHome.getBefore().trim().isEmpty()) {
			String message = "Environment variable FRASCATI_HOME not found";
			Log.error(super.host, message);
		}
	}

	private void verifyFrascati(final Expect expect, final String prompt)
			throws Exception {

		expect.sendLine("frascati --help");
		Result frascati = expect.expect(Matchers.regexp(prompt));

		if (!frascati.getBefore().contains("Usage: frascati")) {
			String message = "FraSCAti not found";
			Log.error(super.host, message);
			throw new RuntimeException(message + " in host " + host);
		}
	}

	private void verifyFrascatiVersion(final Expect expect,
			final String prompt, final Prerequisites descriptor)
			throws Exception {

		expect.sendLine("frascati --version");
		Result frascatiVersion = expect.expect(Matchers.regexp(prompt));

		Pattern fpattern = Pattern
				.compile("OW2 FraSCAti version (([0-9]|\\.)*)");
		Matcher fmatcher = fpattern.matcher(frascatiVersion.getBefore());

		if (fmatcher.find()) {
			if (!descriptor.frascatiVersion().isCompliant(fmatcher.group(1))) {
				String message = "the FraSCAti version (" + fmatcher.group(1)
						+ ") is not compliant with "
						+ descriptor.frascatiVersion();

				Log.error(super.host, message);
				throw new RuntimeException(message + " in host " + host);
			}
		} else {
			Log.warning(super.host,
					"The FraSCAti version could not be verified. Unknown version "
							+ frascatiVersion.getBefore());
		}
	}

	private void verifyJavaVersion(final Expect expect, final String prompt,
			final Prerequisites descriptor) throws Exception {

		expect.sendLine("java -version");
		Result javaVersion = expect.expect(Matchers.regexp(prompt));

		Pattern jpattern = Pattern.compile("java version \"(.*)\"");
		Matcher jmatcher = jpattern.matcher(javaVersion.getBefore());

		if (jmatcher.find()) {
			if (!descriptor.javaVersion().isCompliant(jmatcher.group(1))) {
				String message = "the java version (" + jmatcher.group(1)
						+ ") is not compliant with " + descriptor.javaVersion();

				Log.error(super.host, message);
				throw new RuntimeException(message + " in host " + host);
			}
		} else {
			Log.warning(super.host,
					"The java version could not be verified. Unknown version "
							+ javaVersion.getBefore());
		}
	}

}
