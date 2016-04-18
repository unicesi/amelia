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

import java.net.URI
import java.util.Arrays
import java.util.Collection
import java.util.List
import java.util.Set
import org.amelia.dsl.amelia.AmeliaPackage
import org.amelia.dsl.amelia.CdCommand
import org.amelia.dsl.amelia.CompileCommand
import org.amelia.dsl.amelia.ConfigBlockExpression
import org.amelia.dsl.amelia.CustomCommand
import org.amelia.dsl.amelia.DependDeclaration
import org.amelia.dsl.amelia.EvalCommand
import org.amelia.dsl.amelia.ExtensionDeclaration
import org.amelia.dsl.amelia.ExtensionSection
import org.amelia.dsl.amelia.IncludeDeclaration
import org.amelia.dsl.amelia.Model
import org.amelia.dsl.amelia.OnHostBlockExpression
import org.amelia.dsl.amelia.RichString
import org.amelia.dsl.amelia.RuleDeclaration
import org.amelia.dsl.amelia.RunCommand
import org.amelia.dsl.amelia.Subsystem
import org.amelia.dsl.amelia.SubsystemBlockExpression
import org.amelia.dsl.amelia.VariableDeclaration
import org.amelia.dsl.jvmmodel.ResourceUtils
import org.amelia.dsl.lib.descriptors.Host
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.validation.Check
import org.eclipse.xtext.xbase.XAbstractFeatureCall
import org.eclipse.xtext.xbase.XBooleanLiteral
import org.eclipse.xtext.xbase.XClosure
import org.eclipse.xtext.xbase.XCollectionLiteral
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XNullLiteral
import org.eclipse.xtext.xbase.XNumberLiteral
import org.eclipse.xtext.xbase.XStringLiteral
import org.eclipse.xtext.xbase.XTypeLiteral
import org.eclipse.xtext.xbase.XbasePackage
import org.amelia.dsl.amelia.DeploymentDeclaration

