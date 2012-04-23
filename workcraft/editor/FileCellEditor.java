package workcraft.editor;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import workcraft.XwdFileFilter;
import workcraft.util.Colorf;

public class FileCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
	String currentFile = ""; 
	JButton button ;
	static JFileChooser dialog = null;
		
	protected static final String EDIT = "edit";

	public  FileCellEditor() {
		button = new JButton();
		button.setActionCommand(EDIT);
		button.addActionListener(this);
		button.setBorderPainted(false);

//		Set up the dialog that the button brings up.
		if (dialog == null)
			dialog = new JFileChooser();
	}

	public void actionPerformed(ActionEvent e) {
		if (EDIT.equals(e.getActionCommand())) {
//			The user has clicked the cell, so
//			bring up the dialog.
			
			button.setText(currentFile);

			dialog.setFileFilter(new XwdFileFilter());
			dialog.setCurrentDirectory(new File(currentFile));
			if (dialog.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				currentFile = dialog.getSelectedFile().getAbsolutePath();
				button.setText(currentFile);
			}
			
			fireEditingStopped(); //Make the renderer reappear.

		} else { //User pressed dialog's "OK" button.
			// currentColor = new Colorf (colorChooser.getColor());
		}
	}

//	Implement the one CellEditor method that AbstractCellEditor doesn't.
	public Object getCellEditorValue() {
		return currentFile;
	}

//	Implement the one method defined by TableCellEditor.
	public Component getTableCellEditorComponent(JTable table,
			Object value,
			boolean isSelected,
			int row,
			int column) {
		currentFile = (String)value;
		return button;
	}
}

