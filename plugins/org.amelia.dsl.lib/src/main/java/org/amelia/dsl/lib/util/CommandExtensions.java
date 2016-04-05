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
package org.amelia.dsl.lib.util;

import java.util.List;

import org.amelia.dsl.lib.descriptors.CommandDescriptor;
import org.amelia.dsl.lib.descriptors.Host;
import org.eclipse.xtext.xbase.lib.Inline;
import org.eclipse.xtext.xbase.lib.Pure;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class CommandExtensions {

	/**
	 * The binary {@code ->} operator.
	 * 
	 * @param descriptor
	 *            The command descriptor
	 * @param host
	 *            The host where the descriptor runs
	 * @return {@code descriptor.runsOn(host)}
	 */
	@Inline(value = "$1.runsOn($2)")
	public static boolean operator_mappedTo(
			CommandDescriptor descriptor, Host host) {
		return descriptor.runsOn(host);
	}

	/**
	 * The binary {@code ->} operator.
	 * 
	 * @param descriptor
	 *            The command descriptor
	 * @param hosts
	 *            The hosts where the descriptor runs
	 * @return {@code descriptor.runsOn(hosts)}
	 */
	@Inline(value = "$1.runsOn($2)")
	public static boolean operator_mappedTo(
			CommandDescriptor descriptor, Host[] hosts) {
		return descriptor.runsOn(hosts);
	}

	/**
	 * The binary {@code ->} operator.
	 * 
	 * @param descriptors
	 *            The command descriptors
	 * @param host
	 *            The host where the descriptors run
	 * @return the conjunction of {@code descriptor.runsOn(hosts)} for all
	 *         descriptors
	 */
	public static boolean operator_mappedTo(
			CommandDescriptor[] descriptors, Host host) {
		boolean all = true;
		for (CommandDescriptor descriptor : descriptors) {
			all &= descriptor.runsOn(host);
		}
		return all;
	}
	
	/**
	 * The binary {@code ->} operator.
	 * 
	 * @param descriptors
	 *            The command descriptors
	 * @param host
	 *            The host where the descriptors run
	 * @return the conjunction of {@code descriptor.runsOn(hosts)} for all
	 *         descriptors
	 */
	public static boolean operator_mappedTo(
			List<? extends CommandDescriptor> descriptors, Host host) {
		return operator_mappedTo(descriptors.toArray(new CommandDescriptor[0]), host);
	}

	/**
	 * The binary {@code ->} operator.
	 * 
	 * @param descriptors
	 *            The command descriptors
	 * @param hosts
	 *            The hosts where the descriptors run
	 * @return the conjunction of {@code descriptor.runsOn(hosts)} for all
	 *         descriptors
	 */
	public static boolean operator_mappedTo(
			CommandDescriptor[] descriptors, Host[] hosts) {
		boolean all = true;
		for (CommandDescriptor descriptor : descriptors) {
			all &= descriptor.runsOn(hosts);
		}
		return all;
	}
	
	/**
	 * The binary {@code ->} operator.
	 * 
	 * @param descriptors
	 *            The command descriptors
	 * @param hosts
	 *            The hosts where the descriptors run
	 * @return the conjunction of {@code descriptor.runsOn(hosts)} for all
	 *         descriptors
	 */
	public static boolean operator_mappedTo(
			List<? extends CommandDescriptor> descriptors, List<Host> hosts) {
		return operator_mappedTo(descriptors.toArray(new CommandDescriptor[0]), hosts.toArray(new Host[0]));
	}

	/**
	 * The binary {@code <=} operator.
	 * 
	 * @param descriptor
	 *            The dependent command descriptor
	 * @param dependency
	 *            The dependency
	 * @return {@code descriptor.dependsOn(dependency)}
	 */
	@Inline(value = "$1.dependsOn($2)")
	public static boolean operator_lessEqualsThan(CommandDescriptor descriptor,
			CommandDescriptor dependency) {
		return descriptor.dependsOn(dependency);
	}

	/**
	 * The binary {@code <=} operator.
	 * 
	 * @param descriptor
	 *            The dependent command descriptor
	 * @param dependencies
	 *            The dependencies
	 * @return {@code descriptor.dependsOn(dependency)}
	 */
	@Inline(value = "$1.dependsOn($2)")
	public static boolean operator_lessEqualsThan(CommandDescriptor descriptor,
			CommandDescriptor[] dependencies) {
		return descriptor.dependsOn(dependencies);
	}

	/**
	 * The binary {@code <=} operator.
	 * 
	 * @param descriptors
	 *            The dependent command descriptors
	 * @param dependency
	 *            The dependency
	 * @return the conjunction of {@code descriptor.dependsOn(dependency)} for
	 *         all descriptors
	 */
	public static boolean operator_lessEqualsThan(CommandDescriptor[] descriptors,
			CommandDescriptor dependency) {
		boolean all = true;
		for (CommandDescriptor descriptor : descriptors) {
			all &= descriptor.dependsOn(dependency);
		}
		return all;
	}

	/**
	 * The binary {@code <=} operator.
	 * 
	 * @param descriptors
	 *            The dependent command descriptors
	 * @param dependencies
	 *            The dependencies
	 * @return the conjunction of {@code descriptor.dependsOn(dependencies)} for
	 *         all descriptors
	 */
	public static boolean operator_lessEqualsThan(CommandDescriptor[] descriptors,
			CommandDescriptor[] dependencies) {
		boolean all = true;
		for (CommandDescriptor descriptor : descriptors) {
			all &= descriptor.dependsOn(dependencies);
		}
		return all;
	}
	
	/**
	 * The binary {@code +} operator.
	 * 
	 * @param left
	 *            The command at the left hand side of the plus operand
	 * @param right
	 *            The command at the right hand side of the plus operand
	 * @return A new {@link CommandDescriptor} with the same configuration as
	 *         {@code left}, but augmenting its command with {@code right}.
	 */
	@Pure
	@Inline(value = "$1.augmentWith($2)")
	public static CommandDescriptor operator_plus(
			CommandDescriptor left, CommandDescriptor right) {
		return left.augmentWith(right);
	}

}
