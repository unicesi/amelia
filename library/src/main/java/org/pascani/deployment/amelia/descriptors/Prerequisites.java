package org.pascani.deployment.amelia.descriptors;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Prerequisites extends CommandDescriptor {

	private final Version javaVersion;

	private final Version frascatiVersion;

	public Prerequisites(final Version javaVersion,
			final Version frascatiVersion) {
		super("Java & FraSCAti prerequisites", "",
				"Dissatisfied prerequisites", "Prerequisites satisfied");
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
