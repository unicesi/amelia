package org.amelia.dsl.scoping

import org.eclipse.xtext.xbase.scoping.XImportSectionNamespaceScopeProvider
import org.eclipse.xtext.naming.IQualifiedNameProvider
import com.google.inject.Inject
import java.util.Collections
import org.eclipse.xtext.scoping.impl.ImportNormalizer
import org.eclipse.xtext.xbase.XConstructorCall
import org.amelia.dsl.amelia.Model
import org.eclipse.emf.ecore.EObject
import java.util.List
import com.google.common.collect.Lists
import org.amelia.dsl.amelia.ExtensionSection

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
				if (container.extensions !== null)
					container.extensions.provideImportNormalizerList(ignoreCase)
				else
					Collections.emptyList
			}
			default:
				super.internalGetImportedNamespaceResolvers(context, ignoreCase)
		}
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
        extensionSection.declarations.forEach[includeDecl|
        	val fqn = includeDecl.element.fullyQualifiedName
	        if (fqn !== null)
	        	result.add(fqn.toString.createImportedNamespaceResolver(ignoreCase))
        ]
        result
    }

}