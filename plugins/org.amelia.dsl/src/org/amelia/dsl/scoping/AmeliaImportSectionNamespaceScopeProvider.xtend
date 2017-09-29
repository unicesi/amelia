package org.amelia.dsl.scoping

import com.google.common.collect.Lists
import com.google.inject.Inject
import java.util.List
import org.amelia.dsl.amelia.ExtensionSection
import org.amelia.dsl.amelia.Model
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.scoping.impl.ImportNormalizer
import org.eclipse.xtext.xbase.XConstructorCall
import org.eclipse.xtext.xbase.scoping.XImportSectionNamespaceScopeProvider
import org.amelia.dsl.amelia.IncludeDeclaration
import org.amelia.dsl.amelia.DeploymentDeclaration

/**
 * See more at https://www.eclipse.org/forums/index.php/m/1771411/
 * With a little help from https://stackoverflow.com/a/28027025/738968
 */
class AmeliaImportSectionNamespaceScopeProvider extends XImportSectionNamespaceScopeProvider {

	@Inject extension IQualifiedNameProvider

	override List<ImportNormalizer> internalGetImportedNamespaceResolvers(EObject context, boolean ignoreCase) {
		switch(context) {
			// If changed to XExpression the class can be used as any imported class (not only for constructor calls)
			XConstructorCall: {
				val container = context.model as Model
				// Auto-import generated classes if the container is a deployment,
				// and it includes at least one subsystem
				if (container.extensions !== null
					&& container.typeDeclaration instanceof DeploymentDeclaration) {
					return container.extensions.provideImportNormalizerList(ignoreCase)
				}
			}
		}
		return super.internalGetImportedNamespaceResolvers(context, ignoreCase)
	}

	/*
	 * Iterates upwards through the AST until a DomainModel is found.
	 */
	def EObject model(EObject o) {
        switch (o) {
            Model: o
            default: o.eContainer.model
        }
    }

	/*
	 * Creates the list of all imports of an ExtensionSection. This implementation is similar to 
	 * getImportedNamespaceResolvers(XImportSection, boolean)
	 */
	def List<ImportNormalizer> provideImportNormalizerList(ExtensionSection extensionSection, boolean ignoreCase) {
        val List<ImportNormalizer> result = Lists.newArrayList
        extensionSection.declarations.filter(IncludeDeclaration).forEach [ includeDecl |
			val fqn = includeDecl.element.fullyQualifiedName
			if (fqn !== null)
				result.add(fqn.toString.createImportedNamespaceResolver(ignoreCase))
		]
        result
    }

}