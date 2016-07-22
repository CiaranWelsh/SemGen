package semgen.stage.stagetasks.merge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.annotations.Expose;
import com.teamdev.jxbrowser.chromium.JSArray;
import com.teamdev.jxbrowser.chromium.JSObject;

import semgen.merging.workbench.DataStructureDescriptor;
import semgen.merging.workbench.DataStructureDescriptor.Descriptor;
import semgen.merging.workbench.Merger.ResolutionChoice;
import semgen.merging.workbench.MergerWorkbench;
import semgen.merging.workbench.MergerWorkbench.MergeEvent;
import semgen.stage.serialization.MergePreviewSubmodels;
import semgen.stage.serialization.SemSimModelSerializer;
import semgen.stage.serialization.StageState;
import semgen.stage.serialization.SubModelNode;
import semgen.stage.stagetasks.ModelInfo;
import semgen.stage.stagetasks.StageTask;
import semgen.utilities.SemGenError;
import semgen.utilities.uicomponent.SemGenProgressBar;
import semgen.visualizations.CommunicatingWebBrowserCommandReceiver;
import semsim.model.collection.SemSimModel;
import semsim.reading.ModelAccessor;

public class MergerTask extends StageTask<MergerWebBrowserCommandSender> implements Observer {
	private MergerWorkbench workbench = new MergerWorkbench();
	private MergePreview preview;
	private ArrayList<Pair<DataStructureDescriptor, DataStructureDescriptor>> dsdescriptors;
	protected MergeConflicts conflicts = new MergeConflicts();
	
	public MergerTask(ArrayList<ModelInfo> modelinfo, StageState state) {
		workbench.addObserver(this);
		_commandReceiver = new MergerCommandReceiver();
		ArrayList<ModelAccessor> files = new ArrayList<ModelAccessor>();
		ArrayList<SemSimModel> models = new ArrayList<SemSimModel>();
		
		for (ModelInfo model : modelinfo) {
			_models.put(model.getModelName(), model);
			files.add(model.accessor);
			models.add(model.Model);
		}
		this.state = state;
		workbench.addModels(files, models, true);

	}

	public void primeForMerging() {
		if (workbench.getNumberofStagedModels() == 0) return;
		if(workbench.hasMultipleModels()) {

			SemGenProgressBar progframe = new SemGenProgressBar("Comparing models...", true);
			workbench.mapModels();
			progframe.dispose();
		}
		//Check if two models have semantic overlap
		if (!workbench.hasSemanticOverlap()) {
			SemGenError.showError("SemGen did not find any semantic equivalencies between the models", "Merger message");
			return;
		}
		generateOverlapDescriptors();
		
		preview = workbench.generateMergePreview();
		collectConflicts();
	}

	private void generateOverlapDescriptors() {
		int n = workbench.getSolutionDomainCount();
		ArrayList<Pair<DataStructureDescriptor, DataStructureDescriptor>> descriptors = new ArrayList<Pair<DataStructureDescriptor, DataStructureDescriptor>>();
		
		for (int i = n; i < (workbench.getMappingCount()); i++) {
			descriptors.add(workbench.getDSDescriptors(i));
		}
		dsdescriptors = descriptors;
	}
	
	private void collectConflicts() {
		ArrayList<Boolean> units = workbench.getUnitOverlaps();
		for (int i=0; i<units.size(); i++) {
			if (units.get(i)) {
				conflicts.unitconflicts.add(new UnitConflict(dsdescriptors.get(i), i));
			}
		}
		HashMap<String, String> smoverlaps = workbench.createIdenticalSubmodelNameMap();
		
		for (String overlap : smoverlaps.keySet()) {
			conflicts.dupesubmodels.add(new SyntacticDuplicate(overlap));
		}
		HashMap<String, String> cwoverlaps = workbench.createIdenticalNameMap();
		
		for (String overlap : cwoverlaps.keySet()) {
			conflicts.dupecodewords.add(new SyntacticDuplicate(overlap));
		}
	}
	
	public ModelAccessor saveMerge() {
		return workbench.saveModelAs();
	}
	
	@Override
	public void update(Observable o, Object arg) {
		if (arg==MergeEvent.modellistupdated) {
			primeForMerging();
		}
		if (arg == MergeEvent.threemodelerror) {
			SemGenError.showError("SemGen can only merge two models at a time.", "Too many models");
		}	
		if (arg == MergeEvent.modelerrors) {
			JOptionPane.showMessageDialog(null, "Model " + ((MergeEvent)arg).getMessage() + " has errors.",
					"Failed to analyze.", JOptionPane.ERROR_MESSAGE);
		}
		if (arg == MergeEvent.mergecompleted) {
			ModelAccessor modelfile = saveMerge();
			String mergedname = workbench.getMergedModelName();
			_models.put(mergedname, new ModelInfo(workbench.getMergedModel(), modelfile));
			_commandSender.mergeCompleted(mergedname);
		}
		if (arg == MergeEvent.mappingadded) {	
			generateOverlapDescriptors();
			preview = workbench.generateMergePreview();
		}
	}
	
