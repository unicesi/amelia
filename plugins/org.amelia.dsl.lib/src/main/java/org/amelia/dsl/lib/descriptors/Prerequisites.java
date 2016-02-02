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
package org.amelia.dsl.lib.descriptors;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class Prerequisites extends CommandDescriptor {

	private final Version javaVersion;

	private final Version frascatiVersion;

	public Prerequisites(final Version javaVersion,
			final Version frascatiVersion) {
		super(new CommandDescriptor.Builder()
				.withErrorMessage("Dissatisfied prerequisites")
				.withSuccessMessage("Prerequisites satisfied"));
		this.javaVersion = javaVersion;
		this.frascatiVersion = frascatiVersion;
	}

	public Version javaVersion() {
		return this.javaVersion;
	}

	public Version frascatiVersion() {
		return this.frascatiVersion;
	}

}
