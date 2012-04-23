package workcraft;

import java.util.List;

import javax.swing.JPanel;

import workcraft.editor.BasicEditable;
import workcraft.editor.EditableConnection;
import workcraft.editor.EditorPane;
import workcraft.editor.GroupNode;
import workcraft.editor.PropertyEditable;

public interface Model extends PropertyEditable, XmlSerializable{
	
	public void addComponent(BasicEditable c, boolean auto_name) throws UnsupportedComponentException;
	public void removeComponent(BasicEditable c) throws UnsupportedComponentException;
		
	public EditableConnection createConnection(BasicEditable first, BasicEditable second) throws InvalidConnectionException;
	public List<EditableConnection> getConnections();
	
	public void getComponents(List<BasicEditable> out);
	public void getGuideNodes(List<BasicEditable> out);
	public void removeConnection (EditableConnection con) throws UnsupportedComponentException;

	public Object saveState();
	public void restoreState(Object state);
	
	public void simBegin();
	public void simStep();
	public void simFinish();
	public void simReset();
	public boolean simIsRunning();
	
	public void validate() throws ModelValidationException;
	public JPanel getSimulationControls();
	public void bind(WorkCraftServer server);
	public WorkCraftServer getServer();
	
	public void setRoot(GroupNode root);
	public GroupNode getRoot();
	
	public void loadStart();
	public void loadEnd();
	
	public BasicEditable getComponentById(String id);
	
	public EditorPane getEditor();
	public void setEditor(EditorPane editor);
	public void clearHighlights();
}