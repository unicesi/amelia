package org.pascani.deployment.amelia.process;

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
