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
import org.pascani.deployment.amelia.util.Strings;

public class AssetBundle extends CommandDescriptor {

	private Map<String, List<String>> transfers;
	
	private int currentPadding;

	/**
	 * The logger
	 */
	private final static Logger logger = LogManager
			.getLogger(AssetBundle.class);

	public AssetBundle(Map<String, List<String>> transfers) {
		super("put [...]", "Transfer completed", "Transfer failure");
		this.transfers = transfers;
		this.currentPadding = 0;
	}

	public AssetBundle() {
		this(new HashMap<String, List<String>>());
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
		message += successMessage == null ? toString(currentPadding + 4) : successMessage;

		return message;
	}

	public Map<String, List<String>> transfers() {
		return this.transfers;
	}
	
	public String toString(int leftPadding) {
		StringBuilder sb = new StringBuilder();

		String s = this.transfers.size() == 1 ? "" : "s";
		String description = "File transfer" + s + ": ";
		int l = description.length(), i = 0, maxLength = 30;

		for (Map.Entry<String, List<String>> pair : this.transfers.entrySet()) {
			String key = Strings.truncate(pair.getKey(), maxLength, maxLength);
			
			int c = i == 0 ? 0 : maxLength;
			String left = i == 0 ? description : "";

			String local = String.format("%" + (l + c) + "s", key);
			String remotes = Strings.join(pair.getValue(),
					String.format("%-" + (l + c + 5 + leftPadding) + "s", "\n"));

			sb.append(String.format("%s%s -> %s", left, local, remotes) + "\n");
			
			i++;
		}

		sb.deleteCharAt(sb.length() - 1); // remove last line break
		return sb.toString();
	}

	@Override
	public String toString() {
		return toString(0);
	}

}
