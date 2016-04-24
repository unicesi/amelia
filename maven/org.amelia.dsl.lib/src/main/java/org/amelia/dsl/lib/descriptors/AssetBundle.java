/*
 * Copyright © 2015 Universidad Icesi
 * 
 * This file is part of the Amelia project.
 * 
 * The Amelia project is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Amelia project is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the Amelia project. If not, see <http://www.gnu.org/licenses/>.
 */
package org.amelia.dsl.lib.descriptors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.amelia.dsl.lib.util.ANSI;
import org.amelia.dsl.lib.util.CallableTask;
import org.amelia.dsl.lib.util.Log;
import org.amelia.dsl.lib.util.Pair;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class AssetBundle extends CommandDescriptor {

	private final Map<String, List<String>> transfers;

	private boolean overwrite;

	public AssetBundle(Map<String, List<String>> transfers,
			final boolean overwrite) {
		super(new CommandDescriptor.Builder());
		this.transfers = transfers;
		this.overwrite = overwrite;
		final AssetBundle that = this;
		this.callable = new CallableTask<Void>() {
			@Override
			public Void call(Host host, String prompt) throws Exception {
				try {
					host.ftp().upload(that);
					Log.success(host, that.doneMessage());
				} catch (Exception e) {
					Log.error(host, that.failMessage());
					throw e;
				}
				return null;
			}
		};
	}

	public AssetBundle() {
		this(new HashMap<String, List<String>>(), true);
	}
	
	@Override public String doneMessage() {
		return toString();
	}
	
	@Override public String failMessage() {
		return "Unsuccessful transfer" + (this.transfers.size() == 1 ? "" : "s");
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

	public AssetBundle add(String local, String remote) {
		if (!this.transfers.containsKey(local))
			this.transfers.put(local, new ArrayList<String>());

		this.transfers.get(local).add(remote);
		return this;
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

	@Override
	public String toString() {
		if (this.transfers.isEmpty())
			return "No files to transfer";

		StringBuilder sb = new StringBuilder();
		String s = (this.transfers.size() > 1 ? "s" : "");
		sb.append("Successful transfer" + s + "\n");
		int t = 0;
		
		for (String local : transfers.keySet()) {
			if (t++ == 0)
				sb.append(Log.SEPARATOR_LONG);

			int i = 0;
			String formattedLocal = ANSI.YELLOW.format(local);
			sb.append(String.format("\n   local: %s\n", formattedLocal));

			for (String remote : transfers.get(local)) {
				String label = i++ == 0 ? "remote:" : "       ";
				String formattedRemote = ANSI.YELLOW.format(remote);
				sb.append(String.format("  %s %s\n", label, formattedRemote));
			}
			sb.append(Log.SEPARATOR_LONG);
		}

		return sb.toString();
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public boolean overwrite() {
		return this.overwrite;
	}

}
