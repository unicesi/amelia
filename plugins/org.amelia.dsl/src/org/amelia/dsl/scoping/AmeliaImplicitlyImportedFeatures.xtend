/*
 * Copyright © 2015 Universidad Icesi
 * 
 * This file is part of the Amelia project.
 * 
 * The Amelia project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Amelia project is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with The Amelia project. If not, see <http://www.gnu.org/licenses/>.
 */
 package org.amelia.dsl.scoping

import org.amelia.dsl.lib.util.CommandExtensions
import org.amelia.dsl.lib.util.Hosts
import org.eclipse.xtext.xbase.scoping.batch.ImplicitlyImportedFeatures

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaImplicitlyImportedFeatures extends ImplicitlyImportedFeatures {
	
	override protected getStaticImportClasses() {
		return (super.getStaticImportClasses() + #[Hosts]).toList
	}
	
	override protected getExtensionClasses() {
		return (super.getExtensionClasses() + #[CommandExtensions]).toList
	}
	
}
