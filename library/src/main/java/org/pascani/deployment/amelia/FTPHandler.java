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

/**
 * TODO
 * 
 * @author Miguel Jiménez - Initial contribution and API
 */
public class FTPHandler extends Thread {

	private final Host host;

	private final FTPClient client;

	public FTPHandler(final Host host) {
		this.host = host;
		this.client = new FTPClient();
		
		// Handle uncaught exceptions
		this.setUncaughtExceptionHandler(Amelia.exceptionHandler);
	}

	public void run() {
		try {
			connect();
		} catch (SocketException e) {
			String message = "Error establishing FTP connection for " + this.host;
			throw new RuntimeException(message, e);
		} catch (IOException e) {
			String message = "Error establishing FTP connection for " + this.host;
			throw new RuntimeException(message, e);
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

	public boolean upload(AssetBundle bundle) throws IOException {
		boolean uploaded = true;

		for (Map.Entry<String, List<String>> pair : bundle.transfers()
				.entrySet()) {
			for (String remote : pair.getValue()) {
				uploaded &= this.client.upload(pair.getKey(), remote);
			}
		}

		return uploaded;
	}
	
	public Host host() {
		return this.host;
	}

}