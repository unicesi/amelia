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
package org.amelia.dsl.validation

import java.util.Collection
import org.amelia.dsl.amelia.Task
import java.util.Set
import org.eclipse.xtext.validation.Check
import org.amelia.dsl.amelia.AmeliaPackage
import org.amelia.dsl.amelia.Subsystem
import org.eclipse.emf.ecore.EObject

/**
 * This class contains custom validation rules. 
 *
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 */
class AmeliaValidator extends AbstractAmeliaValidator {
	
	public static val CYCLIC_DEPENDENCY = "amelia.issue.cyclicDependency"
	
	@Check
	def void checkNoRecursiveDependencies(Task task) {
		task.findDependentElements [ cycle |
			if (cycle.size == 1) {
				error('''The task '«task.name»' cannot depend on itself.''', 
					  cycle.head, AmeliaPackage.Literals.TASK__NAME, CYCLIC_DEPENDENCY)
			} else {
				error('''There is a cyclic dependency that involves tasks «cycle.filter(Task).map[name].join(", ")»''', 
					  cycle.head, AmeliaPackage.Literals.TASK__NAME, CYCLIC_DEPENDENCY)
			}
		]
	}
	
	@Check
	def void checkNoRecursiveDependencies(Subsystem subsystem) {
		subsystem.findDependentElements [ cycle |
			if (cycle.size == 1) {
				error('''The subsystem '«subsystem.name»' cannot depend on itself.''', 
					  cycle.head, AmeliaPackage.Literals.SUBSYSTEM__NAME, CYCLIC_DEPENDENCY)
			} else {
				error('''There is a cyclic dependency that involves subsystems «cycle.filter(Subsystem).map[name].join(", ")»''', 
					  cycle.head, AmeliaPackage.Literals.SUBSYSTEM__NAME, CYCLIC_DEPENDENCY)
			}
		]
	}
	
	/**
	 * Adapted from 
	 * https://github.com/xtext/seven-languages-xtext/blob/master/languages/\
	 * org.xtext.builddsl/src/org/xtext/builddsl/validation/BuildDSLValidator.xtend
	 */
	def private Collection<EObject> findDependentElements(EObject it, (Set<EObject>)=>void cycleHandler) {
		// 1. collect all tasks that we depend on
		val elements = <EObject>newLinkedHashSet
		internalFindDependentTasksRec(it, elements)

		// 2. sort them so that dependents come after dependees 
		val result = <EObject>newLinkedHashSet
		var changed = true
		while (changed) {
			changed = false
			for (t : elements.toList) {
				val dependencies = if (t instanceof Task)
						t.dependencies
					else if (t instanceof Subsystem)
						t.dependencies
				if (result.containsAll(dependencies)) {
					changed = true
					result.add(t)
					elements.remove(t)
				}
			}
		}
		if (!elements.empty && cycleHandler != null)
			cycleHandler.apply(elements)
		result
	}
	
	def private void internalFindDependentTasksRec(EObject e, Set<EObject> set) {
		if (!set.add(e))
			return;
		val dependencies = if (e instanceof Task)
				e.dependencies
			else if (e instanceof Subsystem)
				e.dependencies
		for (t : dependencies) 
			internalFindDependentTasksRec(t, set)
	}
	
}
