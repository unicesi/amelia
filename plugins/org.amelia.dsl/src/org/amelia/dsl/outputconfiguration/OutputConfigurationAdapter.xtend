/*
 * Copyright © 2015 Universidad Icesi
 * 
 * This file is part of the Amelia DSL.
 * 
 * The Amelia DSL is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Amelia DSL is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with The Amelia DSL. If not, see <http://www.gnu.org/licenses/>.
 */
package org.amelia.dsl.outputconfiguration

import org.eclipse.emf.common.notify.Adapter
import org.eclipse.emf.common.notify.Notification
import org.eclipse.emf.common.notify.Notifier

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class OutputConfigurationAdapter implements Adapter {

	private String outputConfigurationName

	new(String outputConfigurationName) {
		this.outputConfigurationName = outputConfigurationName
	}

	def String getOutputConfigurationName() {
		return this.outputConfigurationName
	}

	override void notifyChanged(Notification notification) {
	}

	override Notifier getTarget() {
		return null
	}

	override void setTarget(Notifier newTarget) {
	}

	override boolean isAdapterForType(Object type) {
		return false
	}

}
