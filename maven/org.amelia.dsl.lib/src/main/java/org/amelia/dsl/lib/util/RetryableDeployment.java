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

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
public class RetryableDeployment {

	/**
	 * A function without any parameters. <b>Taken from the XBase library.</b>
	 * 
	 * @param <Result>
	 *            the result of the single closure {@link #apply() method}.
	 */
	public static interface Function<Result> {
		Result apply();
	}

	/**
	 * A procedure without any parameters, e.g. a {@link Runnable}. <b>Taken
	 * from the XBase library.</b>
	 */
	public static interface Procedure {
		void apply();
	}

	/**
	 * Executes the specified deployment until one of two things happens: either
	 * a deployment attempt is successful, or all attempts were unsuccessful.
	 * 
	 * @param deployment
	 *            The function containing all the deployment logic
	 * @param attempts
	 *            The number of attempts to perform in case the deployment fails
	 * @return whether or not the deployment was successful
	 * @throws Exception
	 *             In case the deployment throws an {@link Exception}
	 */
	public boolean deploy(Function<Boolean> deployment, int attempts)
			throws Exception {
		return deploy(deployment, attempts, null);
	}

	/**
	 * Executes the specified deployment until one of two things happens: either
	 * a deployment attempt is successful, or all attempts were unsuccessful.
	 * 
	 * @param deployment
	 *            The function containing all the deployment logic
	 * @param attempts
	 *            The number of attempts to perform in case the deployment fails
	 * @param errorCallback
	 *            A callback that gets called when all attempts have been
	 *            unsuccessful
	 * @return whether or not the deployment was successful
	 * @throws Exception
	 *             In case the deployment throws an {@link Exception}
	 */
	public boolean deploy(Function<Boolean> deployment, int attempts,
			Procedure errorCallback) throws Exception {
		boolean successful = false;
		for (int i = 0; i < attempts && !successful; i++) {
			successful = deployment.apply();
		}
		if (!successful && errorCallback != null) {
			errorCallback.apply();
		}
		return successful;
	}

}
