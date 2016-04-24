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
package org.amelia.dsl.validation

import org.eclipse.emf.common.util.URI
import java.util.ArrayList

class ResourceUtils {
	
	def static fromURItoFQN(URI resourceURI) {
		// e.g., platform:/resource/<project>/<source-folder>/org/example/.../TypeDecl.pascani
		var segments = new ArrayList
		if (resourceURI.segments.size > 1) {
			// Remove the first 3 segments, and return the package and file segments
			segments.addAll(resourceURI.segmentsList.subList(3, resourceURI.segments.size - 1))
			// Remove file extension and add the last segment
			segments.add(resourceURI.lastSegment.substring(0, resourceURI.lastSegment.lastIndexOf(".")))
		} else if(resourceURI.lastSegment.contains(".")) {
			segments.add(resourceURI.lastSegment.substring(0, resourceURI.lastSegment.lastIndexOf(".")))
		} else {
			segments.add(resourceURI.lastSegment)
		}
		return segments.fold("", [r, t|if(r.isEmpty) t else r + "." + t])
	}
	
}
