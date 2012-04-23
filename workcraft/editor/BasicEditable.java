package workcraft.editor;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.python.core.PyObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.Model;
import workcraft.ModelBase;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.XmlSerializable;
import workcraft.petri.PetriModel;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.Painter;
import workcraft.visual.TextAlign;

public abstract class BasicEditable extends EditableNode implements XmlSerializable, Comparable<BasicEditable>  {
	protected int zOrder; 
	protected BasicEditable parent;
	protected TreeSet<BasicEditable> children;
	protected BoundingBox boundingBox;
	public TransformNode transform;
	protected String id = "unnamed";
	private String label = "";
	public boolean selected = false;
	protected Model ownerDocument = null;
	protected Colorf labelColor = new Colorf (0.0f, 0.0f, 0.0f, 1.0f);
	public boolean highlight = false;
	
	Hashtable<String, String> customProperties = new Hashtable<String, String>();
	
	private static int labelOrder = 0;

	public LinkedList<EditableConnection> connections = new LinkedList<EditableConnection>();
	protected int rotate = 0;

	protected float getLabelYOffset() {
		return (labelOrder==0)?-0.05f:0.025f;
	}

	// wrapper for automated colour selection at child classes
	protected Colorf getLabelColor() {
		return labelColor;
	}
	
	protected Boolean getIsShorthandNotation() {
		PetriModel pm = (PetriModel) this.getOwnerDocument();
		Boolean isShorthandNotation = false;
		
		if (pm!=null) isShorthandNotation = pm.getShorthandNotation();
		return isShorthandNotation;
	}
	
	public void setId(String id) throws DuplicateIdException {
		if (this.id.equals(id))
			return;
	
		if (ownerDocument == null) {
			this.id = id;
			return;
		}

		if (ownerDocument.getComponentById(id) != null)
			throw new DuplicateIdException(id);
		((ModelBase)ownerDocument).renameComponent(this, id);
		
		WorkCraftServer server = ownerDocument.getServer();
		if (server != null)
		{
			if (server.python.get(id)!=null)
				throw new DuplicateIdException(id);
			if(server.getObjectById(this.id)==this)
				server.unregisterObject(this.id);
			this.id = id;
			server.registerObject(this, this.id);
		} else
			this.id = id;
		

	}

	public String getId() {
		return id;
	}

	public List<EditableConnection> getAllConnections() {
		return connections;
	}

	public void removeFromConnections(EditableConnection con) {
		connections.remove(con);
	}
	
	protected BasicEditable() {
		this.zOrder = 0;
		this.parent = null;
		this.children = new TreeSet<BasicEditable>();
		transform = new TransformNode(this);
		boundingBox = new BoundingBox();
	}

	public BasicEditable(BasicEditable parent) throws UnsupportedComponentException {
		this.zOrder = 0;
		this.children = new TreeSet<BasicEditable>();
		transform = new TransformNode(this);
		boundingBox = new BoundingBox();
		parent.addChild(this);
		parent.getOwnerDocument().addComponent(this, true);
	}

	public int compareTo (BasicEditable other) {
		if (zOrder == other.getZOrder()) {
			return this.hashCode() - other.hashCode();
		}
		return zOrder - other.getZOrder();
	}

	public BasicEditable getParent() {
		return parent;
	}
	
	public BasicEditable getTopParent(GroupNode root) {
		return (parent==root)?this:parent.getTopParent(root);
	}

	public void setParent(BasicEditable parent) {
		this.parent = parent;
		this.ownerDocument = parent.getOwnerDocument();
	}

	public void setZOrder(int z) {
		zOrder = z;
	}

	public int getZOrder() {
		return zOrder;		
	}

	public TreeSet<BasicEditable> getChildren() {
		return (TreeSet<BasicEditable>) children.clone();
	}

	public void addChild (BasicEditable child) {
		children.add(child);
		child.setParent(this);
	}

	public boolean removeChild(BasicEditable child) {
		return children.remove(child);		
	}

	public BoundingBox getBoundingBox() {
		return new BoundingBox(boundingBox);
	}
	
