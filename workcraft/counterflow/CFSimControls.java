package workcraft.counterflow;

import java.awt.GridBagLayout;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import java.awt.GridBagConstraints;
import java.awt.FlowLayout;

public class CFSimControls extends JPanel {

	private static final long serialVersionUID = 1L;
	private JCheckBox chkUserInteractionEnabled = null;
	/**
	 * This is the default constructor
	 */
	public CFSimControls() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(300, 200);
		this.setLayout(new FlowLayout());
		this.add(getChkUserInteractionEnabled(), null);
	}

	/**
	 * This method initializes chkUserInteractionEnabled	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getChkUserInteractionEnabled() {
		if (chkUserInteractionEnabled == null) {
			chkUserInteractionEnabled = new JCheckBox();
			chkUserInteractionEnabled.setText("Enable user interaction");
			chkUserInteractionEnabled.setSelected(true);
		}
		return chkUserInteractionEnabled;
	}
	
	public boolean isUserInteractionEnabled() {
		if (chkUserInteractionEnabled != null)
			return chkUserInteractionEnabled.isSelected();
		else
			return false;
	}
}
