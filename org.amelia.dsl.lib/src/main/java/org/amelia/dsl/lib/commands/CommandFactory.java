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
import org.amelia.dsl.lib.descriptors.ChangeDirectory;
import org.amelia.dsl.lib.descriptors.CommandDescriptor;
import org.amelia.dsl.lib.descriptors.Compilation;
import org.amelia.dsl.lib.descriptors.Execution;
import org.amelia.dsl.lib.descriptors.Host;
import org.amelia.dsl.lib.descriptors.Prerequisites;

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
		Command<?> command = null;

		if (descriptor instanceof Compilation)
			command = new Compile(host, (Compilation) descriptor);
		else if (descriptor instanceof Execution)
			command = new Run(host, (Execution) descriptor);
		else if (descriptor instanceof AssetBundle)
			command = new Transfer(host, (AssetBundle) descriptor);
		else if (descriptor instanceof Prerequisites)
			command = new PrerequisiteCheck(host, (Prerequisites) descriptor);
		else if (descriptor instanceof ChangeDirectory)
			command = new Cd(host, (ChangeDirectory) descriptor);
		else
			command = new Command.Simple(host, descriptor);

		return command;
	}

}
