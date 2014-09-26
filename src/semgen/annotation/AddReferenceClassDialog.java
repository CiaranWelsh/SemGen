package semgen.annotation;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import semgen.SemGenGUI;
import semsim.Annotatable;

public class AddReferenceClassDialog extends JDialog implements
		PropertyChangeListener {

	private static final long serialVersionUID = -3830623199860161812L;
	public ReferenceClassFinderPanel refclasspanel;
	public Annotator annotator;
	public JOptionPane optionPane;
	public Object[] options;
	public JTextArea utilarea;

	public AddReferenceClassDialog(Annotator ann, String[] ontList, Object[] options, Annotatable annotatable) {
		this.annotator = ann;
		setTitle("Select reference concept");
		refclasspanel = new ReferenceClassFinderPanel(annotator, annotatable, ontList);
		refclasspanel.rightscrollerapplybutton.setEnabled(false);
		refclasspanel.leftscrollerapplybutton.setEnabled(false);
		this.options = options;

		utilarea = new JTextArea();
		utilarea.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		utilarea.setBackground(new Color(0,0,0,0));
		utilarea.setLineWrap(true);
		utilarea.setWrapStyleWord(true);
		//utilarea.setOpaque(false);
		utilarea.setEditable(false);
		utilarea.setFont(new Font("SansSerif", Font.BOLD, SemGenGUI.defaultfontsize - 1));

		Object[] array = { utilarea, refclasspanel };

		optionPane = new JOptionPane(array, JOptionPane.PLAIN_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION, null);
		optionPane.addPropertyChangeListener(this);
		this.options = options;
		optionPane.setOptions(options);
		optionPane.setInitialValue(options[0]);
		setContentPane(optionPane);
	}
	
	public void packAndSetModality(){
		setModalityType(ModalityType.APPLICATION_MODAL);
		pack();
		setLocationRelativeTo(SemGenGUI.desktop);
		setVisible(true);
	}
	

	public void propertyChange(PropertyChangeEvent arg0) {
		String value = optionPane.getValue().toString();
		String selectedname = (String) refclasspanel.resultslistright.getSelectedValue();

		if (value == "Add as entity" && this.getFocusOwner() != refclasspanel.findbox) {
			annotator.semsimmodel.addReferencePhysicalEntity(URI.create(refclasspanel.resultsanduris.get(selectedname)), selectedname);
			annotator.setModelSaved(false);
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
			JOptionPane.showMessageDialog(this,"Added " + (String) refclasspanel.resultslistright.getSelectedValue() + " as reference physical enitity", "",
					JOptionPane.PLAIN_MESSAGE);
			annotator.setModelSaved(false);
		}
		else if(value == "Add as process" && this.getFocusOwner() != refclasspanel.findbox){
			annotator.semsimmodel.addReferencePhysicalProcess(URI.create(refclasspanel.resultsanduris.get(selectedname)), selectedname);
			annotator.setModelSaved(false);
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
			JOptionPane.showMessageDialog(this,"Added " + (String) refclasspanel.resultslistright.getSelectedValue() + " as reference physical process", "",
					JOptionPane.PLAIN_MESSAGE);
			annotator.setModelSaved(false);
		}
		else if (value == "Close") {
			optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
			this.dispose();
		}
		if(annotator.focusbutton instanceof CodewordButton) annotator.anndialog.compositepanel.refreshUI();
	}
}
