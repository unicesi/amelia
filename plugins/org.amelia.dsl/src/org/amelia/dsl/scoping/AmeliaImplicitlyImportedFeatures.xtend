package org.amelia.dsl.scoping

import org.eclipse.xtext.xbase.scoping.batch.ImplicitlyImportedFeatures
import org.amelia.dsl.lib.util.CommandExtensions

class AmeliaImplicitlyImportedFeatures extends ImplicitlyImportedFeatures {
	
	override protected getStaticImportClasses() {
		return super.getStaticImportClasses()
	}
	
	override protected getExtensionClasses() {
		return (super.getExtensionClasses() + #[CommandExtensions]).toList
	}
	
}
