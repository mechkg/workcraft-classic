package workcraft.editor;
import java.io.File;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.*;

import workcraft.editor.colorcell.ColorCellEditor;
import workcraft.editor.colorcell.ColorCellRenderer;
import workcraft.propertyeditor.EnumWrapper;
import workcraft.util.Colorf;

public class PropertyEditorTable extends JTable {
	HashMap<Integer, TableCellEditor>	rowEditors = new HashMap<Integer, TableCellEditor>();
	HashMap<Integer, TableCellRenderer> rowRenderers = new HashMap<Integer, TableCellRenderer>();
	static String boolValues[] = new String[] { "true", "false" };

	public PropertyEditorTable(PropertyEditorTableModel petm) {
		super(petm);
	}
	public void clearRowEditors() {
		rowEditors.clear();
	}
	public void setRowClass (int row, Class cls, String[] data) {
		if (cls == String.class || cls == Integer.class || cls == Double.class) {
			JTextField editor = new JTextField();
			rowEditors.put(new Integer(row), new DefaultCellEditor(editor));
			rowRenderers.put(new Integer(row), new DefaultTableCellRenderer());
		} else if (cls == Boolean.class)
		{
			JComboBox editor = new JComboBox(boolValues);
			//JCheckBox editor = new JCheckBox();
			rowEditors.put(new Integer(row), new DefaultCellEditor(editor));
			rowRenderers.put(new Integer(row), new DefaultTableCellRenderer());
		} else if (cls == Colorf.class) {
			rowEditors.put(new Integer(row), new ColorCellEditor());
			rowRenderers.put(new Integer(row), new ColorCellRenderer(true));
		}	else if (cls == EnumWrapper.class) {
			
			EnumWrapper w[] = new EnumWrapper[data.length];
			for (int i=0; i<data.length; i++)
				w[i] = new EnumWrapper (i, data[i]);
			JComboBox editor = new JComboBox(w);
			rowEditors.put(new Integer(row), new DefaultCellEditor(editor));
			rowRenderers.put(new Integer(row), new DefaultTableCellRenderer());
		}	else if (cls == File.class) {
			rowEditors.put(new Integer(row), new FileCellEditor());
			rowRenderers.put(new Integer(row), new DefaultTableCellRenderer());
		}
			else if (cls == Object.class) {
			JComboBox editor = new JComboBox(data);
			rowEditors.put(new Integer(row), new DefaultCellEditor(editor));
			rowRenderers.put(new Integer(row), new DefaultTableCellRenderer());
		} else
			rowEditors.put(new Integer(row), null);
	}

	public TableCellEditor getCellEditor(int row, int col)
	{
		if (col==0)
			return super.getCellEditor(row, col);
		TableCellEditor ret = null;
		if (rowEditors!=null)
			ret = rowEditors.get(row);
		return (ret==null)?super.getCellEditor(row,col):ret;
	}
	public TableCellRenderer getCellRenderer(int row, int col)
	{
		if (col==0)
			return super.getCellRenderer(row, col);
		TableCellRenderer ret = null;
		if (rowRenderers!=null)
			ret = rowRenderers.get(row);
		return (ret==null)?super.getCellRenderer(row,col):ret;
	}

}