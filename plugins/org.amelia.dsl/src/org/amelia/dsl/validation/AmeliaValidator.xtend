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
package org.amelia.dsl.validation

import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.Set
import org.amelia.dsl.amelia.AmeliaPackage
import org.amelia.dsl.amelia.Model
import org.amelia.dsl.amelia.SequentialBlock
import org.amelia.dsl.amelia.Subsystem
import org.amelia.dsl.lib.descriptors.CommandDescriptor
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.validation.Check
import org.eclipse.xtext.xbase.XBlockExpression
import org.eclipse.xtext.xbase.XVariableDeclaration
import org.eclipse.xtext.xbase.XbasePackage

/**
 * This class contains custom validation rules. 
 *
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 * 
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaValidator extends AbstractAmeliaValidator {
	
	public static val CYCLIC_DEPENDENCY = "amelia.issue.cyclicDependency"
	public static val DUPLICATE_LOCAL_VARIABLE = "amelia.issue.duplicateLocalVariable"
	public static val INVALID_FILE_NAME = "amelia.issue.invalidName"
	public static val INVALID_PACKAGE_NAME =  "amelia.issue.invalidPackageName"
	public static val INVALID_PARAMETER_TYPE = "amelia.issue.invalidParameterType"
	public static val NON_CAPITAL_NAME = "amelia.issue.nonCapitalName"
	
	def fromURItoFQN(URI resourceURI) {
		// e.g., platform:/resource/<project>/<source-folder>/org/example/.../TypeDecl.pascani
		var segments = new ArrayList

		// Remove the first 3 segments, and return the package and file segments
		segments.addAll(resourceURI.segmentsList.subList(3, resourceURI.segments.size - 1))

		// Remove file extension and add the last segment
		segments.add(resourceURI.lastSegment.substring(0, resourceURI.lastSegment.lastIndexOf(".")))

		return segments.fold("", [r, t|if(r.isEmpty) t else r + "." + t])
	}

	@Check
	def checkSubsystemStartsWithCapital(Subsystem subsystem) {
		if (!Character.isUpperCase(subsystem.name.charAt(0))) {
			warning("Name should start with a capital", AmeliaPackage.Literals.SUBSYSTEM__NAME,
				NON_CAPITAL_NAME)
		}
	}

	@Check
	def checkPackageIsLowerCase(Model model) {
		if (!model.name.equals(model.name.toLowerCase)) {
			error("Package name must be in lower case", AmeliaPackage.Literals.MODEL__NAME)
		}
	}

	@Check
	def checkSubsystemNameMatchesPhysicalName(Subsystem subsystem) {
		// e.g., platform:/resource/<project>/<source-folder>/org/example/.../Subsystem.amelia
		val URI = subsystem.eResource.URI
		val fileName = URI.lastSegment.substring(0, URI.lastSegment.indexOf(URI.fileExtension) - 1)
		val isPublic = subsystem.eContainer != null && subsystem.eContainer instanceof Model

		if (isPublic && !fileName.equals(subsystem.name)) {
			error("Subsystem '" + subsystem.name + "' does not match the corresponding file name '" + fileName +
				"'", AmeliaPackage.Literals.SUBSYSTEM__NAME, INVALID_FILE_NAME)
		}
	}
	
	@Check
	def checkVariableNameIsUnique(XVariableDeclaration varDecl) {
		val parent = varDecl.eContainer.eContainer
		switch (parent) {
			Subsystem: {
				val duplicateVars = (parent.body as XBlockExpression).expressions.filter [ v |
					switch (v) {
						XVariableDeclaration case v.name.equals(varDecl.name):
							return !v.equals(varDecl)
						default:
							return false
					}
				]
				if (!duplicateVars.isEmpty) {
					error("Duplicate local variable " + varDecl.name, XbasePackage.Literals.XVARIABLE_DECLARATION__NAME,
						DUPLICATE_LOCAL_VARIABLE)
				}
			}
		}
	}
	
	@Check
	def checkPackageMatchesPhysicalDirectory(Model model) {
		val packageSegments = model.name.split("\\.")
		val fqn = fromURItoFQN(model.typeDeclaration.eResource.URI)
		var expectedPackage = fqn.substring(0, fqn.lastIndexOf("."))

		if (!Arrays.equals(expectedPackage.split("\\."), packageSegments)) {
			error("The declared package '" + model.name + "' does not match the expected package '" + expectedPackage +
				"'", AmeliaPackage.Literals.MODEL__NAME, INVALID_PACKAGE_NAME)
		}
	}
	
	@Check
	def void checkNoRecursiveDependencies(Subsystem subsystem) {
		subsystem.findDependentElements [ cycle |
			if (cycle.size == 1) {
				error('''The subsystem '«subsystem.name»' cannot depend on itself.''', 
					  cycle.head, AmeliaPackage.Literals.SUBSYSTEM__NAME, CYCLIC_DEPENDENCY)
			} else {
				error('''There is a cyclic dependency that involves subsystems «cycle.filter(Subsystem).map[name].join(", ")»''', 
					  cycle.head, AmeliaPackage.Literals.SUBSYSTEM__NAME, CYCLIC_DEPENDENCY)
			}
		]
	}
	
	@Check
	def void checkSequentialBlockExpressions(SequentialBlock block) {
		val commands = block.commands.filter[e|e.actualType.getSuperType(CommandDescriptor) != null]
		if (commands.length != block.commands.length)
			error("Sequential blocks can only contain command expressions",
				XbasePackage.Literals.XBLOCK_EXPRESSION__EXPRESSIONS, INVALID_PARAMETER_TYPE)
	}
	
//	@Check
//	def void checkDirectory(ChangeDirectory expr) {
//		val type = expr.directory.actualType
//		if (type.getSuperType(String) == null) {
//			error('''The directory must be of type String, «type.simpleName» was found instead''',
//				AmeliaPackage.Literals.CHANGE_DIRECTORY__DIRECTORY, INVALID_PARAMETER_TYPE)
//		}
//	}
	
	/**
	 * Adapted from 
	 * https://github.com/xtext/seven-languages-xtext/blob/master/languages/\
	 * org.xtext.builddsl/src/org/xtext/builddsl/validation/BuildDSLValidator.xtend
	 */
	def private Collection<EObject> findDependentElements(EObject it, (Set<EObject>)=>void cycleHandler) {
		// 1. collect all tasks that we depend on
		val elements = <EObject>newLinkedHashSet
		internalFindDependentTasksRec(it, elements)

		// 2. sort them so that dependents come after dependees 
		val result = <EObject>newLinkedHashSet
		var changed = true
		while (changed) {
			changed = false
			for (t : elements.toList) {
				val dependencies = if (t instanceof Subsystem) t.dependencies
				if (result.containsAll(dependencies)) {
					changed = true
					result.add(t)
					elements.remove(t)
				}
			}
		}
		if (!elements.empty && cycleHandler != null)
			cycleHandler.apply(elements)
		result
	}
	
	def private void internalFindDependentTasksRec(EObject e, Set<EObject> set) {
		if (!set.add(e))
			return;
		val dependencies = if (e instanceof Subsystem) e.dependencies
		for (t : dependencies) 
			internalFindDependentTasksRec(t, set)
	}
	
}
