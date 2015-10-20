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

import org.pascani.deployment.amelia.descriptors.AssetBundle;
import org.pascani.deployment.amelia.descriptors.Host;

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

}
