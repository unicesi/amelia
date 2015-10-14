package org.pascani.deployment.amelia.commands;

import org.pascani.deployment.amelia.descriptors.AssetBundle;
import org.pascani.deployment.amelia.descriptors.Host;

public class Transfer extends Command<Boolean> {

	public Transfer(final Host host, final AssetBundle bundle) {
		super(host, bundle);
	}

	@Override
	public Boolean call() throws Exception {
		return super.host.ftp().upload((AssetBundle) super.descriptor);
	}

}
