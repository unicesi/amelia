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

import org.eclipse.xtext.generator.IOutputConfigurationProvider
import org.eclipse.xtext.generator.OutputConfiguration
import org.eclipse.xtext.generator.IFileSystemAccess
import java.util.Set

class AmeliaOutputConfigurationProvider implements IOutputConfigurationProvider {

	public static val AMELIA_OUTPUT = "amelia"
	public static val TARGET_SYSTEM_OUTPUT = "target"

	/**
	 * @return a set of {@link OutputConfiguration} available for the generator
	 */
	override Set<OutputConfiguration> getOutputConfigurations() {
		val defaultOutput = configure(IFileSystemAccess.DEFAULT_OUTPUT, "Output folder", "./src-gen")
		val pascaniOutput = configure(AMELIA_OUTPUT, "Output folder for Amelia elements", "./amelia-gen")
		val targetOutput = configure(TARGET_SYSTEM_OUTPUT, "Output folder for target-system components", "./src-gen-target")
		return newHashSet(defaultOutput, pascaniOutput, targetOutput)
	}
	
	def configure(String name, String description, String outputDirectory) {
		val outputConf = new OutputConfiguration(name)
		outputConf.setDescription(description)
		outputConf.setOutputDirectory(outputDirectory)
		outputConf.setOverrideExistingResources(true)
		outputConf.setCreateOutputDirectory(true)
		outputConf.setCleanUpDerivedResources(true)
		outputConf.setSetDerivedProperty(true)
		return outputConf
	}

}