	public BoundingBox getBoundingBoxInViewSpace() {
		Vec2 ll = boundingBox.getLowerLeft();
		Vec2 ur = boundingBox.getUpperRight();
		transform.getLocalToViewMatrix().transform(ll);
		transform.getLocalToViewMatrix().transform(ur);
		return new BoundingBox(ll, ur);
	}

	public boolean intersectsBB (BoundingBox which) {
		Vec2 ll = which.getLowerLeft();
		Vec2 ur = which.getUpperRight();
		transform.getViewToLocalMatrix().transform(ll);
		transform.getViewToLocalMatrix().transform(ur);
		return boundingBox.intersects(new BoundingBox(ll, ur));				
	}

	public boolean hitsBB(Vec2 pointInViewSpace) {
		Vec2 v = new Vec2(pointInViewSpace);
		transform.getViewToLocalMatrix().transform(v);
		return boundingBox.isInside(v);
	}

	public void update (Mat4x4 matView) {
		transform.updateTransformCache(matView);
		for (BasicEditable n : children) {
			n.update(transform.getViewToLocalMatrix());
		}
	}

	public void removeAllChildren() {
		children.clear();
	}

	public Element toXmlDom (Element parent_element) {
		Document d = parent_element.getOwnerDocument();
		Element ee = d.createElement("editable");
		ee.setAttribute("id", getId());
		ee.setAttribute("label", label);
		ee.setAttribute("selected", Boolean.toString(selected));
		ee.setAttribute("rotation", Integer.toString(getRotate()));
		ee.setAttribute("class", this.getClass().getName());
		
		Element cpe = d.createElement("custom-properties");
		
		for (String key : customProperties.keySet()) {
			Element p = d.createElement("property");
			p.setAttribute("key", key);
			p.setAttribute("value", customProperties.get(key));
			
			cpe.appendChild(p);
		}
		
		transform.toXmlDom(ee);
		ee.appendChild(cpe);
		parent_element.appendChild(ee);
		return ee;
	}

	public void fromXmlDom(Element e) throws DuplicateIdException {
		String id = e.getAttribute("id");
		setLabel(e.getAttribute("label"));
		
		NodeList nl = e.getElementsByTagName("transform");
		transform.fromXmlDom((Element)nl.item(0));
		
		String r = e.getAttribute("rotation");
		if (r.length()>0)
			setRotate(Integer.parseInt(r));
		
		nl = e.getElementsByTagName("custom-properties");
		if (nl.getLength() > 0) {
			nl = ((Element)nl.item(0)).getElementsByTagName("property");
			
			for (int i=0; i<nl.getLength(); i++) {
				String k,v;
				k = ((Element)nl.item(i)).getAttribute("key");
				v = ((Element)nl.item(i)).getAttribute("value");
				if (k.length()>0)
					customProperties.put(k, v);
			}
		}
		
		setId (id);
		// selected = Boolean.parseBoolean(e.getAttribute("selected"));
	}

	/*public void fromXmlDom(Element e, HashMap<String, String> renamed) throws DuplicateIdException {
		String id = e.getAttribute("id");
		label = e.getAttribute("label");
		NodeList nl = e.getElementsByTagName("transform");
		transform.fromXmlDom((Element)nl.item(0));
		if(renamed==null)
			setId(id);
		else if(!id.equals(this.id))
			renamed.put(id, this.id);
	}*/

	public List<String> getEditableProperties() {
		LinkedList<String> list = new LinkedList<String>();

		list.add("str,ID,getId,setId");
		list.add("str,Label,getLabel,setLabel");
		list.add("enum,^ Label order,getLabelOrder,setLabelOrder,ID on top,Label on top");
		list.add("enum,Rotation,getRotate,setRotate,0,90,180,270");
		
		return list;
	}

	public boolean hits(Vec2 pointInViewSpace) {
		return hitsBB(pointInViewSpace);		
	}

	public abstract BasicEditable getChildAt(Vec2 point);
	
	public boolean cull(Painter p) {
		return p.cull(getBoundingBoxInViewSpace());
	}
	
