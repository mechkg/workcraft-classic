package workcraft.petri;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.UUID;

import org.python.core.PyObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import workcraft.DuplicateIdException;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.common.DefaultConnection;
import workcraft.editor.BasicEditable;
import workcraft.editor.BoundingBox;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;

public class EditablePetriTransition extends BasicEditable {
	public static final UUID _modeluuid = UUID.fromString("65f89260-641d-11db-bd13-0800200c9a66");
	public static final String _displayname = "Transition";
	public static final String _hotkey = "t";
	public static final int _hotkeyvk = KeyEvent.VK_T;
	//private static VertexBuffer geometry = null;

	private LinkedList<EditablePetriPlace> out;
	private LinkedList<EditablePetriPlace> in;

	public boolean canFire = false;
	public boolean canWork = false;
	
	private static Colorf transitionColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);
	private static Colorf selectedTransitionColor = new Colorf(1.0f, 0.9f, 0.9f, 1.0f);
	private static Colorf enabledTransitionColor = new Colorf(0.6f, 0.6f, 1.0f, 1.0f);
	private static Colorf transitionOutlineColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf selectedTransitionOutlineColor = new Colorf(0.5f, 0.0f, 0.0f, 1.0f);
	private static Colorf userTransitionOutlineColor = new Colorf(0.0f, 0.6f, 0.0f, 1.0f);
	
	public boolean hits(Vec2 pointInViewSpace) {
		
		Vec2 v = new Vec2(pointInViewSpace);
		transform.getViewToLocalMatrix().transform(v);
		
		if (!getIsDrawBorders()) {
			float x = Math.max(0.08f, getLabel().length()*0.026f);;
			
			return (new BoundingBox(new Vec2(-x/2f+0.01f, -0.04f), new Vec2(x/2f-0.01f, 0.04f))).isInside(v);
		}
		
		return boundingBox.isInside(v);
	}
	
	protected boolean getIsDrawBorders() {
		
		WorkCraftServer server = ownerDocument.getServer();
		PyObject po;
		if (server != null) 
			po = server.python.get("_draw_labels");
		else
			po = null;
		
		return canWork||!getIsShorthandNotation()||getLabel().equals("")||po==null||!po.__nonzero__();
		
	}
	
	protected float getLabelYOffset() {
		return (getLabelOrder()==0)?0.03f:-0.07f;
	}

	public LinkedList<EditablePetriPlace> getOut() {
		return (LinkedList<EditablePetriPlace>)out.clone();
	}

	public LinkedList<EditablePetriPlace> getIn() {
		return (LinkedList<EditablePetriPlace>)in.clone();
	}
	public void removeIn(EditablePetriPlace t) {
		in.remove(t);
	}

	public void removeOut(EditablePetriPlace t) {
		out.remove(t);
	}

	public boolean addIn(DefaultConnection con) {
		EditablePetriPlace t = (EditablePetriPlace)con.getFirst();
		if (in.contains(t))
			return false;
		in.add(t);
		connections.add(con);
		return true;
	}

	public boolean addOut(DefaultConnection con) {
		EditablePetriPlace t = (EditablePetriPlace)con.getSecond();
		if (out.contains(t))
			return false;
		out.add(t);
		connections.add(con);
		return true;
	}
	
	public void updateBoundingBox() {
		float x = Math.max(0.08f, getLabel().length()*0.026f);
		
		if (getLabel().equals("")||getLabelOrder()==0) x=0.08f;
		
/*		if (getIsDrawBorders()) {
			boundingBox.setExtents(new Vec2(-x/2-0.01f, -0.05f), new Vec2(x/2+0.01f, 0.05f));
		} else {*/
			boundingBox.setExtents(new Vec2(-x/2f, -0.05f), new Vec2(x/2f, 0.05f));
//		}
	}

	public void setLabel(String label) {
		super.setLabel(label);
		updateBoundingBox();
	}

	public EditablePetriTransition(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		
		updateBoundingBox();
		
		in = new LinkedList<EditablePetriPlace>();
		out = new LinkedList<EditablePetriPlace>();
	}

	public void doDraw(Painter p) {
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL);
		float x = Math.max(0.08f, getLabel().length()*0.026f);

		if (selected)
			p.setFillColor(selectedTransitionOutlineColor);
		else
			if (canWork)
				p.setFillColor(userTransitionOutlineColor);
			else
				p.setFillColor(transitionOutlineColor);
		
		if (getLabel().equals("")||getLabelOrder()==0) x=0.08f;
		
		if (getIsDrawBorders()) {
			// draw the rectangle only if there is no text in it
			p.drawRect(-x/2, 0.05f, x/2, -0.05f);
		}
		
		
		if (selected)
			p.setFillColor(selectedTransitionColor);
		else
			if (canFire)
				p.setFillColor(enabledTransitionColor);
			else
				p.setFillColor(transitionColor);
		
		p.drawRect(-x/2f+0.01f, 0.04f, +x/2f-0.01f, -0.04f);
		
		super.doDraw(p);
	}

	@Override
	public BasicEditable getChildAt(Vec2 point) {
		return null;
	}

	
	@Override
	public void update(Mat4x4 matView) {

	}
	public void fromXmlDom(Element element) throws DuplicateIdException {
//		TODO: do we need it?
//		NodeList nl = element.getElementsByTagName("transition");
//		Element te = (Element) nl.item(0);
		super.fromXmlDom(element);		
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element); 
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("transition");
		ee.appendChild(ppe);
		return ee;
	}
	
	public void simAction(int flag) {
		if (flag == MouseEvent.BUTTON1) {
			canWork = !canWork;
		}
	}	
}