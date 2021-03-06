package semgen.annotation.workbench.drawers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import semgen.annotation.workbench.AnnotatorWorkbench.WBEvent;
import semgen.annotation.workbench.AnnotatorWorkbench.ModelEdit;
import semsim.annotation.SemSimTermLibrary;
import semsim.model.collection.FunctionalSubmodel;
import semsim.model.collection.Submodel;
import semsim.model.computational.Computation;
import semsim.model.computational.datastructures.DataStructure;
import semsim.utilities.CaseInsensitiveComparator;
import semsim.utilities.SemSimComponentComparator;

public class SubModelToolDrawer extends AnnotatorDrawer<Submodel> {
	public SubModelToolDrawer(SemSimTermLibrary lib, ArrayList<Submodel> modlist) {
		super(lib);
		componentlist.addAll(modlist);
		refreshSubModels();
	}
	
	public void refreshSubModels() {
		Collections.sort(componentlist, new SemSimComponentComparator());
	}
	
	public ArrayList<Integer> getSubmodelsToDisplay(boolean showimports) {
		ArrayList<Integer> sms = new ArrayList<Integer>();
		
		int i = 0;
		for (Submodel sm : componentlist) {
			if (!sm.isImported() || showimports) sms.add(i);
			i++;
		}
		return sms;
	}
	
	public boolean isEditable(int index) {
		boolean editable = true;
		Submodel sm = componentlist.get(index);
		if(sm.isFunctional()){
			editable = sm.getParentImport()==null;
		}
		return editable;
	}
	
	public boolean isImported() {
		return componentlist.get(currentfocus).isImported();
	}
	
	public String getComponentName(int index) {
		return componentlist.get(index).getName();
	}
	
	public boolean hasSingularAnnotation(int index) {
		return false;
	}

	public Submodel removeSubmodel() {
		return componentlist.remove(currentfocus);
	}
	
	public void addSubmodelstoSubmodel(ArrayList<Integer> sms) {
		ArrayList<Submodel> smset = new ArrayList<Submodel>();
		for (Integer i : sms) {
			smset.add(componentlist.get(i));
		}
		componentlist.get(currentfocus).setSubmodels(smset);
		setChanged();
		notifyObservers(ModelEdit.SM_SUBMODELS_CHANGED);
	}
	
	public Submodel addSubmodel(String name) {
		Submodel sm = new Submodel(name);
		componentlist.add(sm);
		refreshSubModels();
		currentfocus = componentlist.indexOf(sm);
		return sm;
	}
	
	public ArrayList<DataStructure> getSelectionDataStructures() {
		return getDataStructures(currentfocus);
	}
	
	public ArrayList<DataStructure> getDataStructures(Integer index) {
		ArrayList<DataStructure> smdslist = new ArrayList<DataStructure>();
		smdslist.addAll(componentlist.get(index).getAssociatedDataStructures());
		
		Collections.sort(smdslist, new SemSimComponentComparator());
		
		return smdslist;
	}
	
	public void setDataStructures(Collection<DataStructure> dslist) {
		componentlist.get(currentfocus).setAssociatedDataStructures(dslist);
		setChanged();
		notifyObservers(ModelEdit.SM_DATASTRUCTURES_CHANGED);
	}
	
	public ArrayList<String> getAssociatedSubModelDataStructureNames() {
		Set<DataStructure> smset = Submodel.getCodewordsAssociatedWithNestedSubmodels(componentlist.get(currentfocus));
		
		ArrayList<String> associated = new ArrayList<String>();
		for (DataStructure ds : smset) {
			String name = ds.getName();
			// Get rid of prepended submodel names if submodel is functional
			if (isFunctional()) name = name.substring(name.lastIndexOf(".")+1);
			associated.add(name);
		}
		
		Collections.sort(associated, new CaseInsensitiveComparator());
		return associated;
	}
	
