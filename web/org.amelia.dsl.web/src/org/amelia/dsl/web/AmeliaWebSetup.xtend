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
package org.amelia.dsl.web

import com.google.inject.Guice
import com.google.inject.Injector
import org.amelia.dsl.AmeliaRuntimeModule
import org.amelia.dsl.AmeliaStandaloneSetup
import org.eclipse.xtext.util.Modules2
import org.amelia.dsl.ide.AmeliaIdeModule

/**
 * Initialization support for running Xtext languages in web applications.
 * 
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaWebSetup extends AmeliaStandaloneSetup {
	
	override Injector createInjector() {
		return Guice.createInjector(Modules2.mixin(new AmeliaRuntimeModule, new AmeliaIdeModule, new AmeliaWebModule))
	}
	
}
