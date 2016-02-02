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

import org.eclipse.xtext.generator.IFileSystemAccess
import org.eclipse.xtext.generator.IFileSystemAccessExtension
import org.eclipse.xtext.generator.IFileSystemAccessExtension2
import org.eclipse.emf.common.util.URI

class SingleOutputConfigurationFileSystemAccess implements IFileSystemAccess, IFileSystemAccessExtension, IFileSystemAccessExtension2 {

	protected IFileSystemAccess fsa
	protected String outputConfigurationName

	new(org.eclipse.xtext.generator.IFileSystemAccess fsa, String outputConfigurationName) {
		this.fsa = fsa
		this.outputConfigurationName = outputConfigurationName
	}

	override void generateFile(String fileName, CharSequence contents) {
		fsa.generateFile(fileName, outputConfigurationName, contents)
	}

	override void generateFile(String fileName, String outputConfiguration, CharSequence contents) {
		fsa.generateFile(fileName, outputConfigurationName, contents)
	}

	override void deleteFile(String fileName) {
		deleteFile(fileName, outputConfigurationName)
	}

	override void deleteFile(String fileName, String ignoredOutputConfigurationName) {
		(fsa as IFileSystemAccessExtension).deleteFile(fileName, outputConfigurationName)
	}

	override URI getURI(String fileName, String outputConfiguration) {
		return (fsa as IFileSystemAccessExtension2).getURI(fileName, outputConfigurationName)
	}

	override URI getURI(String fileName) {
		return (fsa as IFileSystemAccessExtension2).getURI(fileName, outputConfigurationName)
	}

}
