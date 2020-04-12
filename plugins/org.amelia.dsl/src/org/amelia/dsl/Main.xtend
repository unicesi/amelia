/*
 * Copyright © 2017 Universidad Icesi
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
package org.amelia.dsl

import com.google.common.collect.Lists
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.xtext.util.CancelIndicator
import org.eclipse.xtext.validation.CheckMode
import org.eclipse.xtext.validation.IResourceValidator
import org.eclipse.xtext.validation.Issue
import org.eclipse.xtext.generator.GeneratorDelegate
import org.eclipse.xtext.generator.JavaIoFileSystemAccess
import org.eclipse.xtext.generator.GeneratorContext
import org.eclipse.xtext.diagnostics.Severity

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class Main {

	// https://wiki.eclipse.org/Xtext/FAQ#How_do_I_load_my_model_in_a_standalone_Java_application.C2.A0.3F
	// https://www.eclipse.org/forums/index.php?t=msg&th=327704&goto=1746034&#msg_1746034
	def static void main(String[] args) {
		val files = Lists.newArrayList(args);
		val injector = new AmeliaStandaloneSetup().createInjectorAndDoEMFRegistration();
		val rs = injector.getInstance(ResourceSet);
		val resources = Lists.newArrayList();
		for (String file : files) {
			val r = rs.getResource(URI.createFileURI(file), true);
			resources.add(r);
		}
		val validator = injector.getInstance(IResourceValidator);
		var stop = false
		for (Resource r : resources) {
			val issues = validator.validate(r, CheckMode.ALL, CancelIndicator.NullImpl);
			stop = stop || !issues.filter[it.severity.equals(Severity.ERROR)].empty
			for (Issue i : issues) {
				System.err.println(i);
			}
		}
		if (stop)
			System.exit(1);
		val generator = injector.getInstance(GeneratorDelegate);
		val fsa = injector.getInstance(JavaIoFileSystemAccess);
		fsa.setOutputPath("output/");
		val context = new GeneratorContext();
		context.setCancelIndicator(CancelIndicator.NullImpl);
		for (Resource r : resources) {
			generator.generate(r, fsa, context);
		}
	}

}
