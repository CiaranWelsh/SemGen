package semgen.annotation.composites;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import semgen.SemGenGUI;
import semgen.SemGenScrollPane;
import semgen.annotation.SemSimComponentSelectorDialog;
import semgen.resource.SemGenIcon;
import semsim.SemSimConstants;
import semsim.model.SemSimComponent;
import semsim.model.SemSimModel;
import semsim.model.annotation.SemSimRelation;
import semsim.model.physical.PhysicalEntity;
import semsim.model.physical.PhysicalProcess;
import semsim.model.physical.ProcessParticipant;

public class ProcessParticipantEditor extends JPanel implements ActionListener, PropertyChangeListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5639851498722801279L;
	public SemSimModel model;
	public SemSimRelation relation;
	public PhysicalProcess process;
	public JPanel headerpanel;
	public JLabel headerlabel;
	public JButton plusbutton = new JButton(SemGenIcon.plusicon);
	public JButton minusbutton = new JButton(SemGenIcon.minusicon);
	public SemGenScrollPane scroller;
	public JTable table;
	public ProcessParticipantTableModel tablemod;
	public String[] columnNames = {"Physical entity", "Multiplier"};
	public Map<String,ProcessParticipant> namesandparticipantmap = new HashMap<String,ProcessParticipant>();
	public SemSimComponentSelectorDialog sscsd; 
	public int scrollerwidth = 550;
	public int multcolwidth = 100;

	public ProcessParticipantEditor(SemSimModel model, SemSimRelation relation, PhysicalProcess process) {	
		this.model = model;
		this.relation = relation;
		this.process = process;
		
		headerlabel = new JLabel(relation.getName());

		plusbutton.addActionListener(this);
		plusbutton.setToolTipText("Add process participant");

		minusbutton.addActionListener(this);
		minusbutton.setToolTipText("Remove selected process participant");

		headerpanel = new JPanel();
		headerpanel.setOpaque(false);
		headerpanel.add(headerlabel);
		headerpanel.add(plusbutton);
		headerpanel.add(minusbutton);
		
		setTableData();
		
		scroller = new SemGenScrollPane(table);
		scroller.setPreferredSize(new Dimension(scrollerwidth, 100));
		scroller.getViewport().setBackground(Color.white);
		
		setLayout(new BorderLayout());
		add(headerpanel, BorderLayout.NORTH);
		add(scroller, BorderLayout.SOUTH);
	}
	
	public void setTableData(){
		Set<ProcessParticipant> participants = new HashSet<ProcessParticipant>();
		if(relation == SemSimConstants.HAS_SOURCE_RELATION) participants.addAll(process.getSources());
		else if(relation == SemSimConstants.HAS_SINK_RELATION) participants.addAll(process.getSinks());
		else if(relation == SemSimConstants.HAS_MEDIATOR_RELATION) participants.addAll(process.getMediators());
		
		ArrayList<ProcessParticipant> temp = new ArrayList<ProcessParticipant>();
		for(ProcessParticipant pp : participants){
			temp.add(pp);
		}
		tablemod = new ProcessParticipantTableModel(temp);
		table = new JTable(tablemod);
		table.setFillsViewportHeight(false);
	}
	
	public class ProcessParticipantTableModel extends AbstractTableModel{

		private static final long serialVersionUID = 1L;
		private String[] columnNames = new String[]{"Physical entity", "Multiplier"};
		public ArrayList<ProcessParticipant> data = new ArrayList<ProcessParticipant>();
		
		public ProcessParticipantTableModel(ArrayList<ProcessParticipant> data){
			this.data = data;
		}
		
		public int getColumnCount() {
			return columnNames.length;
		}

		public int getRowCount() {
			return data.size();
		}
		
		public String getColumnName(int col) {
		      return columnNames[col];
	    }

		public Object getValueAt(int rowIndex, int columnIndex) {
			ProcessParticipant pp = data.get(rowIndex);
			if(columnIndex==0) return pp.getPhysicalEntity().getName();
			else return pp.getMultiplier();
		}
		
		@Override
		public boolean isCellEditable(int x, int y){
			return (y==1);
		}
		
		public void addRow(Object[] rowData){
			ProcessParticipant pp = ((ProcessParticipant)rowData[0]);
			data.add(pp);
			fireTableRowsInserted(getRowCount(), getRowCount());
		}
		
		public void removeRows(int[] rows){
			for(int x : rows){
				data.remove(x);
				fireTableRowsDeleted(x, x);
			}
		}
		
		@Override
		public void setValueAt(Object value, int row, int col){
			if(col==0){
				data.get(row).setPhysicalEntity((PhysicalEntity)value);
			}
			else if(col==1){
				double val = 1.0;
				try{
					val = Double.parseDouble((String)value);
				}
				catch(NumberFormatException ex){
					JOptionPane.showMessageDialog(SemGenGUI.desktop, "Multiplier not a valid number.");
					return;
				}
				data.get(row).setMultiplier(val);
			}
		    fireTableCellUpdated(row, col);
		}
	}
	
	

	public void actionPerformed(ActionEvent ae) {
		Object o = ae.getSource();
		if (o == plusbutton) {		
			Set<SemSimComponent> sscs = new HashSet<SemSimComponent>();
			for(String ssctempname : namesandparticipantmap.keySet()){
				sscs.add((SemSimComponent) namesandparticipantmap.get(ssctempname));
			}
			PhysicalEntity unspecent = model.getCustomPhysicalEntityByName(SemGenGUI.unspecifiedName);
			sscsd = new SemSimComponentSelectorDialog(model.getPhysicalEntitiesAndExclude(unspecent), null, sscs, null, false, "Physical entities in model");
			sscsd.setUpUI(this);
		}
		
		if (o == minusbutton) {
			tablemod.removeRows(table.getSelectedRows());
		}
	}

	public void propertyChange(PropertyChangeEvent evt) {
		String value = sscsd.optionPane.getValue().toString();
		if (value == "OK") {
			namesandparticipantmap.clear();
			for(Component c : sscsd.panel.getComponents()){
				if(c instanceof JCheckBox){
					JCheckBox box = (JCheckBox)c;
					if(box.isSelected()){
						ProcessParticipant pp = new ProcessParticipant((PhysicalEntity) sscsd.nameobjectmap.get(box.getText()), 1.0);
						tablemod.addRow(new Object[]{pp});
					}
				}
			}
			sscsd.optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
		}
		if (value == "Cancel") {
			sscsd.optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
			
		}
		sscsd.setVisible(false);
	}
}
