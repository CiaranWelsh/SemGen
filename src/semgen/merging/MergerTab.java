package semgen.merging;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Observer;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;

import org.jdom.JDOMException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import JSim.util.Xcept;
import semgen.GlobalActions;
import semgen.SemGenSettings;
import semgen.encoding.Encoder;
import semgen.merging.workbench.MergerWorkbench;
import semgen.utilities.GenericThread;
import semgen.utilities.SemGenError;
import semgen.utilities.SemGenFont;
import semgen.utilities.SemGenIcon;
import semgen.utilities.SemGenTask;
import semgen.utilities.file.LoadSemSimModel;
import semgen.utilities.file.SemGenOpenFileChooser;
import semgen.utilities.file.SemGenSaveFileChooser;
import semgen.utilities.uicomponent.SemGenProgressBar;
import semgen.utilities.uicomponent.SemGenScrollPane;
import semgen.utilities.uicomponent.SemGenTab;
import semsim.SemSimUtil;
import semsim.model.SemSimModel;
import semsim.model.annotation.ReferenceOntologyAnnotation;
import semsim.model.computational.datastructures.DataStructure;
import semsim.model.physical.CompositePhysicalEntity;
import semsim.model.physical.PhysicalProperty;
import semsim.model.physical.Submodel;
import semsim.owl.SemSimOWLFactory;
import semsim.reading.ModelClassifier;
import semsim.writing.CaseInsensitiveComparator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

public class MergerTab extends SemGenTab implements ActionListener, MouseListener {

	private static final long serialVersionUID = -1383642730474574843L;
	public OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	public File file1;
	public File file2;
	public File mergedfile;
	public SemSimModel semsimmodel1;
	public SemSimModel semsimmodel2;

	public int dividerlocation = 350;
	public JPanel filelistpanel = new JPanel();
	public JButton plusbutton = new JButton(SemGenIcon.plusicon);
	public JButton minusbutton = new JButton(SemGenIcon.minusicon);

	public JPanel resolvepanel = new JPanel();
	public SemGenScrollPane resolvescroller;
	public JButton mergebutton = new JButton("MERGE");

	public JSplitPane resmapsplitpane;
	public MappingPanel mappingpanelleft = new MappingPanel("[ ]");
	public MappingPanel mappingpanelright = new MappingPanel("[ ]");
	public JButton addmanualmappingbutton = new JButton("Add manual mapping");
	public JButton loadingbutton = new JButton(SemGenIcon.blankloadingiconsmall);
	public Set<String> initialidenticalinds = new HashSet<String>();
	public Set<String> identicaldsnames = new HashSet<String>();
	
	public MergerTab(SemGenSettings sets, GlobalActions globalacts, MergerWorkbench bench) {
		super("Merger", SemGenIcon.mergeicon, "Tab for Merging SemSim Models", sets, globalacts);
	}
	
