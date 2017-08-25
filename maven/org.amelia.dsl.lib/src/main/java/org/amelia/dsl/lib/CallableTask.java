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
package org.amelia.dsl.lib;

import org.amelia.dsl.lib.descriptors.Host;

/**
 * 
 * @author Miguel Jiménez - Initial contribution and API
 *
 * @param <V>
 *            The return type
 */
public interface CallableTask<V> {

	/**
	 * Call this task.
	 * @param host the host where this task is being executed
	 * @param prompt the current prompt
	 * @param quiet whether to print success and error messages
	 * @return an object
	 * @throws Exception is case there's an error executing this task
	 */
	public V call(Host host, String prompt, boolean quiet) throws Exception;
	
}
