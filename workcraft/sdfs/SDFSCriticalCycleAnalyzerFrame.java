package workcraft.sdfs;

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

import workcraft.mg.MGModel;
import workcraft.mg.MGPlace;

public class SDFSCriticalCycleAnalyzerFrame extends JDialog {

	private static final long serialVersionUID = 1L;

	private JPanel jContentPane = null;

	private JTextArea txtReport = null;

	private JButton btnOK = null;

	private JFrame parent;

	protected MGModel doc;
	
	/**
	 * @param owner
	 */
	public SDFSCriticalCycleAnalyzerFrame(JFrame parent)
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
			txtReport.setLineWrap(true);
			txtReport.setText("");
			txtReport.setWrapStyleWord(true);
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

}  //  @jve:decl-index=0:visual-constraint="10,10"
