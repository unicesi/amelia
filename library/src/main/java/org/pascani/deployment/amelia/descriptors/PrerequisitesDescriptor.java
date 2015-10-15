package org.pascani.deployment.amelia.descriptors;

public class PrerequisitesDescriptor extends CommandDescriptor {
	
	private final String javaVersion;
	
	private final String frascatiVersion;
	
	public PrerequisitesDescriptor() {
		this("1.6.0_23", "1.4");
	}
	
	public PrerequisitesDescriptor(final String javaVersion, final String frascatiVersion) {
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
