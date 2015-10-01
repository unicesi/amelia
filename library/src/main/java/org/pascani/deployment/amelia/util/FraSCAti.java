package org.pascani.deployment.amelia.util;

import static net.sf.expectit.matcher.Matchers.regexp;

import java.io.IOException;

import net.sf.expectit.Expect;
import net.sf.expectit.ExpectIOException;

import org.pascani.deployment.amelia.descriptors.FrascatiCompilation;
import org.pascani.deployment.amelia.descriptors.FrascatiExecution;

public class FraSCAti {

	private final String workingDirectory;

	public FraSCAti() {
		this("/");
	}

	public FraSCAti(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	/**
	 * The working directory of the current instance will remain the same. A new
	 * instance is returned with the working directory set to the specified one.
	 * 
	 * @param directoryPath
	 * @return
	 */
	public FraSCAti withWorkingDirectory(String directoryPath) {
		return new FraSCAti(directoryPath);
	}

	public void compile(Expect e, FrascatiCompilation descriptor)
			throws DeploymentException, IOException {

		// The Amelia prompt
		String prompt = ShellUtils.ameliaPromptRegexp();

		// Change current directory
		e.sendLine("cd " + workingDirectory);
		String cd = e.expect(regexp(prompt)).getBefore().toLowerCase();

		if (cd.contains("no such file or directory"))
			throw new DeploymentException("No such file or directory \""
					+ workingDirectory + "\"");

		// Perform the compilation
		e.sendLine(descriptor.toCommandString());
		String compile = e.expect(regexp(prompt)).getBefore().toLowerCase();

		if (compile.contains("no such file or directory"))
			throw new DeploymentException("No such file or directory \""
					+ descriptor.sourceDirectory() + "\"");
	}

	public void run(Expect e, FrascatiExecution descriptor)
			throws DeploymentException, IOException {

		// The Amelia prompt
		String prompt = ShellUtils.ameliaPromptRegexp();

		String errorMessage = "Cannot instantiate the FraSCAti factory!";
		String[] errors = { "Cannot instantiate the FraSCAti factory!",
				"OW2 FraSCAti Standalone Runtime\nException in thread" };

		// Change current directory
		e.sendLine("cd " + workingDirectory);
		String cd = e.expect(regexp(prompt)).getBefore().toLowerCase();

		if (cd.contains("no such file or directory"))
			throw new DeploymentException("No such file or directory \""
					+ workingDirectory + "\"");

		// Perform the execution
		e.sendLine(descriptor.toCommandString());
		boolean ok = true;

		try {
			
			String run = e.expect(regexp(prompt)).getBefore();
			
			if (Strings.containsAnyOf(run, errors))
				ok = false;
			
		} catch (ExpectIOException ex) {
			// TODO: System.out.println(Arrays.toString(ex.getInputBuffer().split("\n")));
			if (Strings.containsAnyOf(ex.getInputBuffer(), errors))
				ok = false;
		} finally {
			if(!ok)
				throw new DeploymentException(errorMessage);
		}
	}

}
