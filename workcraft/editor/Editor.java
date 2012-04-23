package workcraft.editor;
import java.awt.Component;
import java.util.List;

import javax.swing.JPanel;

import workcraft.DocumentOpenException;
import workcraft.Model;

public interface Editor {
	public void save();
	public void saveAs();
	public void open();
	public Model load(String path) throws DocumentOpenException;
	public void setDocument(Model document);
	public List<BasicEditable> getSelection();
	public Model getDocument();
	public String getFileName();
	public String getLastDirectory();
	public Component getSimControls();
	public void refresh();
}