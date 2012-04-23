package workcraft.mg;

import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Container;
import java.awt.Frame;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JButton;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import javax.swing.JCheckBox;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;

public class MGAnalyserFrame extends JDialog {

	private static final long serialVersionUID = 1L;

	private JPanel jContentPane = null;

	private JTextArea txtReport = null;

	private JButton btnOK = null;

	private JCheckBox chkPlaces = null;

	private JCheckBox chkTransitions = null;
	
	private JFrame parent;

	private JCheckBox chkConnections = null;
	
	protected MGModel doc;
	
	/**
	 * @param owner
	 */
	public MGAnalyserFrame(JFrame parent)
	{
		super(parent);

		this.parent = parent;

		initialize();

		int x;
		int y;

		Container myParent = getParent();
		java.awt.Point topLeft = myParent.getLocationOnScreen();
		Dimension parentSize = myParent.getSize();

		Dimension mySize = getSize();

		if (parentSize.width > mySize.width) 
			x = ((parentSize.width - mySize.width)/2) + topLeft.x;
		else 
			x = topLeft.x;

		if (parentSize.height > mySize.height) 
			y = ((parentSize.height - mySize.height)/2) + topLeft.y;
		else 
			y = topLeft.y;

		setLocation (x, y);
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize()
	{
		this.setSize(504, 368);
		this.setTitle("Marked Graph Properties Analyser");
		this.setContentPane(getJContentPane());
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
			gridBagConstraints11.gridx = 0;
			gridBagConstraints11.anchor = GridBagConstraints.WEST;
			gridBagConstraints11.insets = new Insets(0, 20, 0, 0);
			gridBagConstraints11.gridy = 3;
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.gridx = 0;
			gridBagConstraints4.anchor = GridBagConstraints.WEST;
			gridBagConstraints4.insets = new Insets(0, 20, 0, 0);
			gridBagConstraints4.gridy = 2;
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.gridx = 0;
			gridBagConstraints3.anchor = GridBagConstraints.WEST;
			gridBagConstraints3.insets = new Insets(0, 20, 0, 0);
			gridBagConstraints3.gridy = 1;
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.gridx = 0;
			gridBagConstraints1.gridwidth = 1;
			gridBagConstraints1.weightx = 0.0;
			gridBagConstraints1.fill = GridBagConstraints.NONE;
			gridBagConstraints1.insets = new Insets(3, 3, 3, 3);
			gridBagConstraints1.anchor = GridBagConstraints.CENTER;
			gridBagConstraints1.gridy = 4;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.fill = GridBagConstraints.BOTH;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.weighty = 1.0;
			gridBagConstraints.gridheight = 1;
			gridBagConstraints.insets = new Insets(3, 3, 3, 3);
			gridBagConstraints.anchor = GridBagConstraints.NORTH;
			gridBagConstraints.gridwidth = 1;
			gridBagConstraints.gridx = 0;
			jContentPane = new JPanel();
			jContentPane.setLayout(new GridBagLayout());
			jContentPane.add(getTxtReport(), gridBagConstraints);
			jContentPane.add(getBtnOK(), gridBagConstraints1);
			jContentPane.add(getChkPlaces(), gridBagConstraints3);
			jContentPane.add(getChkTransitions(), gridBagConstraints4);
			jContentPane.add(getChkConnections(), gridBagConstraints11);
		}
		return jContentPane;
	}

	/**
	 * This method initializes txtReport	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	protected JTextArea getTxtReport() {
		if (txtReport == null) {
			txtReport = new JTextArea();
			txtReport.setBorder(new SoftBevelBorder(SoftBevelBorder.LOWERED));
		}
		return txtReport;
	}

	/**
	 * This method initializes btnOK	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBtnOK() {
		if (btnOK == null) {
			btnOK = new JButton();
			btnOK.setText("OK");
			btnOK.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					setVisible(false);
				}
			});
		}
		return btnOK;
	}
	
	public void go()
	{
		setModal(true);
		setVisible(true);
	}

	/**
	 * This method initializes chkPlaces	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	protected JCheckBox getChkPlaces() {
		if (chkPlaces == null) {
			chkPlaces = new JCheckBox();
			chkPlaces.setText("show critical places");
			chkPlaces.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					MGPlace.show_critical = chkPlaces.isSelected();
					parent.repaint();
				}
			});
		}
		return chkPlaces;
	}

	/**
	 * This method initializes chkTransitions	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	protected JCheckBox getChkTransitions() {
		if (chkTransitions == null) {
			chkTransitions = new JCheckBox();
			chkTransitions.setText("show critical transitions");
			chkTransitions.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					MGTransition.show_critical = chkTransitions.isSelected();
					parent.repaint();
				}
			});
		}
		return chkTransitions;
	}

	/**
	 * This method initializes chkConnections	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	protected JCheckBox getChkConnections() {
		if (chkConnections == null) {
			chkConnections = new JCheckBox();
			chkConnections.setText("show critical connections");
			chkConnections.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					MGConnection.show_critical = chkConnections.isSelected();
					doc.updateConnectionsColor();
					parent.repaint();
				}
			});
		}
		return chkConnections;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
