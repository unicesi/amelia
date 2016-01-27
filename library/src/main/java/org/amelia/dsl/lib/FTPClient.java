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
package org.amelia.dsl.lib;

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
 * <li>{@link #makeDirectories(String)}: allows to create several directories at
 * once. The remote path can be either a file or a directory.</li>
 * <li>{@link #upload(String, String)}: allows to upload either a file or a
 * directory. It automatically checks if the directories exist or not. In case
 * they do not, {@link #makeDirectories(String)} is called.</li>
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
	public void makeDirectories(String pathname) throws IOException {
		String workingDirectory = super.printWorkingDirectory();
		internalMakeDirectories(pathname);

		// Return to working directory
		super.changeWorkingDirectory(workingDirectory);
	}

	private void internalMakeDirectories(String pathname) throws IOException {
		String s = remoteFileSeparator();
		boolean cd = super.changeWorkingDirectory(pathname);

		// No such file or directory || Failed to change directory
		if (getReplyCode() == 550 && pathname.contains(s)) {
			String parent = getPathParent(pathname);
			makeDirectories(parent);
		}

		if (!cd)
			if (!super.makeDirectory(pathname))
				throw new IOException("Unable to create remote directory "
						+ pathname + ". Error is: " + super.getReplyString());
	}

	/**
	 * <b>Note</b>: returns false if the user does not have the permission to
	 * access the directory.
	 * 
	 * @param pathname
	 *            The path of the directory
	 * @return whether or not the directory exists (supposing the user has the
	 *         necessary permissions)
	 * @throws IOException
	 */
	public boolean directoryExists(String pathname) throws IOException {
		String workingDirectory = super.printWorkingDirectory();
		boolean exists = changeWorkingDirectory(pathname);
		int replyCode = getReplyCode();

		// No such file or directory || Failed to change directory
		if (replyCode == 550) {
			exists = false;
		}

		// Return to working directory
		changeWorkingDirectory(workingDirectory);

		return exists;
	}

	/**
	 * Removes a directory on the FTP server (with all of its contents)
	 * 
	 * @param pathname
	 *            The pathname of the directory to delete
	 * @return
	 * @throws IOException
	 */
	public void removeDirectoryWithContents(String pathname) throws IOException {

		// Show hidden files
		boolean hiddenFiles = super.getListHiddenFiles();
		super.setListHiddenFiles(true);

		boolean removed = true;
		String s = remoteFileSeparator();

		// List the remote files
		/*
		 * FIXME: Does not work in Mac when hidden files are shown. It's always
		 * returning an empty array
		 */
		FTPFile[] files = super.listFiles(pathname);

		if (files.length == 0)
			removed = super.removeDirectory(pathname);
		else {
			// Remove all of the contents.
			for (int i = 0; i < files.length && removed; i++) {
				String name = files[i].getName();
				if (name.equals(".") || name.equals(".."))
					continue;

				if (files[i].isDirectory())
					removeDirectoryWithContents(pathname + s + name);
				else if (!super.deleteFile(pathname + s + name))
					throw new IOException("Unable to delete remote file "
							+ pathname + s + name + ". Error is: "
							+ super.getReplyString());
			}

			// Remove the directory
			if (!super.removeDirectory(pathname))
				throw new IOException("Unable to remove remote directory "
						+ pathname + ". Error is: " + super.getReplyString());
		}

		// Return to previous state
		super.setListHiddenFiles(hiddenFiles);
	}

	/**
	 * Uploads either a file or directory to the specified remote location. It
	 * automatically creates the directories if necessary.
	 * 
	 * @param localPath
	 *            The pathname of the local file/directory to upload
	 * @param remotePath
	 *            The destination pathname of the remote file/directory
	 * @param overwrite
	 *            If the remote path is an existing directory, it will be
	 *            overwritten if {@code overwrite} is {@code true}
	 * @return whether the specified file was uploaded or not
	 * @throws IOException
	 *             If an I/O error occurs while either sending a command to the
	 *             server or receiving a reply from the server
	 */
	public void upload(String localPath, String remotePath, boolean overwrite)
			throws IOException {

		File file = new File(localPath);
		boolean isDir = file.isDirectory();

		// Unless an existing directory is removed, an IOException will be
		// thrown
		if (isDir && overwrite && directoryExists(remotePath))
			removeDirectoryWithContents(remotePath);

		// Makes sure the directories exists in the remote machine
		makeDirectories(isDir ? remotePath : getPathParent(remotePath));

		if (isDir)
			uploadDirectory(localPath, remotePath);
		else
			uploadFile(localPath, remotePath);
	}

	private void uploadFile(String localPath, String remotePath)
			throws IOException {

		setFileType(FTP.BINARY_FILE_TYPE);

		File localFile = new File(localPath);
		InputStream stream = new FileInputStream(localFile);
		if (!super.storeFile(remotePath, stream))
			throw new IOException("Unable to upload local file " + localPath
					+ ". Error is: " + getReplyString());

		stream.close();
	}

	private void uploadDirectory(String localPath, String remotePath)
			throws IOException {

		String separator = remoteFileSeparator();

		File localDirectory = new File(localPath);
		File[] subFiles = localDirectory.listFiles();

		remotePath = remotePath.isEmpty() ? "" : remotePath + separator;

		if (subFiles != null && subFiles.length > 0) {
			for (File file : subFiles) {

				String localFilePath = file.getAbsolutePath();
				String remoteFilePath = remotePath + file.getName();

				if (file.isFile()) {
					uploadFile(localFilePath, remoteFilePath);
				} else {
					// create directory on the server
					if (!super.makeDirectory(remoteFilePath)) {
						String existing = directoryExists(remoteFilePath) ? "existing "
								: "";
						throw new IOException("Unable to create the "
								+ existing + "remote directory "
								+ remoteFilePath);
					}

					uploadDirectory(localFilePath, remoteFilePath);
				}
			}
		}

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