	protected class MergerCommandReceiver extends CommunicatingWebBrowserCommandReceiver {

		public void onInitialized(JSObject jstaskobj) {
			jstask = jstaskobj;
			jstask.setProperty("conflictsj", new MergerBridge());
		}
		
		public void onRequestConflicts() {
			_commandSender.showConflicts(conflicts);
		}
		
		public void onRequestOverlaps() {
			ArrayList<Overlap> overlaps = new ArrayList<Overlap>();
			for (Pair<DataStructureDescriptor, DataStructureDescriptor> dsd : dsdescriptors) {
				overlaps.add(new Overlap(dsd));
			}
			_commandSender.showOverlaps(overlaps.toArray(new Overlap[]{}));
		}

		public void onMinimizeTask(JSObject snapshot) {
			createStageState(snapshot);
			switchTask(0);
		}
		
		public void onRequestPreview(Double index) {
			MergePreviewSubmodels psms = preview.getPreviewSerializationforSelection(index);
			_commandSender.showPreview(psms);
		}

		public void onCreateCustomOverlap(String nodes, Double nodemodelindex) {
			String[] nodestolink = nodes.split(",");
			if (nodemodelindex.intValue()==0) {
				workbench.addManualCodewordMapping(nodestolink[0], nodestolink[1]);
			}
			else {
				workbench.addManualCodewordMapping(nodestolink[1], nodestolink[0]);
			}
			preview = workbench.generateMergePreview();
		}

		public void onExecuteMerge(JSArray choicesmade) {
			ArrayList<ResolutionChoice> choicelist = new ArrayList<ResolutionChoice>();

			int ndomains = workbench.getSolutionDomainCount();
			for (int i = 0; i < ndomains; i++) {
				choicelist.add(ResolutionChoice.first);
			}
			
			for (int i=0; i<choicesmade.length(); i++) {
				int choice = choicesmade.get(i).asNumber().getInteger();
				switch(choice) {
				case 0:
					choicelist.add(ResolutionChoice.first);
					break;
				case 1:
					choicelist.add(ResolutionChoice.second);
					break;
				case 2:
					choicelist.add(ResolutionChoice.ignore);
					break;
				}
			}
			
			SemGenProgressBar progframe = new SemGenProgressBar("Merging...", true);
			String error = workbench.executeMerge(conflicts.buildCodewordNameMap(), 
					conflicts.buildSubmodelNameMap(), 
					choicelist, 
					conflicts.buildConversionList(), 
					progframe);
			
			if (error!=null){
				SemGenError.showError(
						"ERROR: " + error, "Merge Failed");
			}

		}

		public void onQueryModel(String modelName, String query) {
			ModelInfo modelInfo = _models.get(modelName);
			switch (query) {
			case "hassubmodels":
				Boolean hassubmodels = !modelInfo.Model.getSubmodels().isEmpty();
				_commandSender.receiveReply(hassubmodels.toString());
				break;
			case "hasdependencies":
				Boolean hasdependencies = !modelInfo.Model.getAssociatedDataStructures().isEmpty();
				_commandSender.receiveReply(hasdependencies.toString());
				break;
			}
		}
		public void onTaskClicked(String modelName, String task) {
			onTaskClicked(modelName, task, null);
		}

		public void onTaskClicked(String modelName, String task, JSObject snapshot) {
			// If the model doesn't exist throw an exception
			if(!_models.containsKey(modelName))
				throw new IllegalArgumentException(modelName);

			// Get the model
			ModelInfo modelInfo = _models.get(modelName);
			SemSimModel model = modelInfo.Model;

			// Execute the proper task
			switch(task) {
				case "dependencies":
					_commandSender.showDependencyNetwork(model.getName(),
							SemSimModelSerializer.getDependencyNetwork(model));
					break;
				case "submodels":
					SubModelNode[] submodelNetwork = SemSimModelSerializer.getSubmodelNetwork(model);
					if(submodelNetwork.length <= 0)
						JOptionPane.showMessageDialog(null, "'" + model.getName() + "' does not have any submodels");
					else
						_commandSender.showSubmodelNetwork(model.getName(), submodelNetwork);
					break;
				default:
					JOptionPane.showMessageDialog(null, "Task: '" + task +"', coming soon :)");
					break;
			}
		}

		public void onConsoleOut(String msg) {
			System.out.println(msg);
		}
		
		public void onConsoleOut(Number msg) {
			System.out.println(msg.toString());
		}
		
