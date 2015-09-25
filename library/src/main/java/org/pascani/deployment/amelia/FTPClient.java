package org.pascani.deployment.amelia;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.net.ftp.FTP;

public class FTPClient extends org.apache.commons.net.ftp.FTPClient {

	@Override
	public boolean makeDirectory(String remotePath) throws IOException {
		mlistDir(remotePath);

		// No such file or directory
		if (getReplyCode() == 550 && remotePath.contains(File.separator)) {
			String parent = getParent(remotePath);
			makeDirectory(parent);
		}

		return super.makeDirectory(remotePath);
	}

	protected String getParent(String path) {
		int last = path.length();

		if (path.contains(File.separator))
			last = path.lastIndexOf(File.separator);

		String parent = path.substring(0, last);
		return parent;
	}

	public boolean upload(String localPath, String remotePath)
			throws IOException {

		File file = new File(localPath);
		boolean dir = file.isDirectory();
		boolean success = true;

		// Makes sure the directory exists in the remote machine
		makeDirectory(dir ? remotePath : getParent(remotePath));

		if (dir)
			success = uploadDirectory(localPath, remotePath);
		else
			success = uploadFile(localPath, remotePath);

		return success;
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
					success = created
							&& uploadDirectory(localFilePath, remoteFilePath);

					if (!success)
						break;
				}
			}
		}

		return success;
	}

}
