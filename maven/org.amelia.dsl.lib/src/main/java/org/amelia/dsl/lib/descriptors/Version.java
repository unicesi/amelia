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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.amelia.dsl.lib.util.Arrays;
import org.amelia.dsl.lib.util.Pair;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Version {

	private final String versionExpression;

	private final List<Pair<String, String>> expressions;

	/**
	 * <p>
	 * A valid version expression is composed of two parts: first, an optional
	 * relational operator to specify flexible versions, and second, the actual
	 * version. Versions must contain numeric characters, '-' and/or '.'.
	 * Additionally, versions can be composed with conjunction operators (&) to
	 * enforce meeting multiple criteria. For instance, the following versions
	 * are valid: *, 1.6.0_23, > 1.6, < 1.7, >= 1.6 & < 1.7.
	 * </p>
	 * 
	 * <ul>
	 * <li>version : must match version exactly</li>
	 * <li>>version : must be greater than version</li>
	 * <li>>=version : greater or equal than version</li>
	 * <li>< version : less than version</li>
	 * <li><= version : less or equal than version</li>
	 * <li>&lt;version&gt; & ... & &lt;version&gt; : compose multiple criteria</li>
	 * <li>* : Matches any version (cannot be composed with the '&' operator)</li>
	 * </ul>
	 * 
	 * @param versionExpression
	 * @throws Exception
	 */
	public Version(final String versionExpression) throws Exception {

		String expression = versionExpression.replace("_", ".")
				.replace(" ", "");
		this.versionExpression = expression;

		if (versionExpression.equals("*")) {
			this.expressions = new ArrayList<Pair<String, String>>();
			this.expressions.add(new Pair<String, String>("*", null));
		} else {
			this.expressions = parse(expression);
		}
	}

	public List<Pair<String, String>> parse(final String versionExpression)
			throws Exception {
		List<Pair<String, String>> expressions = new ArrayList<Pair<String, String>>();

		// simple version expression
		String simple = "((<|>)=?)?(\\d+(\\.\\d+)*)";

		StringBuilder sb = new StringBuilder();
		sb.append("^");
		sb.append(simple);
		sb.append("(");
		sb.append("&" + simple); // conjunction
		sb.append(")*");
		sb.append("$");

		// ^((<|>)=?)?(\d+(\.\d+)*)(&((<|>)=?)?(\d+(\.\d+)*))*$
		Pattern pattern = Pattern.compile(sb.toString());
		Matcher matcher = pattern.matcher(versionExpression);

		if (!matcher.find())
			throw new Exception("Invalid version expression \""
					+ versionExpression);

		String[] groups = matcher.group(0).split("&");

		for (String group : groups) {
			Pattern sp = Pattern.compile(simple);
			Matcher sm = sp.matcher(group);
			sm.find();
			expressions.add(new Pair<String, String>(sm.group(1), sm.group(3)));
		}

		return expressions;
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
	public boolean isCompliant(String version) {
		boolean compliant = true;

		if (!this.versionExpression.equals("*")) {
			String[] parts = version.replace("_", ".").split("\\.");
			parts[0] = parts[0] + ".";

			for (Pair<String, String> expression : this.expressions) {
				String[] rest = expression.getValue().split("\\.");
				rest[0] = rest[0] + ".";

				// It must be compliant with all expressions
				compliant &= compare(expression.getKey(),
						Arrays.join(parts, ""), Arrays.join(rest, ""));
			}
		}

		return compliant;
	}

	private boolean compare(final String operator, final String _a,
			final String _b) {

		boolean ok = true;
		double a = Double.parseDouble(_a);
		double b = Double.parseDouble(_b);

		if (operator == null || operator.equals("")) {
			ok = a == b;
		} else if (operator.equals("<")) {
			ok = a < b;
		} else if (operator.equals(">")) {
			ok = a > b;
		} else if (operator.equals("<=")) {
			ok = a <= b;
		} else if (operator.equals(">=")) {
			ok = a >= b;
		}

		return ok;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\"");

		for (int i = 0; i < this.expressions.size(); i++) {
			Pair<String, String> expression = this.expressions.get(i);
			sb.append(expression.getKey() + " ");
			sb.append(expression.getValue());

			if (i < this.expressions.size() - 1)
				sb.append(" & ");
		}

		sb.append("\"");
		return sb.toString();
	}

}
