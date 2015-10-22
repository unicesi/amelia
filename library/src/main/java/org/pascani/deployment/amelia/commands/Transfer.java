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

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.pascani.deployment.amelia.descriptors.AssetBundle;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.filesystem.FTPClient;
import org.pascani.deployment.amelia.util.Log;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Transfer extends Command<Void> {

	public Transfer(final Host host, final AssetBundle bundle) {
		super(host, bundle);
	}

	@Override
	public Void call() throws Exception {

		AssetBundle descriptor = (AssetBundle) super.descriptor;

		try {
			super.host.ftp().upload(descriptor);
		} catch (Exception e) {
			descriptor.fail(super.host);
			throw e;
		}

		return null;
	}

	@Override
	public void rollback() throws Exception {
		Host host = super.host;
		AssetBundle descriptor = (AssetBundle) super.descriptor;
		FTPClient client = host.ftp().client();

		for (Entry<String, List<String>> entry : descriptor.transfers()
				.entrySet()) {
			for (String remote : entry.getValue()) {
				try {
					client.removeDirectoryWithContents(remote);
				} catch (IOException e) {
					Log.warning(host, "Could not remove remote file " + remote);
				}
			}
		}
	}

}