		public void onConsoleOut(boolean msg) {
			System.out.println(msg);
		}

	}
	
	@Override
	public Task getTaskType() {
		return Task.MERGER;
	}
	
	public Class<MergerWebBrowserCommandSender> getSenderInterface() {
		return MergerWebBrowserCommandSender.class;
	}
	
	//Classes for passing information to the stage
	public class Overlap {
		public StageDSDescriptor dsleft;
		public StageDSDescriptor dsright; 
		
		protected Overlap(Pair<DataStructureDescriptor, DataStructureDescriptor> dsdesc) {
			dsleft = new StageDSDescriptor(dsdesc.getLeft());
			dsright = new StageDSDescriptor(dsdesc.getRight());
		}
	}
	
	public class StageDSDescriptor {
		public String name;
		public String type;
		public String description;
		public String annotation;
		public String equation;
		public String unit;

		protected StageDSDescriptor(DataStructureDescriptor dsdesc) {
			name = dsdesc.getDescriptorValue(Descriptor.name);
			type = dsdesc.getDescriptorValue(Descriptor.type);
			description = dsdesc.getDescriptorValue(Descriptor.description);
			annotation = dsdesc.getDescriptorValue(Descriptor.annotation);
			equation = dsdesc.getDescriptorValue(Descriptor.computationalcode);
			unit = dsdesc.getDescriptorValue(Descriptor.units);
		}
	}
	
	public class MergeConflicts {
		@Expose public ArrayList<SyntacticDuplicate> dupecodewords = new ArrayList<SyntacticDuplicate>();
		@Expose public ArrayList<SyntacticDuplicate> dupesubmodels = new ArrayList<SyntacticDuplicate>();
		@Expose public ArrayList<UnitConflict> unitconflicts = new ArrayList<UnitConflict>();
	
		public HashMap<String,String> buildCodewordNameMap() {
			HashMap<String,String> dupemap = new HashMap<String, String>();
			for (SyntacticDuplicate dcw : dupecodewords) {
				dupemap.put(dcw.duplicate, dcw.replacement);
			}
			
			return dupemap;
		}
		
		public HashMap<String,String> buildSubmodelNameMap() {
			HashMap<String,String> dupemap = new HashMap<String, String>();
			for (SyntacticDuplicate dsm : dupesubmodels) {
				dupemap.put(dsm.duplicate, dsm.replacement);
			}
			
			return dupemap;
		}
		
		public ArrayList<Pair<Double,String>> buildConversionList() {
			ArrayList<Pair<Double,String>> conversions = new ArrayList<Pair<Double,String>>();
			for (UnitConflict uc : unitconflicts) {
				conversions.add(uc.getConversion());
			}
			
			return conversions;
		}
	}
	
	public class SyntacticDuplicate {
		@Expose public String duplicate;
		@Expose public String replacement = "";
		@Expose public boolean userightmodel = true;
		
		protected SyntacticDuplicate(String dupe) {
			duplicate = dupe;
		}
		
		public void setReplacementName(boolean rightmodel, String rep) {
			replacement = rep;
			userightmodel = rightmodel;
		}
	}
	
	public static class Jjstest {
		public void test() {
			System.out.println("Sucess!");
		}
	}
	
	public class UnitConflict {
		@Expose public String cdwdleft;
		@Expose public String cwdright;
		@Expose public String unitleft;
		@Expose public String unitright;
		@Expose public boolean multiply = true;
		@Expose public Float conversion = 1.0f;
		@Expose public int index;
		
		protected UnitConflict(Pair<DataStructureDescriptor, DataStructureDescriptor> descs, int index) {
			cdwdleft = descs.getLeft().getDescriptorValue(Descriptor.name);
			unitleft = descs.getLeft().getDescriptorValue(Descriptor.units);
			cwdright = descs.getRight().getDescriptorValue(Descriptor.name);
			unitright = descs.getRight().getDescriptorValue(Descriptor.units);
			this.index = index;
		}
		
		public void setConversion(Float val, boolean mult) {
			conversion = val;
			multiply = mult;
		}
		
		public Pair<Double, String> getConversion() {
			String operator = "*";
			if (!multiply) operator = "/"; 
			return Pair.of(conversion.doubleValue(), operator);
		}
	}
	
	public class MergerBridge {
		public void setUnitConversion(Integer index, boolean multiply, String conversion) {
			conflicts.unitconflicts.get(index).setConversion(Float.valueOf(conversion), multiply);
		}
		
		public void setSubmodelName(Integer index, boolean rightmodel, String name) {
			conflicts.dupesubmodels.get(index).setReplacementName(rightmodel, name);
		}
		
		public void setCodewordName(Integer index, boolean rightmodel, String name) {
			conflicts.dupecodewords.get(index).setReplacementName(rightmodel, name);
		}
		
	}
}
