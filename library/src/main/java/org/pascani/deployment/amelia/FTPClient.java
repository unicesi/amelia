package org.pascani.deployment.amelia;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.net.ftp.FTP;

public class FTPClient extends org.apache.commons.net.ftp.FTPClient {

	public boolean upload(String localPath, String remotePath)
			throws IOException {
		File file = new File(localPath);
		boolean stored = false;

		if (file.isDirectory())
			stored = uploadDirectory(localPath, remotePath);
		else
			stored = uploadFile(localPath, remotePath);

		return stored;
	}

	private boolean uploadFile(String localPath, String remotePath)
			throws IOException {

		setFileType(FTP.BINARY_FILE_TYPE);

		File localFile = new File(localPath);
		InputStream stream = new FileInputStream(localFile);
		boolean stored = storeFile(remotePath, stream);

		stream.close();
		return stored;
	}

	private boolean uploadDirectory(String localPath, String remotePath)
			throws IOException {
		boolean success = true;

		File localDirectory = new File(localPath);
		File[] subFiles = localDirectory.listFiles();
		
		remotePath = remotePath.isEmpty() ? "" : remotePath + "/";

		if (subFiles != null && subFiles.length > 0) {
			for (File file : subFiles) {
				
				String localFilePath = file.getAbsolutePath();
				String remoteFilePath = remotePath + file.getName();

				if (file.isFile()) {
					success = uploadFile(localFilePath, remoteFilePath);
					if (!success)
						break;

				} else {
					// create directory on the server
					boolean created = makeDirectory(remoteFilePath);
					success = created && uploadDirectory(localFilePath, remoteFilePath);
							
					if (!success)
						break;
				}
			}
		}

		return success;
	}

}
