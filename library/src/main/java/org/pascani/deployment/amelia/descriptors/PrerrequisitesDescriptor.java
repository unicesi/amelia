package org.pascani.deployment.amelia.descriptors;

public class PrerrequisitesDescriptor extends CommandDescriptor {
	
	private final String javaVersion;
	
	private final String frascatiVersion;
	
	public PrerrequisitesDescriptor() {
		this("1.6.0_23", "1.4");
	}
	
	public PrerrequisitesDescriptor(final String javaVersion, final String frascatiVersion) {
		super("", "", "Dissatisfied prerequisites", "Prerequisites satisfied");
		this.javaVersion = javaVersion;
		this.frascatiVersion = frascatiVersion;
	}
	
	public String javaVersion() {
		return this.javaVersion;
	}
	
	public String frascatiVersion() {
		return this.frascatiVersion;
	}

}
