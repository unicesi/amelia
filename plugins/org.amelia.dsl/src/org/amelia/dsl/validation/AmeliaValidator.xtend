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
import java.util.List
import java.util.Set
import org.amelia.dsl.amelia.AmeliaPackage
import org.amelia.dsl.amelia.CdCommand
import org.amelia.dsl.amelia.CompileCommand
import org.amelia.dsl.amelia.CustomCommand
import org.amelia.dsl.amelia.EvalCommand
import org.amelia.dsl.amelia.IncludeDeclaration
import org.amelia.dsl.amelia.IncludeSection
import org.amelia.dsl.amelia.Model
import org.amelia.dsl.amelia.OnHostBlockExpression
import org.amelia.dsl.amelia.RuleDeclaration
import org.amelia.dsl.amelia.RunCommand
import org.amelia.dsl.amelia.StringLiteral
import org.amelia.dsl.amelia.Subsystem
import org.amelia.dsl.lib.descriptors.Host
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.validation.Check
import org.eclipse.xtext.xbase.XAbstractFeatureCall
import org.eclipse.xtext.xbase.XBlockExpression
import org.eclipse.xtext.xbase.XBooleanLiteral
import org.eclipse.xtext.xbase.XClosure
import org.eclipse.xtext.xbase.XCollectionLiteral
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XNullLiteral
import org.eclipse.xtext.xbase.XNumberLiteral
import org.eclipse.xtext.xbase.XStringLiteral
import org.eclipse.xtext.xbase.XTypeLiteral
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
	public static val INVALID_SELF_INCLUDE = "amelia.issue.invalidSelfInclude"
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
	def void checkCdCommand(CdCommand expr) {
		val allowed = #[XAbstractFeatureCall, XStringLiteral, StringLiteral]
		if (!allowed.map[type|type.isInstance(expr.directory)].exists[v|v]) {
			error("This expression is not allowed in this context", AmeliaPackage.Literals.CD_COMMAND__DIRECTORY,
				INVALID_PARAMETER_TYPE)
		} else if (expr.directory.actualType.getSuperType(String) ==
			null) {
			error('''The directory parameter must be of type String, «expr.directory.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.CD_COMMAND__DIRECTORY, INVALID_PARAMETER_TYPE)
		}
	}
	
	@Check
	def void checkRunCommand(RunCommand expr) {
		if (expr.composite.actualType.getSuperType(String) == null) {
			error('''The composite parameter must be of type String, «expr.port.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.RUN_COMMAND__COMPOSITE, INVALID_PARAMETER_TYPE)
		}
		if (expr.hasPort 
			&& expr.port.actualType.getSuperType(int) == null 
			&& expr.port.actualType.getSuperType(Integer) == null
		) {
			error('''The port parameter must be of type integer, «expr.port.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.RUN_COMMAND__PORT, INVALID_PARAMETER_TYPE)
		}
		if (expr.hasService && expr.service.actualType.getSuperType(String) == null) {
			error('''The service parameter must be of type String, «expr.service.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.RUN_COMMAND__SERVICE, INVALID_PARAMETER_TYPE)
		}
		if (expr.hasMethod && expr.method.actualType.getSuperType(String) == null) {
			error('''The method parameter must be of type String, «expr.method.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.RUN_COMMAND__METHOD, INVALID_PARAMETER_TYPE)
		}
		val paramsType = expr.params.actualType
		if (expr.hasParams
			&& paramsType.getSuperType(typeof(String)) == null
			&& paramsType.getSuperType(typeof(String[])) == null
			&& paramsType.getSuperType(List) == null
		) {
			error('''The method parameter must be of type String or String[], «paramsType.simpleName» was found instead''',
				AmeliaPackage.Literals.RUN_COMMAND__PARAMS, INVALID_PARAMETER_TYPE)
		} else if (paramsType.getSuperType(List) != null) {
			var showError = false
			if (paramsType.getSuperType(List).typeArguments.length == 1) {
				if (!paramsType.getSuperType(List).typeArguments.get(0).identifier.equals(String.canonicalName))
					showError = true
			} else
				showError = true

			if (showError)
				error('''The classpath must be of type List<String>, «paramsType.simpleName» was found instead''',
					AmeliaPackage.Literals.RUN_COMMAND__PARAMS, INVALID_PARAMETER_TYPE)
		}
		val libpathType = expr.libpath.actualType
		if (libpathType.getSuperType(typeof(String)) == null 
			&& libpathType.getSuperType(typeof(String[])) == null 
			&& libpathType.getSuperType(List) == null
		) {
			error('''The libpath parameter must be of type String or String[], «libpathType.simpleName» was found instead''',
				AmeliaPackage.Literals.RUN_COMMAND__LIBPATH, INVALID_PARAMETER_TYPE)
		} else if (libpathType.getSuperType(List) != null) {
			var showError = false
			if (libpathType.getSuperType(List).typeArguments.length == 1) {
				if (!libpathType.getSuperType(List).typeArguments.get(0).identifier.equals(String.canonicalName))
					showError = true
			} else
				showError = true

			if (showError)
				error('''The libpath must be of type List<String>, «libpathType.simpleName» was found instead''',
					AmeliaPackage.Literals.RUN_COMMAND__LIBPATH, INVALID_PARAMETER_TYPE)
		}
	}

	@Check
	def void checkCompileCommand(CompileCommand expr) {
		if (expr.source.actualType.getSuperType(String) == null) {
			error('''The source parameter must be of type String, «expr.source.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.COMPILE_COMMAND__SOURCE, INVALID_PARAMETER_TYPE)
		}
		if (expr.output.actualType.getSuperType(String) == null) {
			error('''The output parameter must be of type String, «expr.output.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.COMPILE_COMMAND__OUTPUT, INVALID_PARAMETER_TYPE)
		}
		val classpathType = expr.classpath.actualType
		if (classpathType.getSuperType(typeof(String)) == null 
			&& classpathType.getSuperType(typeof(String[])) == null 
			&& classpathType.getSuperType(List) == null
		) {
			error('''The classpath must be of type String or String[], «classpathType.simpleName» was found instead''',
				AmeliaPackage.Literals.COMPILE_COMMAND__CLASSPATH, INVALID_PARAMETER_TYPE)
		} else if (classpathType.getSuperType(List) != null) {
			var showError = false
			if (classpathType.getSuperType(List).typeArguments.length == 1) {
				if (!classpathType.getSuperType(List).typeArguments.get(0).identifier.equals(String.canonicalName))
					showError = true
			} else {
				showError = true
			}

			if (showError)
				error('''The classpath must be of type List<String>, «classpathType.simpleName» was found instead''',
					AmeliaPackage.Literals.COMPILE_COMMAND__CLASSPATH, INVALID_PARAMETER_TYPE)
		}
	}
	
	@Check
	def void checkEvalCommand(EvalCommand expr) {
		if (expr.uri != null && expr.uri.actualType.getSuperType(java.net.URI) == null) {
			error('''The binding URI must be of type URI, «expr.uri.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.EVAL_COMMAND__URI, INVALID_PARAMETER_TYPE)
		}
		
		if (expr.script != null && expr.script.actualType.getSuperType(String) == null) {
			error('''The script must be of type String, «expr.script.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.EVAL_COMMAND__SCRIPT, INVALID_PARAMETER_TYPE)
		}
	}
	
	@Check
	def void checkHost(OnHostBlockExpression declaration) {
		val types = declaration.hosts.map[h|h.actualType.getSuperType(Host)]
		if (types.exists[t|t == null]) {
			error('''Hosts must be of type «Host.simpleName»''', AmeliaPackage.Literals.ON_HOST_BLOCK_EXPRESSION__HOSTS,
				INVALID_PARAMETER_TYPE)
		}
	}
	
	@Check
	def void checkInterpolatedExpression(StringLiteral literal) {
		for (part : literal.value.expressions) {
			if (part instanceof XExpression) {
				val allowed = #[XAbstractFeatureCall, XCollectionLiteral, XClosure, XBooleanLiteral, XNumberLiteral,
					XNullLiteral, XStringLiteral, XTypeLiteral]
				if (!allowed.map[type|type.isInstance(part)].exists[v|v]) {
					error("This expression is not allowed in this context", AmeliaPackage.Literals.STRING_LITERAL__VALUE,
						INVALID_PARAMETER_TYPE)
				}
			}
		}
	}
	
	@Check
	def void checkCustomCommand(CustomCommand command) {
		if (command.value.actualType.getSuperType(String) == null) {
			error("The command expression must be of type String", AmeliaPackage.Literals.CUSTOM_COMMAND__VALUE,
				INVALID_PARAMETER_TYPE)
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
	def void checkNoRecursiveDependencies(RuleDeclaration rule) {
		rule.findDependentElements [ cycle |
			if (cycle.size == 1) {
				error('''The rule '«rule.name»' cannot depend on itself.''', 
					  cycle.head, AmeliaPackage.Literals.RULE_DECLARATION__NAME, CYCLIC_DEPENDENCY)
			} else {
				error('''There is a cyclic dependency that involves rules «cycle.filter(RuleDeclaration).map[name].join(", ")»''', 
					  cycle.head, AmeliaPackage.Literals.RULE_DECLARATION__NAME, CYCLIC_DEPENDENCY)
			}
		]
	}
	
	/**
	 * Check rules inside an include declaration and its siblings
	 */
	@Check
	def checkEventInImportDeclaration(IncludeDeclaration includeDeclaration) {
		// Rule name is unique
		val includeSection = includeDeclaration.eContainer as IncludeSection
		val rules = includeSection.includeDeclarations.toList.map[d|d.rules].flatten
		for (rule : includeDeclaration.rules) {
			val count = rules.filter[r|r.name.equals(rule.name)].size
			if (count > 1) {
				error("Duplicate local variable " + rule.name, AmeliaPackage.Literals.INCLUDE_DECLARATION__RULES,
					DUPLICATE_LOCAL_VARIABLE)
			}
		}
	}
	
	@Check
	def checkIncludeIsExternal(IncludeDeclaration includeDeclaration) {
		val subsystem = (EcoreUtil2.getRootContainer(includeDeclaration) as Model).typeDeclaration as Subsystem
		if (subsystem.equals(includeDeclaration.includedType)) {
			error("A subsystem cannot include rules from itself",
				AmeliaPackage.Literals.INCLUDE_DECLARATION__INCLUDED_TYPE, INVALID_SELF_INCLUDE)
		}
	}
	
	/**
	 * Adapted from 
	 * https://github.com/xtext/seven-languages-xtext/blob/master/languages/\
	 * org.xtext.builddsl/src/org/xtext/builddsl/validation/BuildDSLValidator.xtend
	 */
	def private Collection<EObject> findDependentElements(EObject it, (Set<EObject>) => void cycleHandler) {
		// 1. collect all tasks that we depend on
		val elements = <EObject>newLinkedHashSet
		internalFindDependentTasksRec(it, elements)

		// 2. sort them so that dependents come after dependees 
		val result = <EObject>newLinkedHashSet
		var changed = true
		while (changed) {
			changed = false
			for (t : elements.toList) {
				val dependencies = if (t instanceof Subsystem)
						t.includes.includeDeclarations.map[d|d.includedType]
					else if (t instanceof RuleDeclaration)
						t.dependencies
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
		val dependencies = if (e instanceof Subsystem)
				e.includes.includeDeclarations.map[d|d.includedType]
			else if (e instanceof RuleDeclaration)
				e.dependencies
		for (t : dependencies) 
			internalFindDependentTasksRec(t, set)
	}
	
}
