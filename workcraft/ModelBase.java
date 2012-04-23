package workcraft;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.editor.BasicEditable;
import workcraft.editor.EditorPane;
import workcraft.editor.GroupNode;
import workcraft.util.Colorf;

public abstract class ModelBase implements Model {

	public static UUID _modeluuid;
	public static String _displayname;
	
	Hashtable<String, BasicEditable> idMap = new Hashtable<String, BasicEditable>();
	
	protected GroupNode root = new GroupNode(this, "_root");
	protected WorkCraftServer server = null;
	protected EditorPane editor = null;
	
	protected Boolean loading = false;
	protected Boolean showGrid = true;
	protected Boolean showLabels = true;
	protected Boolean showIDs = true;
	protected Colorf backgroundColor = new Colorf (1.0f, 1.0f, 1.0f, 1.0f);
	protected Colorf majorGridColor = new Colorf(0.5f, 0.5f, 0.5f, 1.0f);
	protected Colorf minorGridColor = new Colorf(0.925f, 0.925f, 0.925f, 1.0f);
	protected Double 	majorGridInterval = 1.0;
	protected Double minorGridInterval = 0.1;
	protected boolean showHighlight = false;
	protected Colorf highlightColor = new Colorf(1.0f, 0.0f, 0.0f, 1.0f);
	
	public void recBind(BasicEditable n, WorkCraftServer server) {
		try {
			server.registerObject(n, n.getId());
		} catch (DuplicateIdException e) {
			e.printStackTrace();
		}
		for (BasicEditable nn : n.getChildren()) {
			recBind(nn, server);
		}
	}
	
	public void loadEnd() {
		loading = false;
		
	}

	public void loadStart() {
		loading = true;
	}
	
	public boolean isLoading() {
		return loading;
	}
	
	public void bind(WorkCraftServer server) {
		this.server = server;
		server.python.set("_document", this);
		if (root!=null)
			recBind(root, server);
	}
	
	public void removeComponent(BasicEditable c) throws UnsupportedComponentException {
		if (server!=null)
			server.unregisterObject(c.getId());
		idMap.remove(c.getId());
		root.removeChild(c);
	}
	
	public void addComponent(BasicEditable c, boolean auto_name) throws UnsupportedComponentException {
		//System.err.println ("Adding component " + c.getId());
		if (auto_name && loading)
			try {
				c.setId(c.getId()+"_ld");
			} catch (DuplicateIdException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		idMap.put(c.getId(), c);
		// root.addChild(c);
		c.setOwnerDocument(this);
		
	}
	
	public void renameComponent (BasicEditable e, String newId) {
		//System.err.println ("Renaming "+e.getId()+" to "+newId);
		idMap.remove(e.getId());
		idMap.put(newId, e);
	}
	
	public void restoreState(Object state) {
		
	}
	
	public Object saveState() {
		return null;
	}
	
	public List<String>getEditableProperties()  {
		List<String> list = new LinkedList<String>(); 
		 
		list.add("color,Background color,getBackgroundColor,setBackgroundColor");
		list.add("bool,Show grid,getShowGrid,setShowGrid");
		list.add("double,Major grid interval,getMajorGridInterval,setMajorGridInterval");
		list.add("color,Major grid color,getMajorGridColor,setMajorGridColor");
		list.add("double,Minor grid interval,getMinorGridInterval,setMinorGridInterval");
		list.add("color,Minor grid color,getMinorGridColor,setMinorGridColor");
		list.add("bool,Show labels,getShowLabels,setShowLabels");
		list.add("bool,Show IDs,getShowIDs,setShowIDs");
		list.add("bool,Highlight,isShowHighlight,setShowHighlight");
		list.add("color,Highlight color,getHighlightColor,setHighlightColor");
		
		return list;
	}

	public void fromXmlDom(Element element)  {
	/*	String id = e.getAttribute("id");
		label = e.getAttribute("label");
		NodeList nl = e.getElementsByTagName("transform");
		
		String r = e.getAttribute("rotation");
		if (r.length()>0)
			setRotate(Integer.parseInt(r));
		
		transform.fromXmlDom((Element)nl.item(0));
		setId (id);*/
		
	}

	public Element toXmlDom(Element parent_element) {
		// TODO Auto-generated method stub
		return null;
	}

	public Colorf getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(Colorf backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public Colorf getMajorGridColor() {
		return majorGridColor;
	}

	public void setMajorGridColor(Colorf majorGridColor) {
		this.majorGridColor = majorGridColor;
		if (editor != null)
			editor.grid.setMajorColor(majorGridColor);
	}

	public Double getMajorGridInterval() {
		return majorGridInterval;
	}

	public void setMajorGridInterval(Double majorGridInterval) {
		
		if (majorGridInterval < 0.01)
			majorGridInterval = 0.01;
		if (majorGridInterval > 10.0)
			majorGridInterval = 10.0;
		
		this.majorGridInterval = majorGridInterval;
		
		if (editor != null)
			editor.grid.setMajorInterval(majorGridInterval.floatValue());
		
		this.majorGridInterval = majorGridInterval;
	}

	public Colorf getMinorGridColor() {
		return minorGridColor;
	}

	public void setMinorGridColor(Colorf minorGridColor) {
		this.minorGridColor = minorGridColor;

		if (editor != null)
			editor.grid.setMinorColor(minorGridColor);
	}

	public Double getMinorGridInterval() {
		return minorGridInterval;
	}

	public void setMinorGridInterval(Double minorGridInterval) {
		if (minorGridInterval < 0.01)
			minorGridInterval = 0.01;
		if (minorGridInterval > majorGridInterval)
			minorGridInterval = majorGridInterval;
		
		this.minorGridInterval = minorGridInterval;
		
		if (editor != null)
			editor.grid.setMinorInterval(minorGridInterval.floatValue());
	}

	public Boolean getShowGrid() {
		return showGrid;
	}

	public void setShowGrid(Boolean showGrid) {
		this.showGrid = showGrid;
	}

	public Boolean getShowIDs() {
		return showIDs;
	}

	public void setShowIDs(Boolean showIDs) {
		this.showIDs = showIDs;
	}

	public Boolean getShowLabels() {
		return showLabels;
	}

	public void setShowLabels(Boolean showLabels) {
		this.showLabels = showLabels;
	}

	public EditorPane getEditor() {
		return editor;
	}

	public void setEditor(EditorPane editor) {
		this.editor = editor;
	}

	public Boolean isShowHighlight() {
		return showHighlight;
	}

	public void setShowHighlight(Boolean showHighlight) {
		this.showHighlight = showHighlight;
	}

	public Colorf getHighlightColor() {
		return highlightColor;
	}

	public void setHighlightColor(Colorf highlightColor) {
		this.highlightColor = highlightColor;
	}	
	
	public void clearHighlights() {
		LinkedList<BasicEditable> c = new LinkedList<BasicEditable>();
		getComponents(c);

		for (BasicEditable e : c)
			e.highlight = false;		
	}

	public void getGuideNodes(List<BasicEditable> out) {
		
	}
	
	public GroupNode getRoot() {
		return root;
	}

	public void setRoot(GroupNode root) {
		this.root = root;
	}
	
	public BasicEditable getComponentById(String id) {
		return idMap.get(id);
	}

	
	// public void saveState()
}