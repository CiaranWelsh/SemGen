package semgen.encoding;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.semanticweb.owlapi.model.OWLException;

import semgen.SemGen;
import semsim.model.SemSimModel;
import semsim.model.physical.PhysicalEntity;
import semsim.model.physical.PhysicalProcess;
import semsim.model.physical.object.CustomPhysicalEntity;
import semsim.model.physical.object.CustomPhysicalProcess;
import semsim.model.physical.object.PhysicalProperty;

public class ChalkboardCoder {

	public static Hashtable<String,CustomPhysicalEntity> nounnamesandphysents;
	private static int x = 1;
	public static Set<Element> nouncollection = new HashSet<Element>();
	
	public static void translate(SemSimModel semsimmodel, File outputfile) throws JDOMException, IOException, OWLException {
		// Create top of CB xml file
		Element root = new Element("BioDModel");
		root.setAttribute("BioDMLversion", "0.02");
		root.setAttribute("AuthorApp", "Chalkboard");
		root.setAttribute("ModelName", semsimmodel.getName());
		
		Document doc = new Document(root);
		
		Element nounlist = new Element("nounList");
		Element verblist = new Element("verbList");
		
		root.addContent(nounlist);
		root.addContent(verblist);

		Set<Element> verbcollection = new HashSet<Element>();
		
		Set<CustomPhysicalProcess> setofverbobjects = new HashSet<CustomPhysicalProcess>();
		
		nounnamesandphysents = new Hashtable<String,CustomPhysicalEntity>();

		Set<String> allflowrates = SemGen.semsimlib.getOPBsubclasses("OPB_00573");
		SemGen.semsimlib.getOPBsubclasses("OPB00093");
		SemGen.semsimlib.getOPBsubclasses("OPB_00569");

		/* For each data structure
		if has a property that is a flow, get entity-flow physiomap
		else
			if associated with an entity, use the entity as the node
			else use the computational component
			
			collect its inputs and what it's an input for
			for inputs
				see if physical entity associated with input
					if yes, assert connection between input physical entity and the current node
					else assert connection between input data structure and the current node
			for what its an input for
				see if physical entity associated with what its an input for
					if yes, assert connection between the antecedent physical entity and the node
					else assert connection between what its an input for and the current node
		*/
		
		
		for(PhysicalProperty prop : semsimmodel.getPhysicalProperties()){
			if(prop.hasRefersToAnnotation()){
				if(allflowrates.contains(prop.getFirstRefersToReferenceOntologyAnnotation().getReferenceURI().toString())){  // If we have terms annotated against OPB flow rates
					PhysicalProcess proc = (PhysicalProcess) prop.getPhysicalPropertyOf();
					String processname = "Process " + Integer.toString(x);
					
					// Get the inputs to the flow term
					String verbID = Integer.toString(x);
					String desc;
					if(prop.getPhysicalPropertyOf().hasRefersToAnnotation()){
						desc = prop.getPhysicalPropertyOf().getFirstRefersToReferenceOntologyAnnotation().getValueDescription();
					}
					else desc = processname;
					CustomPhysicalProcess verbobject = new CustomPhysicalProcess(processname, desc);
					setofverbobjects.add(verbobject);
					x++;
					
					// Associate the source physical entities with the process
					for(PhysicalEntity input : proc.getSourcePhysicalEntities()){
						CustomPhysicalEntity noun = collectNounData(input, verbID);
						if(noun!=null && !verbobject.setofinputs.contains(noun)){
							verbobject.setofinputs.add(noun);
						}
					}
					// Get the sinks
					for(PhysicalEntity output : proc.getSinkPhysicalEntities()){
						CustomPhysicalEntity noun = collectNounData(output, verbID);
						
						// Add the noun to the set of outputs
						if(noun!=null && !verbobject.setofoutputs.contains(noun)){
							verbobject.setofoutputs.add(noun);
						}
					}
					
					// Get the mediators
					for(PhysicalEntity mediator : proc.getMediatorPhysicalEntities()){
						CustomPhysicalEntity noun = collectNounData(mediator, verbID);
						
						// Add the noun to the set of outputs
						if(noun!=null && !verbobject.setofmediators.contains(noun)){
							verbobject.setofmediators.add(noun);
						}
					}
				}
			}
		}
		
		// Set the input/output relationships in the XML document
		for(CustomPhysicalProcess verb : setofverbobjects){
			Element verbel = new Element("verb").setAttribute("name",verb.getName());
			
			for(CustomPhysicalEntity input : verb.setofinputs){
				verbel.addContent(new Element("start").setAttribute("nounID",input.getName()));
			}
			for(CustomPhysicalEntity output : verb.setofoutputs){
				verbel.addContent(new Element("end").setAttribute("nounID",output.getName()));
			}
			for(CustomPhysicalEntity mediator : verb.setofmediators){
				verbel.addContent(new Element("mediator").setAttribute("nounID",mediator.getName()));
			}
			verbcollection.add(verbel);
		}
		
		nounlist.addContent(nouncollection);
		verblist.addContent(verbcollection);
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(doc, new FileWriter(outputfile));
	}
	
	public static CustomPhysicalEntity collectNounData(PhysicalEntity ent, String verbID){
		CustomPhysicalEntity noun;
		String compositename = ent.getName();
		if(!nounnamesandphysents.keySet().contains(compositename)){
			String nounID = Integer.toString(x);
			noun = new CustomPhysicalEntity(ent.getName(), ent.getName());
			Element nounel = new Element("noun");
			nounel.setAttribute("ID", nounID);
			nounel.setAttribute("name",compositename);
			nouncollection.add(nounel);
			nounnamesandphysents.put(compositename, noun);
			x++;
		}
		else{
			noun = nounnamesandphysents.get(compositename);
		}
		return noun;
	}
}