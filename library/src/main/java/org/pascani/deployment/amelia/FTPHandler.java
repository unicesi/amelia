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
package org.pascani.deployment.amelia;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.Map;

import org.pascani.deployment.amelia.descriptors.AssetBundle;
import org.pascani.deployment.amelia.descriptors.Host;
import org.pascani.deployment.amelia.filesystem.FTPClient;
import org.pascani.deployment.amelia.util.Log;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class FTPHandler extends Thread {

	private final Host host;

	private final FTPClient client;

	public FTPHandler(final Host host) {
		this.host = host;
		this.client = new FTPClient();
	}

	public void run() {
		try {
			connect();
		} catch (SocketException e) {
			Log.error(this.host, "Error establishing connection");
			System.exit(0);
		} catch (IOException e) {
			Log.error(this.host, "Error establishing connection");
			System.exit(0);
		}
	}

	private void connect() throws SocketException, IOException {
		this.client.connect(this.host.hostname(), this.host.ftpPort());
		this.client.login(this.host.username(), this.host.password());
	}

	public FTPClient client() {
		return this.client;
	}

	public boolean close() throws IOException {
		if(this.client != null && this.client.isConnected())
			return this.client.logout();
		else
			return false;
	}

	public synchronized void upload(AssetBundle bundle) throws IOException {
		for (Map.Entry<String, List<String>> pair : bundle.transfers()
				.entrySet()) {
			for (String remote : pair.getValue()) {
				this.client.upload(pair.getKey(), remote, bundle.overwrite());
			}
		}
	}
	
	public boolean isConnected() {
		if(this.client == null)
			return false;
		return this.client.isConnected();
	}
	
	public Host host() {
		return this.host;
	}

}
