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
package org.amelia.dsl.validation

import com.google.inject.Inject
import java.net.URI
import java.util.Collection
import java.util.List
import java.util.Set
import org.amelia.dsl.amelia.AmeliaPackage
import org.amelia.dsl.amelia.CdCommand
import org.amelia.dsl.amelia.CompileCommand
import org.amelia.dsl.amelia.ConfigBlockExpression
import org.amelia.dsl.amelia.CustomCommand
import org.amelia.dsl.amelia.DependDeclaration
import org.amelia.dsl.amelia.DeploymentDeclaration
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
import org.amelia.dsl.amelia.TransferCommand
import org.amelia.dsl.amelia.TypeDeclaration
import org.amelia.dsl.amelia.VariableDeclaration
import org.amelia.dsl.lib.descriptors.Host
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.naming.IQualifiedNameProvider
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
import java.util.concurrent.atomic.AtomicBoolean
import com.google.common.base.Supplier

/**
 * This class contains custom validation rules. 
 *
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 * 
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaValidator extends AbstractAmeliaValidator {
	
	@Inject extension IQualifiedNameProvider
	
	public static val CONFIGURE_NOT_ALLOWED = "amelia.issue.configureNotAllowed"
	public static val CONFLICTING_PARAMETER = "amelia.issue.conflictingParam"
	public static val CYCLIC_DEPENDENCY = "amelia.issue.cyclicDependency"
	public static val DISCOURAGED_NAME_USAGE = "amelia.issue.discouragedNameUsage"
	public static val DUPLICATE_EXTENSION_DECLARATION = "amelia.issue.duplicateInclude"
	public static val DUPLICATE_LOCAL_RULE = "amelia.issue.duplicateLocalRule"
	public static val DUPLICATE_LOCAL_VARIABLE = "amelia.issue.duplicateLocalVariable"
	public static val EMPTY_COMMAND_LIST = "amelia.issue.emptyCommandList"
	public static val INVALID_EXTENSION_DECLARATION = "amelia.issue.invalidExtensionDeclaration"
	public static val INVALID_FILE_NAME = "amelia.issue.invalidName"
	public static val INVALID_PACKAGE_NAME =  "amelia.issue.invalidPackageName"
	public static val INVALID_PARAMETER_TYPE = "amelia.issue.invalidParameterType"
	public static val INVALID_SELF_EXTENSION = "amelia.issue.invalidSelfInclude"
	public static val MISSING_VARIABLE_TYPE = "amelia.issue.missingDataType"
	public static val NON_CAPITAL_NAME = "amelia.issue.nonCapitalName"
	public static val USING_PRIMITIVE_TYPE = "amelia.issue.usingPrimitiveType"
	public static val RESERVED_TYPE_NAME = "amelia.issue.reservedTypeName"
	
	@Check
	def checkUseOfJavaLangNames(TypeDeclaration typeDecl) {
		val ClassLoader classLoader = this.getClass().getClassLoader()
		try {
			val clazz = classLoader.loadClass("java.lang." + typeDecl.name);
			warning('''The use of type name «typeDecl.name» is discouraged because it can cause unexpected behavior with members from class «clazz.canonicalName»''',
				AmeliaPackage.Literals.TYPE_DECLARATION__NAME, DISCOURAGED_NAME_USAGE)
		} catch (ClassNotFoundException e) {
		}
	}

	@Check
	def checkSubsystemName(Subsystem subsystem) {
		val model = EcoreUtil2.getRootContainer(subsystem) as Model
		if (subsystem.name.equals("Amelia") && model.name === null) {
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
	def checkNameMatchesPhysicalName(TypeDeclaration declaration) {
		// e.g., platform:/resource/<project>/<source-folder>/org/example/.../Subsystem.amelia
		val URI = declaration.eResource.URI
		val fileName = URI.lastSegment.substring(0, URI.lastSegment.indexOf(URI.fileExtension) - 1)
		val isPublic = declaration.eContainer !== null && declaration.eContainer instanceof Model

		if (isPublic && !fileName.equals(declaration.name)) {
			var name = declaration.eClass.name
			if (name.equals("DeploymentDeclaration"))
				name = "Deployment"
			error('''«name» '«declaration.name»' does not match the corresponding file name '«fileName»' ''',
				AmeliaPackage.Literals.TYPE_DECLARATION__NAME, INVALID_FILE_NAME)
		}
	}
	
//	@Check
//	def checkPackageMatchesPhysicalDirectory(Model model) {
//		val packageSegments = model.name.split("\\.")
//		val fqn = ResourceUtils.fromURItoFQN(model.typeDeclaration.eResource.URI)
//		var expectedPackage = if(fqn.contains(".")) fqn.substring(0, fqn.lastIndexOf(".")) else ""
//
//		if (!Arrays.equals(expectedPackage.split("\\."), packageSegments)) {
//			error('''The declared package '«model.name»' does not match the expected package '«expectedPackage»' ''',
//				AmeliaPackage.Literals.MODEL__NAME, INVALID_PACKAGE_NAME)
//		}
//	}
	
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
	def checkNonPrimitiveTypes(VariableDeclaration e) {
		val primitives = #["int", "double", "float", "short", "byte", "boolean", "long", "char"]
		if (e.type !== null && primitives.contains(e.type.identifier)) {
			error("Primitive data types are not allowed", 
				AmeliaPackage.Literals.VARIABLE_DECLARATION__TYPE, USING_PRIMITIVE_TYPE)
		}
	}
	
	@Check
	def checkExplicitTypes(VariableDeclaration e) {
		if (e.type === null) {
			error("Missing data type", 
				AmeliaPackage.Literals.VARIABLE_DECLARATION__TYPE, MISSING_VARIABLE_TYPE)
		}
	}
	
	@Check
	def checkConflictingVarDecl(Subsystem subsystem) {
		if (subsystem.extensions !== null) {
			val includes = subsystem.extensions.declarations.filter(IncludeDeclaration)
			val includedSubsystems = includes.map[i| if(i.element instanceof Subsystem) i.element as Subsystem]
			val conflictingVarDcls = includedSubsystems
				.map[s|s.body.expressions.filter(VariableDeclaration)].flatten
				.groupBy[p|p.name].values.filter[l|l.size > 1]
			if (!conflictingVarDcls.empty) {
				var names = conflictingVarDcls.join("'", "', '", "'", [l|l.get(0).name])
				val index = names.lastIndexOf("', '")
				if (index > -1)
					names = names.substring(0, index + 1) + " and " + names.substring(index + 3)
				val d = if(conflictingVarDcls.size == 1) #["", "s", "Its"] else #["s", "", "Their"]
				info('''Variable«d.get(0)» «names» belong«d.get(1)» to several included subsystems. «d.get(2)» direct access has been hidden''',
					AmeliaPackage.Literals.TYPE_DECLARATION__NAME)
			}
		}
	}
	
	@Check
	def checkConflictingVarDecl(VariableDeclaration varDecl) {
		val type = if(varDecl.param) "parameter" else "variable"
		val typeDecl = (EcoreUtil2.getRootContainer(varDecl) as Model).typeDeclaration
		if (typeDecl instanceof Subsystem) {
			if (typeDecl.extensions !== null) {
				val includes = typeDecl.extensions.declarations.filter(IncludeDeclaration)
				val includedSubsystems = includes.filter[i|i.element instanceof Subsystem].map[i|i.element as Subsystem]
				val includedVarDecls = includedSubsystems
					.map[s|s.body.expressions.filter(VariableDeclaration)].flatten
				if (includedVarDecls.map[p|p.name].toList.contains(varDecl.name)) {
					val conflictingVarDecl = includedVarDecls.filter[p|p.name.equals(varDecl.name)]
					val subsystems = conflictingVarDecl.map[ p |
						((EcoreUtil2.getRootContainer(p) as Model).typeDeclaration) as Subsystem
					]
					val d = if(subsystems.size == 1) "" else "s"
					var list = subsystems.join("'", "', '", "'", [s|s.fullyQualifiedName.toString])
					val index = list.lastIndexOf("', '")
					if (index > -1)
						list = list.substring(0, index + 1) + " and " + list.substring(index + 3)
					info('''This «type» hides the direct access to parameter '«varDecl.name»' from the included subsystem«d» «list»''', 
						AmeliaPackage.Literals.VARIABLE_DECLARATION__NAME, CONFLICTING_PARAMETER)
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
	def void checkTransferCommand(TransferCommand expr) {
		if (expr.source.actualType.getSuperType(String) === null) {
			error('''The source parameter must be of type String, «expr.source.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.TRANSFER_COMMAND__SOURCE, INVALID_PARAMETER_TYPE)
		}
		val type = expr.destination.actualType
		val isOk = type.getSuperType(String) !== null || type.getSuperType(Iterable) !== null
		val msg = '''The destination parameter must be of type String or Iterable<String>, «type.simpleName» was found instead'''
		val showError = !isOk || type.getSuperType(List).typeArguments.length == 0 ||
			!type.getSuperType(Iterable).typeArguments.get(0).identifier.equals(String.canonicalName)
		if (showError) {
			error(msg, AmeliaPackage.Literals.TRANSFER_COMMAND__DESTINATION, INVALID_PARAMETER_TYPE)
		}
	}
	
	@Check
	def void checkRunCommand(RunCommand expr) {
		if (expr.composite.actualType.getSuperType(String) === null) {
			error('''The composite parameter must be of type String, «expr.port.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.RUN_COMMAND__COMPOSITE, INVALID_PARAMETER_TYPE)
		}
		if (expr.hasPort 
			&& expr.port.actualType.getSuperType(int) === null 
			&& expr.port.actualType.getSuperType(Integer) === null) {
			error('''The port parameter must be of type integer, «expr.port.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.RUN_COMMAND__PORT, INVALID_PARAMETER_TYPE)
		}
		if (expr.hasService && expr.service.actualType.getSuperType(String) === null) {
			error('''The service parameter must be of type String, «expr.service.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.RUN_COMMAND__SERVICE, INVALID_PARAMETER_TYPE)
		}
		if (expr.hasMethod && expr.method.actualType.getSuperType(String) === null) {
			error('''The method parameter must be of type String, «expr.method.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.RUN_COMMAND__METHOD, INVALID_PARAMETER_TYPE)
		}
		var type = expr.params.actualType
		var isOk = type.getSuperType(String) !== null || type.getSuperType(Iterable) !== null
		var msg = '''The arguments parameter must be of type String or Iterable<String>, «type.simpleName» was found instead'''
		var showError = !isOk || type.getSuperType(Iterable).typeArguments.length == 0 ||
			!type.getSuperType(Iterable).typeArguments.get(0).identifier.equals(String.canonicalName)
		if (showError) {
			error(msg, AmeliaPackage.Literals.RUN_COMMAND__PARAMS, INVALID_PARAMETER_TYPE)
		}
		
		type = expr.libpath.actualType
		isOk = type.getSuperType(String) !== null || type.getSuperType(Iterable) !== null
		msg = '''The libpath parameter must be of type String or Iterable<String>, «type.simpleName» was found instead'''
		showError = !isOk || type.getSuperType(Iterable).typeArguments.length == 0 ||
			!type.getSuperType(Iterable).typeArguments.get(0).identifier.equals(String.canonicalName)
		if (showError) {
			error(msg, AmeliaPackage.Literals.RUN_COMMAND__LIBPATH, INVALID_PARAMETER_TYPE)
		}
	}

	@Check
	def void checkCompileCommand(CompileCommand expr) {
		if (expr.source.actualType.getSuperType(String) === null) {
			error('''The source parameter must be of type String, «expr.source.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.COMPILE_COMMAND__SOURCE, INVALID_PARAMETER_TYPE)
		}
		if (expr.output.actualType.getSuperType(String) === null) {
			error('''The output parameter must be of type String, «expr.output.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.COMPILE_COMMAND__OUTPUT, INVALID_PARAMETER_TYPE)
		}
		var type = expr.classpath.actualType
		var isOk = type.getSuperType(String) !== null || type.getSuperType(Iterable) !== null
		var msg = '''The classpath parameter must be of type String or Iterable<String>, «type.simpleName» was found instead'''
		var showError = !isOk || type.getSuperType(Iterable).typeArguments.length == 0 ||
			!type.getSuperType(Iterable).typeArguments.get(0).identifier.equals(String.canonicalName)
		if (showError) {
			error(msg, AmeliaPackage.Literals.COMPILE_COMMAND__CLASSPATH, INVALID_PARAMETER_TYPE)
		}
	}
	
	@Check
	def void checkEvalCommand(EvalCommand expr) {
		if (expr.uri !== null && expr.uri.actualType.getSuperType(URI) === null) {
			error('''The binding URI must be of type URI, «expr.uri.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.EVAL_COMMAND__URI, INVALID_PARAMETER_TYPE)
		}
		
		if (expr.script !== null && expr.script.actualType.getSuperType(String) === null) {
			error('''The script must be of type String, «expr.script.actualType.simpleName» was found instead''',
				AmeliaPackage.Literals.EVAL_COMMAND__SCRIPT, INVALID_PARAMETER_TYPE)
		}
	}
	
	@Check
	def void checkHost(OnHostBlockExpression blockExpression) {
		val type = blockExpression.hosts.actualType
		val isOk = type.getSuperType(Host) !== null || type.getSuperType(Iterable) !== null
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
		if (command.value.actualType.getSuperType(String) === null) {
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
	def checkDeclExtensions(ExtensionDeclaration extensionDecl) {
		val typeDecl = (EcoreUtil2.getRootContainer(extensionDecl) as Model).typeDeclaration
		switch (typeDecl) {
			DeploymentDeclaration: {
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
			Subsystem: {
				switch (extensionDecl) {
					DependDeclaration case extensionDecl.element instanceof DeploymentDeclaration: {
						error("Subsystems cannot depend on deployment strategies", AmeliaPackage.Literals.EXTENSION_DECLARATION__ELEMENT,
							INVALID_EXTENSION_DECLARATION)
					}
				}
			}
		}
	}
	
	@Check
	def checkDeploymentIncludes(DeploymentDeclaration deployment) {
		var warn = true
		if (deployment.extensions !== null) {
			warn = deployment.extensions.declarations.filter(IncludeDeclaration).empty
		}
		if (warn)
			warning("Deployments should include at least one subsystem", AmeliaPackage.Literals.TYPE_DECLARATION__NAME)
	}
	
	@Check
	def checkCondition(OnHostBlockExpression onHostBlock) {
		val supplierType = onHostBlock.condition.actualType.getSuperType(Supplier)
		if (onHostBlock.condition.actualType.getSuperType(boolean) === null
			&& onHostBlock.condition.actualType.getSuperType(Boolean) === null
			&& onHostBlock.condition.actualType.getSuperType(AtomicBoolean) === null
			&& supplierType === null) {
			error(
				"Conditions must be either of type Boolean, Supplier<Boolean>, or AtomicBoolean",
				AmeliaPackage.Literals.ON_HOST_BLOCK_EXPRESSION__CONDITION,
				INVALID_PARAMETER_TYPE
			)
		} else if (supplierType !== null
			&& (supplierType.typeArguments.size != 1
				|| !supplierType.typeArguments.get(0).identifier.equals(Boolean.canonicalName))) {
			error(
				'''Incorrect type argument «supplierType.typeArguments.get(0).identifier». Boolean expected.''',
				AmeliaPackage.Literals.ON_HOST_BLOCK_EXPRESSION__CONDITION,
				INVALID_PARAMETER_TYPE
			)
		}
	}
	
	@Check
	def checkCondition(RuleDeclaration rule) {
		val supplierType = rule.condition.actualType.getSuperType(Supplier)
		if (rule.condition.actualType.getSuperType(boolean) === null
			&& rule.condition.actualType.getSuperType(Boolean) === null
			&& rule.condition.actualType.getSuperType(AtomicBoolean) === null
			&& supplierType === null) {
			error(
				"Conditions must be either of type Boolean, Supplier<Boolean>, or AtomicBoolean",
				AmeliaPackage.Literals.RULE_DECLARATION__CONDITION,	
				INVALID_PARAMETER_TYPE
			)
		} else if (supplierType !== null
			&& (supplierType.typeArguments.size != 1
				|| !supplierType.typeArguments.get(0).identifier.equals(Boolean.canonicalName))) {
			error(
				'''Incorrect type argument «supplierType.typeArguments.get(0).identifier». Boolean expected.''',
				AmeliaPackage.Literals.RULE_DECLARATION__CONDITION,
				INVALID_PARAMETER_TYPE
			)
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
		if (!elements.empty && cycleHandler !== null)
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
