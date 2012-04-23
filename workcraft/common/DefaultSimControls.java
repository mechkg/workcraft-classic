package workcraft.common;

import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import java.awt.GridBagConstraints;
import java.awt.FlowLayout;
import javax.swing.JRadioButton;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import java.awt.Font;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JButton;

import workcraft.petri.EditablePetriPlace;
import workcraft.petri.EditablePetriTransition;
import workcraft.petri.GFileFilter;
import workcraft.sdfs.WTFileFilter;
import javax.swing.JSlider;

public class DefaultSimControls extends JPanel {

	private static final long serialVersionUID = 1L;
	private JTabbedPane panelControls = null;
	private JRadioButton btnInterSim = null;
	private JRadioButton btnTraceSim = null;
	private JPanel panelMode = null;
	private JRadioButton btnAutoSim = null;
	private JCheckBox chkWriteTrace = null;
	private JPanel panelTrace = null;
	private JTextArea jTextArea = null;
	private JPanel panelTraceButtons = null;
	private JButton btnLoadTrace = null;
	private JButton btnSaveTrace = null;
	private JScrollPane scrollTrace = null;
	private JTextArea txtTrace = null;
	
	String parentModelUUID;
	
	private boolean traceWriteRestore = false;
	
	Trace trace = new Trace();

	
	private JCheckBox chkStepEnabled = null;
	private JSlider slSpeed = null;
	private JLabel lblSpeed = null;
	private JLabel lblSlower = null;
	private JLabel lblFaster = null;

	/**
	 * This is the default constructor
	 */
	public DefaultSimControls(String parentModelUUID) {
		super();
		this.parentModelUUID = parentModelUUID;
		initialize();
	}
	
	public boolean isTraceWriteEnabled() {
		return chkWriteTrace.isSelected();
	}
	
	public boolean isTraceReplayEnabled() {
		return btnTraceSim.isSelected();
	}
	
	public boolean isStepByStepEnabled() {
		return chkStepEnabled.isSelected();
	}

	public void addTraceEvent(String event) {
		trace.addEvent(event);
		
		String s = txtTrace.getText();
		if (s.length() > 0)
			s += ";";
		s += event;
		
		txtTrace.setText(s);
	}

	public void setTrace (String trace) {
		this.trace.parseTrace(trace);
		txtTrace.setText(this.trace.toString());
	};

	public void saveTrace() {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new WTFileFilter());
		if (fc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
		{
			String path = fc.getSelectedFile().getPath();
			if (!path.endsWith(".wt")) path += ".wt";
			{
				try {
					File f = new File (path);
					PrintWriter out = new PrintWriter(new FileWriter(path));
					out.println(parentModelUUID);
					out.print(trace.toString());
					out.close();
				}
				catch (IOException e)
				{
					JOptionPane.showMessageDialog(null, "File could not be opened for writing.");
					return;
				}					
			}
		}		
	}

	public void loadTrace() {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new WTFileFilter());
		if (fc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
		{
			String path = fc.getSelectedFile().getPath();
			if (!path.endsWith(".wt")) path += ".wt";
			{
				try {
					File f = new File (path);
					BufferedReader in = new BufferedReader(new FileReader(f));
					String k = in.readLine();
					
					if (!k.equals(parentModelUUID)) {
						JOptionPane.showMessageDialog(null, "This trace contains information for a different model.");
						return;
					}
					
					k = in.readLine();
					
					
					setTrace(k);
				}
				catch (IOException e)
				{
					JOptionPane.showMessageDialog(null, "File could not be opened for writing.");
					return;
				}					
			}
		}	
	}
	
	public void clearTrace() {
		trace.clear();
		txtTrace.setText("");
	}
	
	public Trace getTrace() {
		return trace;
	}
	
	public double getSpeedFactor() {
		return slSpeed.getValue() / 1000.0;
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
		gridBagConstraints4.fill = GridBagConstraints.BOTH;
		gridBagConstraints4.gridy = 0;
		gridBagConstraints4.weightx = 1.0;
		gridBagConstraints4.weighty = 1.0;
		gridBagConstraints4.gridx = 0;
		this.setSize(300, 200);
		this.setLayout(new GridBagLayout());
		this.add(getPanelControls(), gridBagConstraints4);

		ButtonGroup g = new ButtonGroup();
		g.add(getBtnAutoSim());
		g.add(getBtnInterSim());
		g.add(getBtnTraceSim());
	}

	public boolean isUserInteractionEnabled() {
		return getBtnInterSim().isSelected();
	}

