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
package org.amelia.dsl

import com.google.inject.Binder
import com.google.inject.name.Names
import org.amelia.dsl.scoping.AmeliaScopeProvider
import org.eclipse.xtext.linking.LinkingScopeProviderBinding
import org.eclipse.xtext.scoping.IScopeProvider
import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider
import org.eclipse.xtext.xbase.scoping.batch.ImplicitlyImportedFeatures
import org.amelia.dsl.scoping.AmeliaImplicitlyImportedFeatures
import org.amelia.dsl.compiler.AmeliaCompiler
import org.eclipse.xtext.xbase.compiler.XbaseCompiler
import org.amelia.dsl.typesystem.AmeliaTypeComputer
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputer

/**
 * Use this class to register components to be used at runtime / without the Equinox extension registry.
 */
class AmeliaRuntimeModule extends AbstractAmeliaRuntimeModule {
	
	override void configureIScopeProviderDelegate(Binder binder) {
		binder
			.bind(IScopeProvider)
			.annotatedWith(Names.named(AbstractDeclarativeScopeProvider.NAMED_DELEGATE))
			.to(AmeliaScopeProvider)
	}
	
	override void configureLinkingIScopeProvider(Binder binder) {
		binder
			.bind(IScopeProvider)
			.annotatedWith(LinkingScopeProviderBinding)
			.to(AmeliaScopeProvider);
	}
	
	override Class<? extends IScopeProvider> bindIScopeProvider() {
		return AmeliaScopeProvider
	}
	
	override void configure(Binder binder) {
		super.configure(binder)
		binder
			.bind(ImplicitlyImportedFeatures)
			.to(AmeliaImplicitlyImportedFeatures)
	}
	
	def Class<? extends ITypeComputer> bindITypeComputer() {
		return AmeliaTypeComputer
	}

	def Class<? extends XbaseCompiler> bindXbaseCompiler() {
		return AmeliaCompiler
	}
	
}
