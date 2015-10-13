package org.pascani.deployment.amelia.process;

import static net.sf.expectit.matcher.Matchers.regexp;

import java.util.concurrent.Callable;

import org.pascani.deployment.amelia.DeploymentException;
import org.pascani.deployment.amelia.descriptors.CommandDescriptor;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.util.ShellUtils;

import net.sf.expectit.Expect;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public abstract class Command<T> implements Callable<T> {
	
	public static class Simple extends Command<Boolean> {

		public Simple(Host host, CommandDescriptor descriptor) {
			super(host, descriptor);
		}
		
		public Boolean call() throws Exception {
			
			Expect expect = this.host.ssh().expect();
			String prompt = ShellUtils.ameliaPromptRegexp();

			expect.sendLine(this.descriptor.toCommandString());
			String response = expect.expect(regexp(prompt)).getBefore();

			if (!this.descriptor.isOk(response)) {
				this.descriptor.fail(this.host);
				throw new DeploymentException(this.descriptor.errorMessage());
			}

			return true;
		}
	}
	
	protected final Host host;

	protected final CommandDescriptor descriptor;

	public Command(final Host host, final CommandDescriptor descriptor) {
		this.host = host;
		this.descriptor = descriptor;
	}

	public abstract T call() throws Exception;
	
	public Host host() {
		return this.host;
	}
	
	public CommandDescriptor descriptor() {
		return this.descriptor;
	}

}