	/**
	 * This method initializes panelTrace	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getPanelTrace() {
		if (panelTrace == null) {
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.insets = new Insets(6, 3, 173, 46);
			gridBagConstraints2.gridy = 0;
			gridBagConstraints2.gridx = 1;
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.anchor = GridBagConstraints.NORTH;
			gridBagConstraints1.gridx = 0;
			gridBagConstraints1.gridy = 0;
			gridBagConstraints1.ipadx = 2;
			gridBagConstraints1.ipady = 2;
			gridBagConstraints1.insets = new Insets(2, 2, 2, 2);
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
			gridBagConstraints.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints.gridheight = 1;
			gridBagConstraints.gridwidth = 1;
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.ipadx = 2;
			gridBagConstraints.ipady = 2;
			gridBagConstraints.fill = GridBagConstraints.NONE;
			panelTrace.add(getBtnAutoSim(), null);
			panelTrace.add(getBtnInterSim(), null);
			panelTrace.add(getBtnTraceSim(), null);
		}
		return panelTrace;
	}

	/**
	 * This method initializes panelControls	
	 * 	
	 * @return javax.swing.JTabbedPane	
	 */
	private JTabbedPane getPanelControls() {
		if (panelControls == null) {
			panelControls = new JTabbedPane();
			panelControls.addTab("Mode", null, getPanelMode(), null);
			panelControls.addTab("Trace", null, getJPanel2(), null);
		}
		return panelControls;
	}

	/**
	 * This method initializes btnInterSim	
	 * 	
	 * @return javax.swing.JRadioButton	
	 */
	private JRadioButton getBtnInterSim() {
		if (btnInterSim == null) {
			btnInterSim = new JRadioButton();
			btnInterSim.setSelected(true);
			btnInterSim.setText("Interactive");
		}
		return btnInterSim;
	}