/**
 * This class contains custom validation rules. 
 *
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 * 
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaValidator extends AbstractAmeliaValidator {
	
	public static val CONFIGURE_NOT_ALLOWED = "amelia.issue.configureNotAllowed"
	public static val CYCLIC_DEPENDENCY = "amelia.issue.cyclicDependency"
	public static val DUPLICATE_EXTENSION_DECLARATION = "amelia.issue.duplicateInclude"
	public static val DUPLICATE_LOCAL_RULE = "amelia.issue.duplicateLocalRule"
	public static val DUPLICATE_LOCAL_VARIABLE = "amelia.issue.duplicateLocalVariable"
	public static val EMPTY_COMMAND_LIST = "amelia.issue.emptyCommandList"
	public static val INVALID_EXTENSION_DECLARATION = "amelia.issue.invalidExtensionDeclaration"
	public static val INVALID_FILE_NAME = "amelia.issue.invalidName"
	public static val INVALID_PACKAGE_NAME =  "amelia.issue.invalidPackageName"
	public static val INVALID_PARAM_DECLARATION = "amelia.issue.invalidParamDeclaration"
	public static val INVALID_PARAMETER_TYPE = "amelia.issue.invalidParameterType"
	public static val INVALID_SELF_EXTENSION = "amelia.issue.invalidSelfInclude"
	public static val NON_CAPITAL_NAME = "amelia.issue.nonCapitalName"
	public static val RESERVED_TYPE_NAME = "amelia.issue.reservedTypeName"

	@Check
	def checkSubsystemName(Subsystem subsystem) {
		val model = EcoreUtil2.getRootContainer(subsystem) as Model
		if (subsystem.name.equals("Amelia") && model.name == null) {
			error("The fully qualified name 'Amelia' is reserved", AmeliaPackage.Literals.TYPE_DECLARATION__NAME,
				RESERVED_TYPE_NAME)
		}
		if (!Character.isUpperCase(subsystem.name.charAt(0))) {
			warning("Name should start with a capital", AmeliaPackage.Literals.TYPE_DECLARATION__NAME,
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
				"'", AmeliaPackage.Literals.TYPE_DECLARATION__NAME, INVALID_FILE_NAME)
		}
	}
	
	@Check
	def checkVariableNameIsUnique(VariableDeclaration varDecl) {
		val parent = varDecl.eContainer.eContainer
		switch (parent) {
			Subsystem: {
				val duplicateVars = (parent.body as SubsystemBlockExpression).expressions.filter [ v |
					switch (v) {
						VariableDeclaration case v.name.equals(varDecl.name):
							return !v.equals(varDecl)
						RuleDeclaration case v.name.equals(varDecl.name):
							return true
						default:
							return false
					}
				]
				if (!duplicateVars.isEmpty) {
					error("Duplicate local variable " + varDecl.name, AmeliaPackage.Literals.VARIABLE_DECLARATION__NAME,
						DUPLICATE_LOCAL_VARIABLE)
				}
			}
		}
	}
	
	@Check
	def checkRuleNameIsUnique(RuleDeclaration rule) {
		val subsystem = (EcoreUtil2.getRootContainer(rule) as Model).typeDeclaration as Subsystem
		val duplicates = subsystem.body.expressions.filter(OnHostBlockExpression).map[o|o.rules].flatten.filter [ r |
			r.name.equals(rule.name) && !rule.equals(r)
		] + subsystem.body.expressions.filter(VariableDeclaration).filter [ v |
			v.name.equals(rule.name)
		]
		if (!duplicates.isEmpty) {
			error("Duplicate local rule " + rule.name, AmeliaPackage.Literals.RULE_DECLARATION__NAME,
				DUPLICATE_LOCAL_RULE)
		}
	}
	
	@Check
	def checkPackageMatchesPhysicalDirectory(Model model) {
		val packageSegments = model.name.split("\\.")
		val fqn = ResourceUtils.fromURItoFQN(model.typeDeclaration.eResource.URI)
		var expectedPackage = if(fqn.contains(".")) fqn.substring(0, fqn.lastIndexOf(".")) else ""

		if (!Arrays.equals(expectedPackage.split("\\."), packageSegments)) {
			error("The declared package '" + model.name + "' does not match the expected package '" + expectedPackage +
				"'", AmeliaPackage.Literals.MODEL__NAME, INVALID_PACKAGE_NAME)
		}
	}
	
	@Check
	def void checkCdCommand(CdCommand expr) {
		val allowed = #[XAbstractFeatureCall, XStringLiteral, RichString]
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
		if (expr.uri != null && expr.uri.actualType.getSuperType(URI) == null) {
			error('''The binding URI must be of type URI, «expr.uri.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.EVAL_COMMAND__URI, INVALID_PARAMETER_TYPE)
		}
		
		if (expr.script != null && expr.script.actualType.getSuperType(String) == null) {
			error('''The script must be of type String, «expr.script.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.EVAL_COMMAND__SCRIPT, INVALID_PARAMETER_TYPE)
		}
	}
	
	@Check
	def void checkHost(OnHostBlockExpression blockExpression) {
		val type = blockExpression.hosts.actualType
		val isOk = type.getSuperType(Host) != null || type.getSuperType(Iterable) != null
		val msg = '''The hosts parameter must be of type «Host.simpleName» or Iterable<«Host.simpleName»>, «type.simpleName» was found instead'''
		val showError = !isOk || type.getSuperType(List).typeArguments.length == 0 ||
			!type.getSuperType(Iterable).typeArguments.get(0).identifier.equals(Host.canonicalName)
		if (showError) {
			error(msg, AmeliaPackage.Literals.ON_HOST_BLOCK_EXPRESSION__HOSTS, INVALID_PARAMETER_TYPE)
		}
	}
	
	@Check
	def void checkInterpolatedExpression(RichString literal) {
		for (part : literal.expressions) {
			if (part instanceof XExpression) {
				val allowed = #[XAbstractFeatureCall, XCollectionLiteral, XClosure, XBooleanLiteral, XNumberLiteral,
					XNullLiteral, XStringLiteral, XTypeLiteral]
				if (!allowed.map[type|type.isInstance(part)].exists[v|v]) {
					error("This expression is not allowed in this context", AmeliaPackage.Literals.RICH_STRING_LITERAL__VALUE,
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
					  cycle.head, AmeliaPackage.Literals.TYPE_DECLARATION__NAME, CYCLIC_DEPENDENCY)
			} else {
				error('''There is a cyclic dependency that involves subsystems «cycle.filter(Subsystem).map[name].join(", ")»''', 
					  cycle.head, AmeliaPackage.Literals.TYPE_DECLARATION__NAME, CYCLIC_DEPENDENCY)
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
	
	@Check
	def checkIncludesOrDependsNotBoth(ExtensionDeclaration extensionDeclaration) {
		val extendSection = extensionDeclaration.eContainer as ExtensionSection
		val duplicates = extendSection.declarations
			.filter [i|i.element.equals(extensionDeclaration.element)]
			.filter[i|!i.equals(extensionDeclaration)]
		val extensionType = if (extensionDeclaration instanceof IncludeDeclaration)
				"include"
			else if (extensionDeclaration instanceof DependDeclaration)
				"depend"
		for (declaration : duplicates) {
			if (declaration.class.equals(extensionDeclaration.class)) {
				error('''Duplicate «extensionType» statement''', AmeliaPackage.Literals.EXTENSION_DECLARATION__ELEMENT,
					DUPLICATE_EXTENSION_DECLARATION)
			} else {
				error("A subsystem cannot depend on a subsystem that is also included",
					AmeliaPackage.Literals.EXTENSION_DECLARATION__ELEMENT, INVALID_EXTENSION_DECLARATION)
			}
		}
	}
	
	@Check
	def checkSelfIncludes(ExtensionDeclaration extensionDeclaration) {
		val typeDecl = (EcoreUtil2.getRootContainer(extensionDeclaration) as Model).typeDeclaration
		if (typeDecl instanceof Subsystem) {
			val subsystem = typeDecl as Subsystem
			if (subsystem.equals(extensionDeclaration.element)) {
				val relation = if (extensionDeclaration instanceof IncludeDeclaration)
						"include"
					else if (extensionDeclaration instanceof DependDeclaration)
						"depend on"
				error('''A subsystem cannot «relation» itself''',
					AmeliaPackage.Literals.EXTENSION_DECLARATION__ELEMENT, INVALID_SELF_EXTENSION)
			}	
		}
	}
	
	@Check
	def checkNoEmptyRules(RuleDeclaration rule) {
		if (rule.commands.empty) {
			error("This rule must contain at least one command", AmeliaPackage.Literals.RULE_DECLARATION__COMMANDS,
				EMPTY_COMMAND_LIST)
		}
	}
	
	@Check
	def checkInferrableType(VariableDeclaration declaration) {
		if (declaration.param && declaration.type == null && declaration.right == null) {
			error("This parameter must have either an explicit type or an initial value",
				AmeliaPackage.Literals.VARIABLE_DECLARATION__NAME, INVALID_PARAM_DECLARATION)
		}
	}
	
	@Check
	def checkConfigureBlock(ConfigBlockExpression configBlock) {
		val subsystem = (EcoreUtil2.getRootContainer(configBlock) as Model).typeDeclaration as Subsystem
		if (!subsystem.body.expressions.filter(ConfigBlockExpression).filter[c|!c.equals(configBlock)].empty) {
			error("Subsystems can only contain one configuration block",
				XbasePackage.Literals.XBLOCK_EXPRESSION__EXPRESSIONS, CONFIGURE_NOT_ALLOWED)
		}
		if (configBlock.expressions.empty) {
			warning("An empty execution configuration does not execute any of the rules in this subsystem",
				XbasePackage.Literals.XBLOCK_EXPRESSION__EXPRESSIONS)
		}
	}
	
	@Check
	def checkDeploymentDeclExtensions(ExtensionDeclaration extensionDecl) {
		val typeDecl = (EcoreUtil2.getRootContainer(extensionDecl) as Model).typeDeclaration
		if (typeDecl instanceof DeploymentDeclaration) {
			switch (extensionDecl) {
				IncludeDeclaration case extensionDecl.element instanceof DeploymentDeclaration: {
					error("Deployments can only include subsystems", AmeliaPackage.Literals.EXTENSION_DECLARATION__ELEMENT,
						INVALID_EXTENSION_DECLARATION)
				}
				DependDeclaration: {
					error("Deployments cannot have dependencies", AmeliaPackage.Literals.EXTENSION_DECLARATION__ELEMENT,
						INVALID_EXTENSION_DECLARATION)
				}
			}	
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
						t.extensions.declarations.map[d|d.element]
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
				e.extensions.declarations.map[d|d.element]
			else if (e instanceof RuleDeclaration)
				e.dependencies
		for (t : dependencies) 
			internalFindDependentTasksRec(t, set)
	}
	
}
