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
package org.pascani.deployment.amelia.descriptors;

import static org.pascani.deployment.amelia.util.Strings.ascii;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pascani.deployment.amelia.util.Pair;
import org.pascani.deployment.amelia.util.Strings;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class AssetBundle extends CommandDescriptor {

	private final Map<String, List<String>> transfers;

	private int currentPadding;

	private boolean overwrite;

	/**
	 * The logger
	 */
	private final static Logger logger = LogManager
			.getLogger(AssetBundle.class);

	public AssetBundle(Map<String, List<String>> transfers,
			final boolean overwrite) {
		super("put [...]", null, null);
		this.transfers = transfers;
		this.currentPadding = 0;
		this.overwrite = overwrite;
	}

	public AssetBundle() {
		this(new HashMap<String, List<String>>(), true);
	}

	public AssetBundle resolveVariables(Pair<String, String>... variables) {
		Map<String, String> _variables = new HashMap<String, String>();

		for (Pair<String, String> variable : variables)
			_variables.put(variable.getKey(), variable.getValue());

		return resolveVariables(_variables);
	}

	/**
	 * \{>\s*variable_name\s*\} where variable_name is the name of the variable
	 * to replace.
	 * 
	 * @param variables
	 * @return
	 */
	public AssetBundle resolveVariables(Map<String, String> variables) {
		AssetBundle bundle = new AssetBundle();

		for (Map.Entry<String, List<String>> entry : this.transfers.entrySet()) {
			String local = replaceAll(entry.getKey(), variables);

			for (String remote : entry.getValue())
				bundle.add(local, replaceAll(remote, variables));
		}
		return bundle;
	}

	private String replaceAll(String text, Map<String, String> variables) {
		String newText = text;

		for (Map.Entry<String, String> entry : variables.entrySet()) {
			String regexp = "\\{>\\s*" + entry.getKey() + "\\s*\\}";
			newText = newText.replaceAll(regexp, entry.getValue());
		}
		return newText;
	}

	public static AssetBundle fromFile(String pathname) throws IOException {
		AssetBundle bundle = new AssetBundle();
		InputStream in = new FileInputStream(pathname);
		InputStreamReader streamReader = null;
		BufferedReader bufferedReader = null;

		try {
			streamReader = new InputStreamReader(in);
			bufferedReader = new BufferedReader(streamReader);

			String line;
			int l = 1;
			while ((line = bufferedReader.readLine()) != null) {
				if (!line.isEmpty() && line.contains("\t")) {
					String[] data = line.split("\t");
					bundle.add(data[0], data[1]);
				} else {
					logger.warn("Bad format in asset bundle: [" + l + "] "
							+ line);
				}

				++l;
			}
		} finally {
			in.close();
			streamReader.close();
			bufferedReader.close();
		}

		return bundle;
	}

	public AssetBundle add(String local, String remote) {
		if (!this.transfers.containsKey(local))
			this.transfers.put(local, new ArrayList<String>());

		this.transfers.get(local).add(remote);
		return this;
	}

	@Override
	public void done(Host host) {
		currentPadding = host.toString().length();
		super.done(host);
	}

	@Override
	public String doneMessage() {
		String message = ascii(10003) + " ";
		// 5 = __<identifier>_✓_
		message += successMessage == null ? toString(currentPadding + 5)
				: successMessage;

		return message;
	}

	@Override
	public String failMessage() {
		String message = ascii(10007) + " ";
		// 5 = __<identifier>_✗_
		message += errorMessage == null ? toString(currentPadding + 5)
				: errorMessage;

		return message;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((this.transfers == null) ? 0 : this.transfers.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AssetBundle other = (AssetBundle) obj;
		if (this.transfers == null) {
			if (other.transfers != null)
				return false;
		} else if (!this.transfers.equals(other.transfers))
			return false;
		return true;
	}

	public Map<String, List<String>> transfers() {
		return this.transfers;
	}

	public String toString(int _padding) {
		StringBuilder sb = new StringBuilder();
		String padding = String.format("%" + (_padding > 0 ? _padding : "")
				+ "s", "File transfer");
		_padding = _padding + 13; // "File transfer".length
		boolean firstKey = true;

		for (String key : transfers.keySet()) {
			if (!firstKey)
				padding = String.format("%" + _padding + "s", "");

			String _key = Strings.truncate(key, 20, 20);
			sb.append(padding + " " + _key);
			boolean firstValue = true;

			for (String value : transfers.get(key)) {
				String initial = String.format("%-" + _key.length() + "s", "")
						+ padding;
				if (firstValue)
					initial = "";

				sb.append(initial + " -> " + value + "\n");
				firstValue = false;
			}

			firstKey = false;
		}

		sb.deleteCharAt(sb.length() - 1); // remove last line break
		return sb.toString();
	}

	@Override
	public String toString() {
		return toString(0);
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public boolean overwrite() {
		return this.overwrite;
	}

}
