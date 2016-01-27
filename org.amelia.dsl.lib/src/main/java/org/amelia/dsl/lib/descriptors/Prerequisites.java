package org.amelia.dsl.lib.descriptors;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Prerequisites extends CommandDescriptor {

	private final Version javaVersion;

	private final Version frascatiVersion;

	public Prerequisites(final Version javaVersion,
			final Version frascatiVersion) {
		super(new CommandDescriptor.Builder()
				.withErrorMessage("Dissatisfied prerequisites")
				.withSuccessMessage("Prerequisites satisfied"));
		this.javaVersion = javaVersion;
		this.frascatiVersion = frascatiVersion;
	}

	public Version javaVersion() {
		return this.javaVersion;
	}

	public Version frascatiVersion() {
		return this.frascatiVersion;
	}

}
