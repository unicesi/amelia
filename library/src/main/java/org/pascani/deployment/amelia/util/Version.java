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
package org.pascani.deployment.amelia.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version {

	private final String versionExpression;

	private final String modifier;

	private final String rest;

	/**
	 * <p>
	 * A valid version expression is composed of two parts: first, an optional
	 * modifier to specify flexible versions, and second, the actual version.
	 * Versions must contain numeric characters, '-' and/or '.'. For instance,
	 * the following versions are valid: *, 1.6.0_23, >1.6, <1.7.
	 * </p>
	 * 
	 * <ul>
	 * <li>version : must match version exactly</li>
	 * <li>>version : must be greater than version</li>
	 * <li>>=version : greater or equal than version</li>
	 * <li><version : less than version</li>
	 * <li><=version : less or equal than version</li>
	 * <li>* : Matches any version</li>
	 * </ul>
	 * 
	 * @param versionExpression
	 * @throws Exception
	 */
	public Version(String versionExpression) throws Exception {

		versionExpression = versionExpression.replace("_", ".");

		if (versionExpression.equals("*")) {
			this.versionExpression = versionExpression;
			this.modifier = "*";
			this.rest = null;
		} else {
			Pattern pattern = Pattern.compile("^((<|>)=?)?(\\d+(\\.\\d+)*)$");
			Matcher matcher = pattern.matcher(versionExpression);

			if (!matcher.find())
				throw new Exception("Invalid version expression \""
						+ versionExpression);

			this.versionExpression = versionExpression;
			this.modifier = matcher.group(1);
			this.rest = matcher.group(3);
		}
	}

	/**
	 * Verifies whether or not the given semantic version is compliant with the
	 * specified version expression.
	 * 
	 * @param version
	 *            The version string to validate
	 * @return whether the version string is equivalent to the version
	 *         expression
	 * @throws Exception
	 *             If the version expression is not valid
	 */
	public boolean isEquivalent(String version) {
		boolean accepted = true;

		if (!this.versionExpression.equals("*")) {
			String[] parts = version.replace("_", ".").split("\\.");
			String[] rest = this.rest.split("\\.");

			parts[0] = parts[0] + ".";
			rest[0] = rest[0] + ".";

			accepted = compare(Strings.join(parts, ""), Strings.join(rest, ""));
		}

		return accepted;
	}

	private boolean compare(String _a, String _b) {

		boolean ok = true;
		double a = Double.parseDouble(_a);
		double b = Double.parseDouble(_b);

		if (this.modifier == null || this.modifier.equals("")) {
			ok = a == b;
		} else if (this.modifier.equals("<")) {
			ok = a < b;
		} else if (this.modifier.equals(">")) {
			ok = a > b;
		} else if (this.modifier.equals("<=")) {
			ok = a <= b;
		} else if (this.modifier.equals(">=")) {
			ok = a >= b;
		}

		return ok;
	}
	
	@Override
	public String toString() {
		return this.versionExpression;
	}

}
