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
package org.amelia.dsl.lib.commands;

import org.amelia.dsl.lib.descriptors.AssetBundle;
import org.amelia.dsl.lib.descriptors.CommandDescriptor;
import org.amelia.dsl.lib.descriptors.Host;

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class CommandFactory {

	private static CommandFactory instance;

	private CommandFactory() {
	}

	public static CommandFactory getInstance() {
		if (instance == null)
			instance = new CommandFactory();

		return instance;
	}

	public Command<?> getCommand(Host host, CommandDescriptor descriptor) {
		if (descriptor instanceof AssetBundle)
			return new Transfer(host, (AssetBundle) descriptor);
		else
			return new Command.Simple(host, descriptor);
	}

}
