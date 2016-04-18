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
import com.google.inject.Singleton
import org.amelia.dsl.compiler.AmeliaCompiler
import org.amelia.dsl.outputconfiguration.AmeliaOutputConfigurationProvider
import org.amelia.dsl.outputconfiguration.OutputConfigurationAwaredGenerator
import org.amelia.dsl.runtime.AmeliaQualifiedNameProvider
import org.amelia.dsl.scoping.AmeliaImplicitlyImportedFeatures
import org.amelia.dsl.scoping.AmeliaScopeProvider
import org.amelia.dsl.typesystem.AmeliaTypeComputer
import org.eclipse.xtext.generator.IGenerator
import org.eclipse.xtext.generator.IOutputConfigurationProvider
import org.eclipse.xtext.linking.LinkingScopeProviderBinding
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.scoping.IScopeProvider
import org.eclipse.xtext.xbase.compiler.XbaseCompiler
import org.eclipse.xtext.xbase.scoping.batch.ImplicitlyImportedFeatures
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputer
import org.amelia.dsl.debug.AmeliaStratumBreakpointSupport
import org.eclipse.xtext.debug.IStratumBreakpointSupport

/**
 * Use this class to register components to be used at runtime / without the Equinox extension registry.
 * 
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaRuntimeModule extends AbstractAmeliaRuntimeModule {
	
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
			.bind(IOutputConfigurationProvider)
			.to(AmeliaOutputConfigurationProvider)
			.in(Singleton)
		binder
			.bind(ImplicitlyImportedFeatures)
			.to(AmeliaImplicitlyImportedFeatures)
	}
	
	override Class<? extends IGenerator> bindIGenerator() {
		return OutputConfigurationAwaredGenerator
	}
	
	def Class<? extends ITypeComputer> bindITypeComputer() {
		return AmeliaTypeComputer
	}

	def Class<? extends XbaseCompiler> bindXbaseCompiler() {
		return AmeliaCompiler
	}
	
	override Class<? extends IQualifiedNameProvider> bindIQualifiedNameProvider() {
		return AmeliaQualifiedNameProvider
	}
	
	override Class<? extends IStratumBreakpointSupport> bindIStratumBreakpointSupport() {
		return AmeliaStratumBreakpointSupport
	}
	
}
