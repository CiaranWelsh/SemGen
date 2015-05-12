package semsim.model.physical.object;

import java.net.URI;

import semsim.SemSimConstants;
import semsim.annotation.ReferenceOntologyAnnotation;
import semsim.annotation.ReferenceTerm;
import semsim.model.physical.PhysicalProcess;

public class ReferencePhysicalProcess extends PhysicalProcess implements ReferenceTerm{
	
	public ReferencePhysicalProcess(URI uri, String description){
		addReferenceOntologyAnnotation(SemSimConstants.REFERS_TO_RELATION, uri, description);
		setName(description);
	}
	
	
	public ReferenceOntologyAnnotation getRefersToReferenceOntologyAnnotation(){
		if(hasRefersToAnnotation()){
			return new ReferenceOntologyAnnotation(SemSimConstants.REFERS_TO_RELATION, referenceuri, getDescription());
		}
		return null;
	}
	
	public URI getReferstoURI() {
		return URI.create(referenceuri.toString());
	}
	
	
	@Override
	public URI getSemSimClassURI() {
		return SemSimConstants.REFERENCE_PHYSICAL_PROCESS_CLASS_URI;
	}
	
	@Override
	protected boolean isEquivalent(Object obj) {
		return ((ReferencePhysicalProcess)obj).getReferstoURI().compareTo(referenceuri)==0;
	}
}
