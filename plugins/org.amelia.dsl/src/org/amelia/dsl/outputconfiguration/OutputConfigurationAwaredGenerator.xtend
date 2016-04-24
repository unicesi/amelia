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
package org.amelia.dsl.outputconfiguration

import com.google.inject.Inject
import org.amelia.dsl.generator.AmeliaGenerator
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.generator.IFileSystemAccess
import org.eclipse.xtext.xbase.compiler.JvmModelGenerator

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class OutputConfigurationAwaredGenerator extends JvmModelGenerator {
	
	@Inject private AmeliaGenerator generator

	override void doGenerate(Resource input, IFileSystemAccess fsa) {
		val _contents = input.getContents()
		for (obj : _contents) {
			val adapters = obj.eAdapters.filter(OutputConfigurationAdapter)
			for (adapter : adapters) {
				var outputConfiguration = adapter.getOutputConfigurationName()
				if (outputConfiguration == AmeliaOutputConfigurationProvider::AMELIA_OUTPUT) {
					val sfsa = new SingleOutputConfigurationFileSystemAccess(fsa, outputConfiguration)
					this.internalDoGenerate(obj, sfsa) // AmeliaJvmModelInferrer
					this.generator.doGenerate(input, fsa) // AmeliaGenerator
				}
			}
			if (adapters.isEmpty) {
				this.internalDoGenerate(obj, fsa)
			}
		}
	}

}
