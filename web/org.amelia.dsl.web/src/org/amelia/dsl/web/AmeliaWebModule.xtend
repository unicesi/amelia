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
package org.amelia.dsl.web

import org.eclipse.xtext.web.server.persistence.IResourceBaseProvider
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor
import org.eclipse.xtext.web.server.persistence.FileResourceHandler
import org.eclipse.xtext.web.server.persistence.IServerResourceHandler
import com.google.inject.Binder
import org.amelia.dsl.web.resource.AmeliaContentTypeProvider
import org.eclipse.xtext.web.server.model.IWebResourceSetProvider
import org.amelia.dsl.web.resource.AmeliaResourceSetProvider

/**
 * Use this class to register additional components to be used within the web application.
 * 
 * @author Miguel Jiménez - Initial contribution and API
 */
@FinalFieldsConstructor
class AmeliaWebModule extends AbstractAmeliaWebModule {

	val IResourceBaseProvider resourceBaseProvider

	override bindIContentTypeProvider() {
		return AmeliaContentTypeProvider
	}
	
	def Class<? extends IWebResourceSetProvider> bindIWebResourceSetProvider() {
		return AmeliaResourceSetProvider
	}

	def void configureResourceBaseProvider(Binder binder) {
		if (resourceBaseProvider !== null)
			binder
				.bind(IResourceBaseProvider)
				.toInstance(resourceBaseProvider)
	}

	def Class<? extends IServerResourceHandler> bindIServerResourceHandler() {
		return FileResourceHandler
	}

}