	public void draw (Painter p) {
		if (!cull(p))
			doDraw(p);
	}
	
	
	private void recTraceChildren(BasicEditable n, int level) {
		for (int i=0; i<level; i++)
			System.out.print(' ');
		System.out.println(n.getId());
		for (BasicEditable cn : n.getChildren())
			recTraceChildren(cn, level+1);
	}
	
	public void dbgChildren() {
		recTraceChildren(this, 0);
	}
	
	public String getCustomProperty(String key) {
		return customProperties.get(key);
	}
	
	public void setCustomProperty(String key, String value) {
		customProperties.put(key, value);
	}
	
	public void copyCustomProperties(BasicEditable other) {
		customProperties = (Hashtable<String, String>)other.customProperties.clone();
	}

	public void doDraw (Painter p) {
		WorkCraftServer server = ownerDocument.getServer();
		
		Vec2 ll = boundingBox.getLowerLeft();
		Vec2 ur = boundingBox.getUpperRight();
		Vec2 ul = new Vec2(ll.getX(), ur.getY());
		Vec2 lr = new Vec2(ur.getX(), ll.getY());
		
		transform.getLocalToViewMatrix().transform(ll);
		transform.getLocalToViewMatrix().transform(ur);
		transform.getLocalToViewMatrix().transform(ul);
		transform.getLocalToViewMatrix().transform(lr);
		
		BoundingBox superbb = new BoundingBox();
		
		superbb.addPoint(ll);
		superbb.addPoint(ul);
		superbb.addPoint(ur);
		superbb.addPoint(lr);
		
		
		Vec2 v1 = superbb.getLowerLeft();
		Vec2 v2 = superbb.getUpperRight();
		Vec2 center;

		p.setTextColor(getLabelColor());
		
		PyObject po;
		if (server != null) 
			po = server.python.get("_draw_labels");
		else
			po = null;
		
		if ((server == null) || (po != null && po.__nonzero__()))
			if (!label.equals("")) {
				if (labelOrder == 0)
					center = new Vec2(0.5f*(v1.getX()+v2.getX()), v1.getY() + getLabelYOffset() );
				else
					center = new Vec2( (v1.getX()+v2.getX())*0.5f , v2.getY() + getLabelYOffset() );

		//		transform.getLocalToViewMatrix().transform(center);
				p.drawString(label, center, 0.05f, TextAlign.CENTER);
			}

		if (server != null) 
			po = server.python.get("_draw_ids");
		else
			po = null;

		if ((server == null) || (po != null && po.__nonzero__()))
			if (id != null && !id.equals("")) {
				if (labelOrder == 0)
					center = new Vec2( (v1.getX()+v2.getX())*0.5f , v2.getY()+0.025f );
				else
					center = new Vec2(0.5f*(v1.getX()+v2.getX()), v1.getY()-0.05f );
		//		transform.getLocalToViewMatrix().transform(center);
				p.drawString(id, center, 0.05f, TextAlign.CENTER);
			}
	}

	public void dblClick() {
		// qq
	}

	public void setOwnerDocument(Model ownerDocument) {
		this.ownerDocument = ownerDocument;
	}

	public void simAction(int flag) {

	}

	public Model getOwnerDocument() {
		return ownerDocument;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public static Integer getLabelOrder() {
		return labelOrder;
	}

	public static void setLabelOrder(Integer labelOrder) {
		BasicEditable.labelOrder = labelOrder;
	}
	
	public void mouseDown() {
		System.out.println(id+": qwe");
	}
	
	public void mouseUp() {
		System.out.println(id+": xru");
	}
	
	public void register(WorkCraftServer server) throws DuplicateIdException {
		server.registerObject(this, id);
		for (BasicEditable n: children)
			n.register(server);
	}
	
	public void acceptTransform() {
		// do nothing: by default all transformations are valid 
	}

	public Integer getRotate() {
		return rotate;
	}

	public void setRotate(Integer rotate) {
		this.rotate = rotate;
		this.transform.rotateZ(rotate*90);
	}
}