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
package org.amelia.dsl.ui.hover

import com.google.inject.Inject
import org.amelia.dsl.services.AmeliaGrammarAccess
import org.eclipse.xtext.Keyword

/**
 * @author Miguel Jiménez - Initial contribution and API
 */
class AmeliaKeywordHovers {

	@Inject AmeliaGrammarAccess ga;

	def hoverText(Keyword k) {
		val result = switch (k) {
			case ga.subsystemDeclarationAccess.subsystemKeyword_0: '''
				A <b>subsystem</b> is a modular unit representing the operations
				required to deploy a cohesive set of software artefacts. It <ul>
				<li>can <code>include</code> another subsystem, i.e., use its execution rules.</li>
				<li>can <code>depend on</code> another subsystem, i.e., wait for its termination to initiate its own execution.</li>
				<li>has parameters, specification syntax <code>param &lt;type&gt; &lt;name&gt; [= &lt;value&gt;]</code></li>
				<li>has variables, specification syntax <code>var &lt;type&gt; &lt;name&gt; [= &lt;value&gt;]</code></li>
				<li>has on-host statements, specification syntax <code>on &lt;host&gt; { &lt;List of Execution Rules&gt; }</code></li>
				<li>has an optional configuration block, specification syntax <code>config { ... }</code></li>
				</ul>
			'''
			case ga.deploymentDeclarationAccess.deploymentKeyword_0: '''
				A <b>deployment</b> is an execution flow specification that
				dictates how to perform the deployment. For example, it allows
				to retry on failure or systematically repeat the same deployment,
				which is useful to warm up a system before running performance tests.
			'''
		}
		return if (result !== null) result.toString
	}

}
