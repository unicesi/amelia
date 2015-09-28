package org.pascani.deployment.amelia.descriptors;

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

public class AssetBundle {

	private Map<String, List<String>> transfers;

	/**
	 * The logger
	 */
	private final static Logger logger = LogManager
			.getLogger(AssetBundle.class);

	public AssetBundle() {
		this.transfers = new HashMap<String, List<String>>();
	}

	public AssetBundle(Map<String, List<String>> transfers) {
		this.transfers = transfers;
	}
	
	/**
	 * \{>\s*variable_name\s*\} where variable_name is the name of the variable to replace.
	 * 
	 * @param variables
	 * @return
	 */
	public AssetBundle resolveVariables(Map<String, String> variables) {
		AssetBundle bundle = new AssetBundle();
		
		for(Map.Entry<String, List<String>> entry : this.transfers.entrySet()) {
			String local = replaceAll(entry.getKey(), variables);
			
			for(String remote : entry.getValue())
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
					logger.warn("Bad format in asset bundle: [" + l + "] " + line);
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

	public Map<String, List<String>> transfers() {
		return this.transfers;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (Map.Entry<String, List<String>> pair : this.transfers.entrySet())
			sb.append(pair.getKey() + ": " + pair.getValue() + "\n");

		return sb.toString();
	}

}