	/**
	 * This method initializes btnTraceSim	
	 * 	
	 * @return javax.swing.JRadioButton	
	 */
	private JRadioButton getBtnTraceSim() {
		if (btnTraceSim == null) {
			btnTraceSim = new JRadioButton();
			btnTraceSim.setText("Trace replay");
			btnTraceSim.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
				

				}
			});
			btnTraceSim.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					if (btnTraceSim.isSelected()){
						traceWriteRestore = chkWriteTrace.isSelected();
						chkWriteTrace.setSelected(false);
						chkWriteTrace.setEnabled(false);
					} else {
						chkWriteTrace.setEnabled(true);
						chkWriteTrace.setSelected(traceWriteRestore);
					}
				}
			});
		}
		return btnTraceSim;
	}

	/**
	 * This method initializes panelMode	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getPanelMode() {
		if (panelMode == null) {
			GridBagConstraints gridBagConstraints15 = new GridBagConstraints();
			gridBagConstraints15.gridx = 2;
			gridBagConstraints15.anchor = GridBagConstraints.EAST;
			gridBagConstraints15.insets = new Insets(0, 0, 0, 20);
			gridBagConstraints15.gridy = 3;
			lblFaster = new JLabel();
			lblFaster.setText("faster");
			GridBagConstraints gridBagConstraints14 = new GridBagConstraints();
			gridBagConstraints14.gridx = 1;
			gridBagConstraints14.anchor = GridBagConstraints.WEST;
			gridBagConstraints14.gridy = 3;
			lblSlower = new JLabel();
			lblSlower.setText("slower");
			GridBagConstraints gridBagConstraints13 = new GridBagConstraints();
			gridBagConstraints13.gridx = 0;
			gridBagConstraints13.anchor = GridBagConstraints.EAST;
			gridBagConstraints13.fill = GridBagConstraints.NONE;
			gridBagConstraints13.insets = new Insets(10, 20, 0, 0);
			gridBagConstraints13.gridy = 2;
			lblSpeed = new JLabel();
			lblSpeed.setText("Auto sim speed:");
			GridBagConstraints gridBagConstraints12 = new GridBagConstraints();
			gridBagConstraints12.fill = GridBagConstraints.BOTH;
			gridBagConstraints12.gridy = 2;
			gridBagConstraints12.weightx = 1.0;
			gridBagConstraints12.gridwidth = 2;
			gridBagConstraints12.anchor = GridBagConstraints.WEST;
			gridBagConstraints12.ipady = 0;
			gridBagConstraints12.insets = new Insets(20, 0, 0, 20);
			gridBagConstraints12.gridx = 1;
			GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
			gridBagConstraints11.gridx = 2;
			gridBagConstraints11.anchor = GridBagConstraints.WEST;
			gridBagConstraints11.insets = new Insets(0, 0, 0, 20);
			gridBagConstraints11.gridy = 1;
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.gridx = 1;
			gridBagConstraints3.gridy = 1;
			GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.gridx = 2;
			gridBagConstraints7.anchor = GridBagConstraints.WEST;
			gridBagConstraints7.insets = new Insets(20, 0, 0, 20);
			gridBagConstraints7.gridy = 0;
			GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.gridx = 1;
			gridBagConstraints6.insets = new Insets(20, 0, 0, 0);
			gridBagConstraints6.gridy = 0;
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.gridx = 0;
			gridBagConstraints5.anchor = GridBagConstraints.EAST;
			gridBagConstraints5.insets = new Insets(20, 20, 0, 0);
			gridBagConstraints5.gridy = 0;
			panelMode = new JPanel();
			panelMode.setLayout(new GridBagLayout());
			panelMode.add(getBtnAutoSim(), gridBagConstraints5);
			panelMode.add(getBtnInterSim(), gridBagConstraints6);
			panelMode.add(getBtnTraceSim(), gridBagConstraints7);
			panelMode.add(getChkWriteTrace(), gridBagConstraints3);
			panelMode.add(getChkStepEnabled(), gridBagConstraints11);
			panelMode.add(getSlSpeed(), gridBagConstraints12);
			panelMode.add(lblSpeed, gridBagConstraints13);
			panelMode.add(lblSlower, gridBagConstraints14);
			panelMode.add(lblFaster, gridBagConstraints15);
		}
		return panelMode;
	}

	/**
	 * This method initializes btnAutoSim	
	 * 	
	 * @return javax.swing.JRadioButton	
	 */
	private JRadioButton getBtnAutoSim() {
		if (btnAutoSim == null) {
			btnAutoSim = new JRadioButton();
			btnAutoSim.setText("Automatic");
		}
		return btnAutoSim;
	}

	/**
	 * This method initializes chkWriteTrace	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getChkWriteTrace() {
		if (chkWriteTrace == null) {
			chkWriteTrace = new JCheckBox();
			chkWriteTrace.setText("Write trace");
		}
		return chkWriteTrace;
	}

	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel2() {
		if (panelTrace == null) {
			GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
			gridBagConstraints10.gridy = 1;
			GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
			gridBagConstraints8.fill = GridBagConstraints.BOTH;
			gridBagConstraints8.gridy = 0;
			gridBagConstraints8.weightx = 1.0;
			gridBagConstraints8.weighty = 1.0;
			gridBagConstraints8.gridx = 0;
			panelTrace = new JPanel();
			panelTrace.setLayout(new GridBagLayout());
			panelTrace.add(getJPanel22(), gridBagConstraints10);
			panelTrace.add(getScrollTrace(), gridBagConstraints8);
		}
		return panelTrace;
	}

	/**
	 * This method initializes jTextArea	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getJTextArea() {
		if (jTextArea == null) {
			jTextArea = new JTextArea();
		}
		return jTextArea;
	}

	/**
	 * This method initializes jPanel2	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel22() {
		if (panelTraceButtons == null) {
			GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
			gridBagConstraints9.gridx = 0;
			gridBagConstraints9.gridy = 0;
			panelTraceButtons = new JPanel();
			panelTraceButtons.setLayout(new GridBagLayout());
			panelTraceButtons.add(getBtnLoadTrace(), gridBagConstraints9);
			panelTraceButtons.add(getBtnSaveTrace(), new GridBagConstraints());
		}
		return panelTraceButtons;
	}

	/**
	 * This method initializes btnLoadTrace	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBtnLoadTrace() {
		if (btnLoadTrace == null) {
			btnLoadTrace = new JButton();
			btnLoadTrace.setText("Load trace...");
			btnLoadTrace.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					loadTrace();
				}
			});
		}
		return btnLoadTrace;
	}

	/**
	 * This method initializes btnSaveTrace	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBtnSaveTrace() {
		if (btnSaveTrace == null) {
			btnSaveTrace = new JButton();
			btnSaveTrace.setText("Save trace...");
			btnSaveTrace.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					saveTrace();
				}
			});
		}
		return btnSaveTrace;
	}

	/**
	 * This method initializes scrollTrace	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getScrollTrace() {
		if (scrollTrace == null) {
			scrollTrace = new JScrollPane();
			scrollTrace.setViewportView(getTxtTrace());
		}
		return scrollTrace;
	}

	/**
	 * This method initializes txtTrace	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getTxtTrace() {
		if (txtTrace == null) {
			txtTrace = new JTextArea();
			txtTrace.setEditable(false);
			txtTrace.setWrapStyleWord(true);
			txtTrace.setFont(new Font("Dialog", Font.PLAIN, 12));
			txtTrace.setForeground(new Color(50, 51, 170));
			txtTrace.setLineWrap(true);
		}
		return txtTrace;
	}

	/**
	 * This method initializes chkStepEnabled	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getChkStepEnabled() {
		if (chkStepEnabled == null) {
			chkStepEnabled = new JCheckBox();
			chkStepEnabled.setText("Step-by-step");
		}
		return chkStepEnabled;
	}

	/**
	 * This method initializes slSpeed	
	 * 	
	 * @return javax.swing.JSlider	
	 */
	private JSlider getSlSpeed() {
		if (slSpeed == null) {
			slSpeed = new JSlider();
			slSpeed.setPaintTrack(true);
			slSpeed.setPaintLabels(false);
			slSpeed.setMajorTickSpacing(100);
			slSpeed.setMinimum(100);
			slSpeed.setMaximum(1000);
			slSpeed.setValue(450);
			slSpeed.setPaintTicks(true);
		}
		return slSpeed;
	}
}
