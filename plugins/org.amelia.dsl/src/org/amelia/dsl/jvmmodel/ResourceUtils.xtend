package org.amelia.dsl.jvmmodel

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
