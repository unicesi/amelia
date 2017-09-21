/*
 * Copyright © 2017 Universidad Icesi
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
package org.amelia.dsl.web.resource

import org.eclipse.xtext.web.server.model.IWebResourceSetProvider
import org.eclipse.xtext.web.server.IServiceContext
import com.google.inject.Inject
import org.eclipse.emf.ecore.resource.ResourceSet
import javax.inject.Provider

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaResourceSetProvider implements IWebResourceSetProvider {

	static val MULTI_RESOURCE_PREFIX = 'multi-resource'

	@Inject Provider<ResourceSet> provider
	
	override get(String resourceId, IServiceContext serviceContext) {
		if (resourceId !== null && resourceId.startsWith(MULTI_RESOURCE_PREFIX)) {
			val pathEnd = Math.max(resourceId.indexOf('/'), MULTI_RESOURCE_PREFIX.length)
			return serviceContext.session.get(ResourceSet -> resourceId.substring(0, pathEnd), [provider.get])
		} else
			return provider.get
	}

}