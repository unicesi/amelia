package org.pascani.deployment.amelia;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.net.ftp.FTP;

/**
 * Wrapper implementation of the Apache commons net FTP client. In essence, this
 * class (re)implements:
 * 
 * <ul>
 * <li>{@link #makeDirectory(String)}: allows to create several directories at
 * once. The remote path can be either a file or a directory.</li>
 * <li>{@link #upload(String, String)}: allows to upload either a file or a
 * directory. It automatically checks if the directories exist or not. In case
 * they do not, {@link #makeDirectory(String)} is called.</li>
 * </ul>
 * 
 * @author Miguel Jiménez - Initial contribution and API
 */
public class FTPClient extends org.apache.commons.net.ftp.FTPClient {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.commons.net.ftp.FTPClient#makeDirectory(java.lang.String)
	 */
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

	/**
	 * Uploads either a file or directory to the specified remote location. It
	 * automatically creates the directories if necessary.
	 * 
	 * @param localPath
	 *            The pathname of the local file/directory to upload
	 * @param remotePath
	 *            The destination pathname of the remote file/directory
	 * @return whether the specified file was uploaded or not
	 * @throws IOException
	 *             If an I/O error occurs while either sending a command to the
	 *             server or receiving a reply from the server
	 */
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

	private String getParent(String path) {
		int last = path.length();

		if (path.contains(File.separator))
			last = path.lastIndexOf(File.separator);

		String parent = path.substring(0, last);
		return parent;
	}

}