	@Override
	public void loadTab() {
	this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

	JLabel filelisttitle = new JLabel("Models to merge");
	filelisttitle.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));

	plusbutton.addActionListener(this);
	minusbutton.addActionListener(this);

	loadingbutton.setBorderPainted(false);
	loadingbutton.setContentAreaFilled(false);
	
	JPanel plusminuspanel = new JPanel();
	plusminuspanel.setLayout(new BoxLayout(plusminuspanel, BoxLayout.X_AXIS));
	plusminuspanel.add(filelisttitle);
	plusminuspanel.add(plusbutton);
	plusminuspanel.add(minusbutton);
	
	JPanel filelistheader = new JPanel();
	filelistheader.add(plusminuspanel);

	filelistpanel.setBackground(Color.white);
	filelistpanel.setLayout(new BoxLayout(filelistpanel, BoxLayout.Y_AXIS));

	JScrollPane filelistscroller = new JScrollPane(filelistpanel);

	mergebutton.setFont(SemGenFont.defaultBold());
	mergebutton.setForeground(Color.blue);
	mergebutton.addActionListener(this);
	
	JPanel mergebuttonpanel = new JPanel();
	mergebuttonpanel.add(mergebutton);
	
	JPanel filelistpanel = new JPanel(new BorderLayout());
	filelistpanel.add(filelistheader, BorderLayout.WEST);
	filelistpanel.add(filelistscroller, BorderLayout.CENTER);
	filelistpanel.add(mergebuttonpanel, BorderLayout.EAST);
	filelistpanel.setAlignmentX(LEFT_ALIGNMENT);
	filelistpanel.setPreferredSize(new Dimension(settings.getAppWidth() - 200, 60));
	filelistpanel.setMaximumSize(new Dimension(99999, 175));
	
	resolvepanel.setLayout(new BoxLayout(resolvepanel, BoxLayout.Y_AXIS));
	resolvepanel.setBackground(Color.white);
	resolvescroller = new SemGenScrollPane(resolvepanel);
	resolvescroller.setBorder(BorderFactory.createTitledBorder("Resolution points between models"));
	resolvescroller.setAlignmentX(LEFT_ALIGNMENT);

	JSplitPane mappingsplitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mappingpanelleft, mappingpanelright);
	mappingsplitpane.setOneTouchExpandable(true);
	mappingsplitpane.setAlignmentX(LEFT_ALIGNMENT);
	mappingsplitpane.setDividerLocation((settings.getAppWidth() - 20) / 2);

	resmapsplitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resolvescroller, mappingsplitpane);
	resmapsplitpane.setOneTouchExpandable(true);
	resmapsplitpane.setDividerLocation(dividerlocation);

	addmanualmappingbutton.addActionListener(this);
	
	JPanel mappingbuttonpanel = new JPanel();
	mappingbuttonpanel.setAlignmentX(LEFT_ALIGNMENT);
	mappingbuttonpanel.add(addmanualmappingbutton);

	this.add(filelistpanel);
	this.add(resmapsplitpane);
	this.add(mappingbuttonpanel);
	this.add(Box.createGlue());
	this.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));}
	
	public void actionPerformed(ActionEvent arg0) {
		Object o = arg0.getSource();
		
		if (o == plusbutton)
			PlusButtonAction();

		if (o == minusbutton) {
			Boolean cont = false;
			for (Component comp : filelistpanel.getComponents()) {
				if (comp instanceof FileToMergeLabel) {
					FileToMergeLabel ftml = (FileToMergeLabel) comp;
					if (ftml.selected) {
						filelistpanel.remove(comp);
						cont = true;
					}
				}
			}
			if(cont){
				filelistpanel.validate();
				if (filelistpanel.getComponents().length > 0) {
					filelistpanel.getComponent(0).setForeground(Color.blue);
					if (filelistpanel.getComponents().length > 1) {
						filelistpanel.getComponent(1).setForeground(Color.red);
						GenericThread primethread = new GenericThread(this, "primeForMergingStep");
						loadingbutton.setIcon(SemGenIcon.loadingiconsmall);
						primethread.start();
					}
				}
				filelistpanel.repaint();
				filelistpanel.validate();
			}
		}

		if (o == mergebutton) {
			File file = null;
			if (filelistpanel.getComponents().length == 2) {
				file = saveMerge();
				if (file!=null) {
					mergedfile = file;
					addmanualmappingbutton.setEnabled(false);
					
					MergeTask task = new MergeTask();
					task.execute();
					
				}
			}
		}

		if (o == addmanualmappingbutton) {
			if (!mappingpanelleft.scrollercontent.isSelectionEmpty()
					&& !mappingpanelright.scrollercontent.isSelectionEmpty()) {
				String cdwd1 = (String) mappingpanelleft.scrollercontent.getSelectedValue();
				String cdwd2 = (String) mappingpanelright.scrollercontent.getSelectedValue();
				cdwd1 = cdwd1.substring(cdwd1.lastIndexOf("(") + 1, cdwd1.lastIndexOf(")"));
				cdwd2 = cdwd2.substring(cdwd2.lastIndexOf("(") + 1, cdwd2.lastIndexOf(")"));

				if (!codewordsAlreadyMapped(cdwd1, cdwd2, true)) {
					if(resolvepanel.getComponentCount()!=0) resolvepanel.add(new JSeparator());
					ResolutionPanel newrespanel = new ResolutionPanel(semsimmodel1.getDataStructure(cdwd1),
							semsimmodel2.getDataStructure(cdwd2),
							semsimmodel1, semsimmodel2, "(manual mapping)", true);
					resolvepanel.add(newrespanel);
					resolvepanel.repaint();
					resolvepanel.validate();
					this.validate();
					resolvescroller.scrollToComponent(newrespanel);
				}
			} else {
				SemGenError.showError("Please select a codeword from both component models","");
			}
		}
	}

	public void PlusButtonAction(){
			startAdditionOfModels();
	}

	public void startAdditionOfModels(){
		AddModelsToMergeTask task = new AddModelsToMergeTask();
		task.execute();
	}
	
	private class AddModelsToMergeTask extends SemGenTask {
		public Set<File> files = new HashSet<File>();
        public AddModelsToMergeTask(){
        	new SemGenOpenFileChooser(files, "Select SemSim models to merge",
        			new String[]{"owl", "xml", "sbml", "cellml", "mod"});
        }
        @Override
        public Void doInBackground() {
        	progframe = new SemGenProgressBar("Loading models...", true);
        	if (files.size() == 0) endTask();
        	try {
				addModelsToMerge(files);
			} catch (Exception e) {
				e.printStackTrace();
			}
            return null;
        }
    }

	private void addModelsToMerge(Set<File> files) {
		initialidenticalinds = new HashSet<String>();
		identicaldsnames = new HashSet<String>();
		for (File file : files) {
			if(ModelClassifier.classify(file)==ModelClassifier.CELLML_MODEL){
				SemGenError.showFunctionalSubmodelError(file);
			}
			else{
				FileToMergeLabel templabel = new FileToMergeLabel(file.getAbsolutePath());
				templabel.addMouseListener(this);
				templabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
				if (filelistpanel.getComponentCount() == 0) templabel.setForeground(Color.blue);
				else if (filelistpanel.getComponentCount() == 1) templabel.setForeground(Color.red);
				filelistpanel.add(templabel);
			}
		}
		if(filelistpanel.getComponentCount() > 1) 
			primeForMergingStep();
		validate();
		repaint();
	}
	
	public class MergeTask extends SemGenTask {
		public MergeTask(){
			progframe = new SemGenProgressBar("Merging...", true);
		}
        @Override
        public Void doInBackground() {	
        	try {
				merge();
			} catch (Exception e) {
				e.printStackTrace();
			}
            return null;
        }
        @Override
        public void endTask() {
			addmanualmappingbutton.setEnabled(true);
        }
	}

	public void primeForMergingStep() {
		mergebutton.setEnabled(true);
		Component[] filecomponents = filelistpanel.getComponents();
		if (filecomponents.length > 1) {
			refreshModelsToMerge();
			
			// If either of the models have errors, quit
			for(SemSimModel model : refreshModelsToMerge()){
				if(!model.getErrors().isEmpty()){
					JOptionPane.showMessageDialog(this, "Model " + model.getName() + " has errors.",
							"Failed to analyze.", JOptionPane.ERROR_MESSAGE);
					mergebutton.setEnabled(false);
					return;
				}
			}
		}
		
		populateMappingPanel(file1.getName(), semsimmodel1, mappingpanelleft, Color.blue);
		populateMappingPanel(file2.getName(), semsimmodel2, mappingpanelright, Color.red);
		identicaldsnames = identifyIdenticalCodewords();
		initialidenticalinds.addAll(identicaldsnames);
		identifyExactSemanticOverlap();
		resmapsplitpane.setDividerLocation(dividerlocation);
		loadingbutton.setIcon(SemGenIcon.blankloadingiconsmall);
	}

	public void populateMappingPanel(String filename, SemSimModel model, MappingPanel mappingpanel, Color color) {
		Set<String> descannset = new HashSet<String>();
		Set<String> nodescannset = new HashSet<String>();
		for (DataStructure datastr : model.getDataStructures()) {
			String desc = "(" + datastr.getName() + ")";
			if(datastr.getDescription()!=null){
				desc = datastr.getDescription() + " " + desc;
				descannset.add(desc);
			}
			else nodescannset.add(desc);
		}
		
		String[] descannarray = (String[]) descannset.toArray(new String[] {});
		String[] nodescannarray = (String[]) nodescannset.toArray(new String[] {});
		Arrays.sort(descannarray,new CaseInsensitiveComparator());
		Arrays.sort(nodescannarray,new CaseInsensitiveComparator());
		String[] comboarray = new String[descannarray.length + nodescannarray.length];
		for(int i=0; i<comboarray.length; i++){
			if(i<descannarray.length) comboarray[i] = descannarray[i];
			else comboarray[i] = nodescannarray[i-descannarray.length];
		}
		mappingpanel.scrollercontent.setForeground(color);
		mappingpanel.scrollercontent.setListData(comboarray);
		mappingpanel.setTitle(filename);
	}

	public Set<String> identifyIdenticalCodewords() {
		Set<String> matchedcdwds = new HashSet<String>();
		for (DataStructure ds : semsimmodel1.getDataStructures()) {
			if (semsimmodel2.containsDataStructure(ds.getName()))
				matchedcdwds.add(ds.getName());
		}
		return matchedcdwds;
	}

	public void identifyExactSemanticOverlap() {
		resolvepanel.removeAll();
		resolvepanel.validate();

		SemGenProgressBar progframe = new SemGenProgressBar("Comparing models...", true);
		// Only include the annotated data structures in the resolution process
		for(DataStructure ds1 : semsimmodel1.getDataStructures()){
			for(DataStructure ds2 : semsimmodel2.getDataStructures()){
				Boolean match = false;
				
				// Test singular annotations
				if(ds1.hasRefersToAnnotation() && ds2.hasRefersToAnnotation()) {
					match = testNonCompositeAnnotations(ds1.getFirstRefersToReferenceOntologyAnnotation(),
							ds2.getFirstRefersToReferenceOntologyAnnotation());
				}
				
				// If the physical properties are not null
				if(!match && ds1.getPhysicalProperty()!=null && ds2.getPhysicalProperty()!=null){
					// And they are properties of a specified physical model component
					if(ds1.getPhysicalProperty().getPhysicalPropertyOf()!=null && ds2.getPhysicalProperty().getPhysicalPropertyOf()!=null){
						PhysicalProperty prop1 = ds1.getPhysicalProperty();
						PhysicalProperty prop2 = ds2.getPhysicalProperty();
						
						// and they are annotated against reference ontologies
						if(prop1.hasRefersToAnnotation() && prop2.hasRefersToAnnotation()){
							// and the annotations match
							if(prop1.getFirstRefersToReferenceOntologyAnnotation().getReferenceURI().toString().equals(prop2.getFirstRefersToReferenceOntologyAnnotation().getReferenceURI().toString())){
								
								// and they are properties of the same kind of physical model component
								if(prop1.getPhysicalPropertyOf().getClass() == prop2.getPhysicalPropertyOf().getClass()){
									
									// if they are properties of a composite physical entity
									if(prop1.getPhysicalPropertyOf() instanceof CompositePhysicalEntity){
										CompositePhysicalEntity cpe1 = (CompositePhysicalEntity)prop1.getPhysicalPropertyOf();
										CompositePhysicalEntity cpe2 = (CompositePhysicalEntity)prop2.getPhysicalPropertyOf();
										match = testCompositePhysicalEntityEquivalency(cpe1, cpe2);
									}
									// if they are properties of a physical process or singular physical entity
									else{
										// and if they are both annotated against reference ontology terms
										if(prop1.getPhysicalPropertyOf().hasRefersToAnnotation() && prop2.getPhysicalPropertyOf().hasRefersToAnnotation()){
											// and if the annotations match
											if(prop1.getPhysicalPropertyOf().getFirstRefersToReferenceOntologyAnnotation().getReferenceURI().toString().equals(
													prop2.getPhysicalPropertyOf().getFirstRefersToReferenceOntologyAnnotation().getReferenceURI().toString())){
												match = true;
											}
										}
									}
								}
							}
						}
					}
				}
				if(match){
					resolvepanel.add(new ResolutionPanel(ds1, ds2,
							semsimmodel1, semsimmodel2, "(exact semantic match)", false));
					resolvepanel.add(new JSeparator());
					resolvepanel.validate();
					resolvepanel.repaint();
				}
			} // end of iteration through model2 data structures
		} // end of iteration through model1 data structures
		if (resolvepanel.getComponents().length==0) {
			SemGenError.showError("SemGen did not find any semantic equivalencies between the models", "Merger message");
		}
		else resolvepanel.remove(resolvepanel.getComponentCount()-1); // remove last JSeparator
		progframe.dispose();
	}

	public Boolean testNonCompositeAnnotations(ReferenceOntologyAnnotation ann1, ReferenceOntologyAnnotation ann2){
		return (ann1.getReferenceURI().toString().equals(ann2.getReferenceURI().toString()));
	}
	
	public Boolean testCompositePhysicalEntityEquivalency(CompositePhysicalEntity cpe1, CompositePhysicalEntity cpe2){
		if(cpe1.getArrayListOfEntities().size()!=cpe2.getArrayListOfEntities().size())
			return false;
		for(int i=0; i<cpe1.getArrayListOfEntities().size(); i++){
			if(cpe1.getArrayListOfEntities().get(i).hasRefersToAnnotation() && cpe2.getArrayListOfEntities().get(i).hasRefersToAnnotation()){
				if(!cpe1.getArrayListOfEntities().get(i).getFirstRefersToReferenceOntologyAnnotation().getReferenceURI().toString().equals( 
					cpe2.getArrayListOfEntities().get(i).getFirstRefersToReferenceOntologyAnnotation().getReferenceURI().toString())){

					return false;
				}
			}
			else return false;
		}
		return true;
	}

	public Boolean codewordsAlreadyMapped(String cdwd1uri, String cdwd2uri,
			Boolean prompt) {
		String cdwd1 = SemSimOWLFactory.getIRIfragment(cdwd1uri);
		String cdwd2 = SemSimOWLFactory.getIRIfragment(cdwd2uri);
		Boolean alreadymapped = false;
		Component[] resolutionpanels = this.resolvepanel.getComponents();
		for (int x = 0; x < resolutionpanels.length; x++) {
			if (resolutionpanels[x] instanceof ResolutionPanel) {
				ResolutionPanel rp = (ResolutionPanel) resolutionpanels[x];
				if ((cdwd1.equals(rp.ds1.getName()) && cdwd2.equals(rp.ds2.getName()))
						|| (cdwd1.equals(rp.ds2.getName()) && cdwd2.equals(rp.ds1.getName()))) {
					alreadymapped = true;
					if (prompt) {
						JOptionPane.showMessageDialog(this, cdwd1
								+ " and " + cdwd2 + " are already mapped");
					}
				}
			}
		}
		return alreadymapped;
	}

	public void merge() throws IOException, CloneNotSupportedException, OWLException, InterruptedException, JDOMException, Xcept {
		SemSimModel ssm1clone = semsimmodel1.clone();
		SemSimModel ssm2clone = semsimmodel2.clone();

		// First collect all the data structures that aren't going to be used in the resulting merged model
		// Include a mapping between the solution domains
		Component[] resolutionpanels = new Component[resolvepanel.getComponentCount()+1]; 
		for(int j=0; j<resolutionpanels.length-1;j++) resolutionpanels[j] = resolvepanel.getComponent(j);
		
		DataStructure soldom1 = ssm1clone.getSolutionDomains().toArray(new DataStructure[]{})[0];
		DataStructure soldom2 = ssm2clone.getSolutionDomains().toArray(new DataStructure[]{})[0];
		
		resolutionpanels[resolutionpanels.length-1] = new ResolutionPanel(soldom1, soldom2, ssm1clone, ssm2clone, 
				"automated solution domain mapping", false);
		
		SemSimModel modelfordiscardedds = null;
		for (int x = 0; x < resolutionpanels.length; x++) {
			ResolutionPanel rp = null;
			if ((resolutionpanels[x] instanceof ResolutionPanel)) {
				rp = (ResolutionPanel) resolutionpanels[x];
				DataStructure discardedds = null;
				DataStructure keptds = null;
				if (rp.rb1.isSelected() || rp.ds1.isSolutionDomain()){
					discardedds = rp.ds2;
					keptds = rp.ds1;
					modelfordiscardedds = ssm2clone;
				}
				else if(rp.rb2.isSelected()){
					discardedds = rp.ds1;
					keptds = rp.ds2;
					modelfordiscardedds = ssm1clone;
				}
				
				// If "ignore equivalency" is not selected"
				if(keptds!=null && discardedds !=null){
				
					// If we need to add in a unit conversion factor
					String replacementtext = keptds.getName();
					Boolean cancelmerge = false;
					double conversionfactor = 1;
					if(keptds.hasUnits() && discardedds.hasUnits()){
						if (!keptds.getUnit().getComputationalCode().equals(discardedds.getUnit().getComputationalCode())){
							ConversionFactorDialog condia = new ConversionFactorDialog(
									keptds.getName(), discardedds.getName(), keptds.getUnit().getComputationalCode(),
									discardedds.getUnit().getComputationalCode());
							replacementtext = condia.cdwdAndConversionFactor;
							conversionfactor = condia.conversionfactor;
							cancelmerge = !condia.process;
						}
					}
					
					if(cancelmerge) return;
					
					// if the two terms have different names, or a conversion factor is required
					if(!discardedds.getName().equals(keptds.getName()) || conversionfactor!=1){
						SemSimUtil.replaceCodewordInAllEquations(discardedds, keptds, modelfordiscardedds, discardedds.getName(), replacementtext, conversionfactor);
					}
					// What to do about sol doms that have different units?
					
					if(discardedds.isSolutionDomain()){
					  // Re-set the solution domain designations for all DataStructures in model 2
						for(DataStructure nsdds : ssm2clone.getDataStructures()){
							if(nsdds.hasSolutionDomain())
								nsdds.setSolutionDomain(soldom1);
						}
						// Remove .min, .max, .delta solution domain DataStructures
						modelfordiscardedds.removeDataStructure(discardedds.getName() + ".min");
						modelfordiscardedds.removeDataStructure(discardedds.getName() + ".max");
						modelfordiscardedds.removeDataStructure(discardedds.getName() + ".delta");
						identicaldsnames.remove(discardedds.getName() + ".min");
						identicaldsnames.remove(discardedds.getName() + ".max");
						identicaldsnames.remove(discardedds.getName() + ".delta");
					}
					
					// Remove the discarded Data Structure
					modelfordiscardedds.removeDataStructure(discardedds.getName());
					
					// If we are removing a state variable, remove its derivative, if present
					if(discardedds.hasSolutionDomain()){
						if(modelfordiscardedds.containsDataStructure(discardedds.getName() + ":" + discardedds.getSolutionDomain().getName())){
							modelfordiscardedds.removeDataStructure(discardedds.getName() + ":" + discardedds.getSolutionDomain().getName());
						}
					}
					
					// If the semantic resolution took care of a syntactic resolution
					if(!rp.rb3.isSelected()){
						identicaldsnames.remove(discardedds.getName());
					}
				}
			}
		}
		
		// Why isn't this working for Pandit-Hinch merge?
		// Prompt the user to resolve the points of SYNTACTIC overlap (same codeword names)
		for (String dsname : identicaldsnames) {
			Boolean cont = true;
			while(cont){
				String newdsname = JOptionPane.showInputDialog(this, "Both models contain codeword " + dsname + ".\n" +
						"Enter new name for use in " + file1.getName() + " equations.\nNo special characters, no spaces.", "Duplicate codeword", JOptionPane.OK_OPTION);
				if(newdsname!=null && !newdsname.equals("") && !newdsname.equals(dsname)){
					ssm1clone.getDataStructure(dsname).setName(newdsname);
					Boolean derivreplace = false;
					String derivname = null;
					
					// If there is a derivative of the data structure that we're renaming, rename it, too
					if(ssm1clone.getDataStructure(newdsname).hasSolutionDomain()){
						derivname = dsname + ":" + ssm1clone.getDataStructure(newdsname).getSolutionDomain().getName();
						if(ssm1clone.containsDataStructure(derivname)){
							ssm1clone.getDataStructure(derivname).setName(derivname.replace(dsname, newdsname));
							derivreplace = true;
						}
					}
					// Use the new name in all the equations
					SemSimUtil.replaceCodewordInAllEquations(ssm1clone.getDataStructure(newdsname), ssm1clone.getDataStructure(newdsname),
							ssm1clone, dsname, newdsname, 1);
					
					// IS THERE AN ISSUE WITH SELF_REF_ODEs HERE?
					if(derivreplace){
						SemSimUtil.replaceCodewordInAllEquations(ssm1clone.getDataStructure(derivname.replace(dsname, newdsname)),
								ssm1clone.getDataStructure(derivname.replace(dsname, newdsname)),
								ssm1clone, derivname, derivname.replace(dsname, newdsname), 1);
					}
					cont = false;
				}
				else if(newdsname.equals(dsname)){
					JOptionPane.showMessageDialog(this, "That is the existing name. Please choose a new one.");
				}
			}
		}
		
		// What if both models have a custom phys component with the same name?
		SemSimModel mergedmodel = ssm1clone;
		
		// Create submodels representing the merged components, copy over all info from model2 into model1
		if(ssm1clone.getSolutionDomains().size()<=1 && ssm2clone.getSolutionDomains().size()<=1){
			
			Submodel sub1 = new Submodel(ssm1clone.getName());
			sub1.setAssociatedDataStructures(ssm1clone.getDataStructures());
			sub1.setSubmodels(ssm1clone.getSubmodels());
			
			Submodel sub2 = new Submodel(ssm2clone.getName());
			sub2.setAssociatedDataStructures(ssm2clone.getDataStructures());
			sub2.addDataStructure(soldom1);
			
			if(ssm1clone.containsDataStructure(soldom1.getName() + ".min"))
				sub2.addDataStructure(ssm1clone.getDataStructure(soldom1.getName() + ".min"));
			if(ssm1clone.containsDataStructure(soldom1.getName() + ".max"))
				sub2.addDataStructure(ssm1clone.getDataStructure(soldom1.getName() + ".max"));
			if(ssm1clone.containsDataStructure(soldom1.getName() + ".delta"))
				sub2.addDataStructure(ssm1clone.getDataStructure(soldom1.getName() + ".delta"));
			
			sub2.setSubmodels(ssm2clone.getSubmodels());
			mergedmodel.addSubmodel(sub1);
			mergedmodel.addSubmodel(sub2);
			
			// Copy in all data structures
			for(DataStructure dsfrom2 : ssm2clone.getDataStructures()){
				mergedmodel.addDataStructure(dsfrom2);
			}
			
			// Copy in the units
			mergedmodel.getUnits().addAll(ssm2clone.getUnits());
			
			// Copy in the submodels
			for(Submodel subfrom2 : ssm2clone.getSubmodels()){
				mergedmodel.addSubmodel(subfrom2);
			}
			
			// MIGHT NEED TO COPY IN PHYSICAL MODEL COMPONENTS?
		}
		else{
			SemGenError.showError(
					"ERROR: One of the models to be merged has multiple solution domains.\nMerged model not saved.","Merge Failed");
			return;
		}
		
		// WHAT TO DO ABOUT ONTOLOGY-LEVEL ANNOTATIONS?
		
		mergedmodel.setNamespace(mergedmodel.generateNamespaceFromDateAndTime());
		manager.saveOntology(mergedmodel.toOWLOntology(), new RDFXMLOntologyFormat(), IRI.create(mergedfile));
		optionToEncode(mergedmodel);
		refreshModelsToMerge();
	}

	public void optionToEncode(SemSimModel model) throws IOException, OWLException {
		int x = JOptionPane.showConfirmDialog(this, "Finished merging "
				+ mergedfile.getName()
				+ "\nGenerate simulation code from merged model?", "",
				JOptionPane.YES_NO_OPTION);
		if (x == JOptionPane.YES_OPTION) {
			new Encoder(model, mergedfile.getName().substring(0, mergedfile.getName().lastIndexOf(".")));
		}
	}
	
	public Set<SemSimModel> refreshModelsToMerge() {	
		FileToMergeLabel label1 = (FileToMergeLabel) filelistpanel.getComponent(0);
		FileToMergeLabel label2 = (FileToMergeLabel) filelistpanel.getComponent(1);
		file1 = new File(label1.getText());
		file2 = new File(label2.getText());
		
		semsimmodel1 = LoadSemSimModel.loadSemSimModelFromFile(file1, settings.doAutoAnnotate());
		
		if(semsimmodel1.getFunctionalSubmodels().size()>0) SemGenError.showFunctionalSubmodelError(file1);
		
		semsimmodel2 = LoadSemSimModel.loadSemSimModelFromFile(file2, settings.doAutoAnnotate());
		
		if(semsimmodel2.getFunctionalSubmodels().size()>0) 
			SemGenError.showFunctionalSubmodelError(file1);
		
		Set<SemSimModel> models = new HashSet<SemSimModel>();
		models.add(semsimmodel1);
		models.add(semsimmodel2);
		return models;
	}

	public void mouseEntered(MouseEvent arg0) {}

	public void mouseExited(MouseEvent arg0) {}

	public void mousePressed(MouseEvent arg0) {
		if (arg0.getSource() instanceof FileToMergeLabel
				&& arg0.getClickCount() > 0) {
			FileToMergeLabel ftml = (FileToMergeLabel) arg0.getSource();
			arg0.consume();
			ftml.setSelected(true);

			for (Component comp : filelistpanel.getComponents()) {
				if (comp instanceof FileToMergeLabel) {
					FileToMergeLabel otherftml = (FileToMergeLabel) comp;
					otherftml.setSelected(!otherftml.filepath.equals(ftml.filepath));
				}
			}
		}
		filelistpanel.validate();
		filelistpanel.repaint();
	}
	
	public void mouseClicked(MouseEvent arg0) {}
	public void mouseReleased(MouseEvent arg0) {}

	@Override
	public boolean isSaved() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void requestSave() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void requestSaveAs() {
		// TODO Auto-generated method stub
		
	}
	
	public File saveMerge() {
		SemGenSaveFileChooser filec = new SemGenSaveFileChooser("Choose location to save file", 
				new String[]{"cellml","owl"});
		if (filec.SaveAsAction()!=null) {
			mergedfile = filec.getSelectedFile();
			return mergedfile;
		}
		return null;
	}

	@Override
	public void addObservertoWorkbench(Observer obs) {
		// TODO Auto-generated method stub
		
	}
}