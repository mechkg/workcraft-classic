package workcraft.editor;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import workcraft.propertyeditor.EnumWrapper;
import workcraft.util.Colorf;

public class PropertyEditorTableModel extends AbstractTableModel {
	String [] columnNames = { "property", "value" };
	PropertyEditable object = null;

	String [] propertyNames;
	String [] propertyGetters;
	String [] propertySetters;
	Class [] propertyClasses;
	Object[] propertyData;

	int property_count = 0;

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public void setObject(PropertyEditable object, String[] propertyNames,
			String[] propertyGetters, String[] propertySetters, Class[] propertyClasses, Object[] propertyData) {
		this.object = object;

		this.propertyNames = propertyNames;
		this.propertyGetters = propertyGetters;
		this.propertySetters = propertySetters;
		this.propertyClasses = propertyClasses;
		this.propertyData = propertyData;

		property_count = propertyNames.length;

		fireTableDataChanged();
	}

	public void clearObject() {
		object = null;
		property_count = 0;
		fireTableDataChanged();
	}

	public int getColumnCount() {
		return 2;
	}

	public int getRowCount() {
		return property_count;
	}

	public boolean isCellEditable(int row, int col) {
		if (col < 1) {
			return false;
		} else if (propertySetters[row] != null) {
			return true;
		} else
			return false;
	}

	public Object getValueAt(int row, int col) {
		if (col==0)
		{
			return propertyNames[row];		
		}
		else {
			try {
				Method m = object.getClass().getMethod(propertyGetters[row],  (Class[])null );
				Object o = m.invoke(object, (Object[])null);
				if (propertyClasses[row] == EnumWrapper.class)
				{
					int i = (Integer)o;
					String names[] = (String[]) propertyData[row];
					return new EnumWrapper(i, names[i]);
				}
				else
					return o; 
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				return "ERROR!";
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				return "ERROR!";
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				return "ERROR!";
			}
		}
	}

	public void setValueAt(Object value, int row, int col) {
		if (col==1) {
			Object brainyobject = null;
			if (propertyClasses[row] == String.class) {
				brainyobject = (String)value;
				fireTableDataChanged();
			} else if (propertyClasses[row] == Integer.class) {
				brainyobject = Integer.parseInt((String)value);
				fireTableDataChanged();
			} else if (propertyClasses[row] == Double.class) {
				brainyobject = Double.parseDouble((String)value);
				fireTableDataChanged();
			} else if (propertyClasses[row] == Boolean.class) {
				brainyobject = Boolean.parseBoolean((String)value);
				fireTableDataChanged();
			} else if (propertyClasses[row] == Colorf.class) {
				brainyobject = value;
				fireTableDataChanged();
			} else if (propertyClasses[row] == EnumWrapper.class) {
				brainyobject = new Integer(((EnumWrapper)value).index());
				fireTableDataChanged();
			} else if (propertyClasses[row] == File.class) {
				brainyobject = (String)value;
				fireTableDataChanged();
			}
			else {
				System.err.println ("PropertyEditorTableModel doesn't know how to use class " + propertyClasses[row].getName());
			}
			if (propertySetters[row]!=null) {
				try {
					Method m;
					if (propertyClasses[row] == EnumWrapper.class)
						m = object.getClass().getMethod(propertySetters[row],  Integer.class );
					else if (propertyClasses[row] == File.class)
						m = object.getClass().getMethod(propertySetters[row],  String.class );
						else
						m = object.getClass().getMethod(propertySetters[row],  propertyClasses[row] );
					m.invoke(object, brainyobject);
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
