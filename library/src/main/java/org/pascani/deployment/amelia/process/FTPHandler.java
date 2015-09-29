package org.pascani.deployment.amelia.process;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

	/**
	 * The logger
	 */
	private final static Logger logger = LogManager.getLogger(FTPHandler.class);

	public FTPHandler(final Host host) {
		this.host = host;
		this.client = new FTPClient();
	}

	public void run() {
		try {

			connect();

		} catch (SocketException e) {
			logger.error("Error opening FTP connection for " + this.host, e);
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("Error opening FTP connection for " + this.host, e);
			e.printStackTrace();
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
		return this.client.logout();
	}

	public boolean upload(AssetBundle bundle) throws IOException {
		boolean uploaded = true;

		for (Map.Entry<String, List<String>> pair : bundle.transfers()
				.entrySet()) {
			for (String remote : pair.getValue())
				uploaded &= this.client.upload(pair.getKey(), remote);
		}

		return uploaded;
	}
	
	public Host host() {
		return this.host;
	}

}