	public ArrayList<String> getDataStructureNames() {
		ArrayList<String> smdslist = new ArrayList<String>();
		for (DataStructure ds : componentlist.get(currentfocus).getAssociatedDataStructures()) {
			String name = ds.getName();
			// Get rid of prepended submodel names if submodel is functional
			if (isFunctional()) name = name.substring(name.lastIndexOf(".")+1);
			smdslist.add(name);
		}
		
		Collections.sort(smdslist, new CaseInsensitiveComparator());
		
		return smdslist;
	}
	
	public void setSubmodelName(String newname) {
		Submodel sm = componentlist.get(currentfocus);
		String oldname = sm.getName();
		sm.setName(newname);
		sm.setLocalName(newname);
		
		refreshSubModels();
		for (DataStructure ds : sm.getAssociatedDataStructures()) {
			String name = ds.getName();
			int i = name.lastIndexOf(".");
			
			
			if (i!=-1) {
				name = newname + "." + name.substring(i+1);
				ds.setName(name);
			}
			
			if (ds.hasComputation()) {
				Computation comp = ds.getComputation();
				
				if (!comp.getComputationalCode().isEmpty()) {
					String eq = comp.getComputationalCode().replaceAll(oldname, newname);
					
					comp.setComputationalCode(eq);
				}
				if (comp.hasMathML()) {
					String eq = comp.getMathML().replaceAll(oldname, newname);
					comp.setMathML(eq);
				}
			}
		}
		
		currentfocus = componentlist.indexOf(sm);
		setChanged();
		notifyObservers(ModelEdit.SMNAMECHANGED);
	}
	
	public ArrayList<String> getAssociatedSubmodelNames() {
		ArrayList<String> associated = new ArrayList<String>();
		for (Submodel sm : componentlist.get(currentfocus).getSubmodels()) {
			
			// Don't show SemSim-style submodel subsumption if the subsumed models are empty
			if ( ! (sm instanceof FunctionalSubmodel) && sm.getAssociatedDataStructures().isEmpty()) continue;
			
			associated.add(sm.getName());
		}
		
		Collections.sort(associated, new CaseInsensitiveComparator());
		
		return associated;
	}
	
	@Override
	public void setHumanReadableDefinition(String newdef, boolean autoann){
		componentlist.get(currentfocus).setDescription(newdef);
		setChanged();
		notifyObservers(ModelEdit.FREE_TEXT_CHANGED);
	}
	
	@Override
	protected void selectionNotification() {
		notifyObservers(WBEvent.SMSELECTION);
	}
	
	public boolean isFunctional() {
		return componentlist.get(currentfocus).isFunctional();
	}
	
	public boolean isFunctional(int index) {
		return componentlist.get(index).isFunctional();
	}

	public String getHrefValue() {
		return 	componentlist.get(currentfocus).getHrefValue();
	}
		
	public ArrayList<Integer> getSubmodelsWithoutFocus() {
		ArrayList<Integer> submodels = new ArrayList<Integer>();
		for (int i = 0; i < componentlist.size(); i++) {
			submodels.add(i);
		}
		submodels.remove(currentfocus);
		
		return submodels;
	}
	
	public ArrayList<Integer> getAssociatedSubmodelIndicies(ArrayList<Integer> sms) {
		ArrayList<Integer> associates = new ArrayList<Integer>();
		
		for (Submodel sm : componentlist.get(currentfocus).getSubmodels()) {
			associates.add(componentlist.indexOf(sm));
		}
		return associates;
	}
	
	public ArrayList<Integer> getFunctionalSubmodelIndicies(ArrayList<Integer> sms) {
		ArrayList<Integer> associates = new ArrayList<Integer>();
		
		for (Integer sm : sms) {
			if (componentlist.get(sm).isFunctional()) {
				associates.add(sm);
			}
		}
		return associates;
	}
	
	@Override
	protected void changeNotification() {
		setChanged();
		notifyObservers(ModelEdit.SUBMODEL_CHANGED);
	}
}
