package org.pascani.deployment.amelia.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;

/**
 * Wrapper implementation of the Apache commons net FTP client. In essence, this
 * class implements:
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

	/**
	 * Creates new sub-directories on the FTP server in the current directory
	 * (if a relative pathname is given) or where specified (if an absolute
	 * pathname is given).
	 * 
	 * @param pathname
	 *            The pathname of the directory to create
	 * @return whether the sub-directories were successfully created or not
	 * @throws IOException
	 */
	public boolean makeDirectories(String pathname) throws IOException {
		String s = remoteFileSeparator();
		super.mlistDir(pathname);

		// No such file or directory
		if (getReplyCode() == 550 && pathname.contains(s)) {
			String parent = getPathParent(pathname);
			makeDirectories(parent);
		}

		return super.makeDirectory(pathname);
	}

	/**
	 * Removes a directory on the FTP server (with all of its contents)
	 * 
	 * @param pathname
	 *            The pathname of the directory to delete
	 * @return
	 * @throws IOException
	 */
	public boolean removeDirectoryWithContents(String pathname)
			throws IOException {

		boolean removed = true;
		String s = remoteFileSeparator();

		// List the remote files
		FTPFile[] files = mlistDir(pathname);

		if (files.length == 0)
			removed = super.removeDirectory(pathname);
		else {
			// Remove all of the contents. files[0] is the directory itself
			for (int i = 1; i < files.length && removed; i++) {
				String name = files[i].getName();
				if (name.equals(".") || name.equals(".."))
					continue;

				if (files[i].isDirectory())
					removed = removeDirectoryWithContents(pathname + s + name);
				else
					removed = super.deleteFile(pathname + s + name);
			}

			// Remove the directory
			super.removeDirectory(pathname);
		}

		return removed;
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
		boolean isDir = file.isDirectory();
		boolean uploaded = true;

		// Makes sure the directories exists in the remote machine
		makeDirectories(isDir ? remotePath : getPathParent(remotePath));

		if (isDir)
			uploaded = uploadDirectory(localPath, remotePath);
		else
			uploaded = uploadFile(localPath, remotePath);

		return uploaded;
	}

	private boolean uploadFile(String localPath, String remotePath)
			throws IOException {

		setFileType(FTP.BINARY_FILE_TYPE);

		File localFile = new File(localPath);
		InputStream stream = new FileInputStream(localFile);
		boolean uploaded = storeFile(remotePath, stream);

		stream.close();
		return uploaded;
	}

	private boolean uploadDirectory(String localPath, String remotePath)
			throws IOException {

		String separator = remoteFileSeparator();
		boolean uploaded = true;

		File localDirectory = new File(localPath);
		File[] subFiles = localDirectory.listFiles();

		remotePath = remotePath.isEmpty() ? "" : remotePath + separator;

		if (subFiles != null && subFiles.length > 0) {
			for (File file : subFiles) {

				String localFilePath = file.getAbsolutePath();
				String remoteFilePath = remotePath + file.getName();

				if (file.isFile()) {
					uploaded = uploadFile(localFilePath, remoteFilePath);
					if (!uploaded)
						break;

				} else {
					// create directory on the server
					boolean created = super.makeDirectory(remoteFilePath);
					uploaded = created
							&& uploadDirectory(localFilePath, remoteFilePath);

					if (!uploaded)
						break;
				}
			}
		}

		return uploaded;
	}

	/**
	 * @return The parent path of the specified path, based on the file
	 *         separator
	 * @throws IOException
	 */
	private String getPathParent(String path) throws IOException {
		String separator = remoteFileSeparator();
		int last = path.length();

		if (path.contains(separator))
			last = path.lastIndexOf(separator);

		String parent = path.substring(0, last);
		return parent;
	}

	/**
	 * By default, the file separator is UNIX like (/), unless the system type
	 * specifies a Windows system.
	 * 
	 * @return The remote file separator
	 */
	public String remoteFileSeparator() throws IOException {
		String separator = "/";
		String systemType = super.getSystemType().toLowerCase();

		if (systemType.contains("windows"))
			separator = "\\";

		return separator;
	}

}
