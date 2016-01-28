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
package org.amelia.dsl.web

import com.google.inject.Provider
import java.util.List
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.servlet.annotation.WebServlet
import org.eclipse.xtext.web.servlet.XtextServlet

/**
 * Deploy this class into a servlet container to enable DSL-specific services.
 */
@WebServlet(name = 'XtextServices', urlPatterns = '/xtext-service/*')
class AmeliaServlet extends XtextServlet {
	
	val List<ExecutorService> executorServices = newArrayList
	
	override init() {
		super.init()
		val Provider<ExecutorService> executorServiceProvider = [Executors.newCachedThreadPool => [executorServices += it]]
		new AmeliaWebSetup(executorServiceProvider).createInjectorAndDoEMFRegistration()
	}
	
	override destroy() {
		executorServices.forEach[shutdown()]
		executorServices.clear()
		super.destroy()
	}
	
}
