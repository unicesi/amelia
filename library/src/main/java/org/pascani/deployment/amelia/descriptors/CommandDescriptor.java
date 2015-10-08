package org.pascani.deployment.amelia.descriptors;

import java.util.Observable;

public abstract class CommandDescriptor extends Observable {
	
	public abstract String toCommandString();

}
