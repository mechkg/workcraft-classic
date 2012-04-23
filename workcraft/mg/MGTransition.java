package workcraft.mg;

import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.media.opengl.GL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.Model;
import workcraft.UnsupportedComponentException;
import workcraft.common.DefaultConnection;
import workcraft.editor.BasicEditable;
import workcraft.editor.EditableConnection;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.JOGLPainter;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.VertexBuffer;
import workcraft.visual.VertexFormat;
import workcraft.visual.PrimitiveType;
import workcraft.visual.VertexFormatException;

public class MGTransition extends BasicEditable {
	public static final UUID _modeluuid = UUID.fromString("4fbdb830-dba3-11db-8314-0800200c9a66");
	public static final String _displayname = "Transition";
	private static VertexBuffer geometry = null;

	private static Colorf transitionColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);
	private static Colorf selectedTransitionColor = new Colorf(1.0f, 0.9f, 0.9f, 1.0f);
	private static Colorf enabledTransitionColor = new Colorf(0.6f, 0.6f, 1.0f, 1.0f);
	private static Colorf transitionOutlineColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf selectedTransitionOutlineColor = new Colorf(0.5f, 0.0f, 0.0f, 1.0f);
	private static Colorf userTransitionOutlineColor = new Colorf(0.0f, 0.6f, 0.0f, 1.0f);

	private LinkedList<MGPlace> out;
	private LinkedList<MGPlace> in;

	public boolean canFire = false;
	public boolean canWork = false;
	
	protected int flag = 0;
	
	protected boolean critical = false;
	protected static boolean show_critical = true;
	
	public LinkedList<MGPlace> getOut() {
		return (LinkedList<MGPlace>)out.clone();
	}

	public LinkedList<MGPlace> getIn() {
		return (LinkedList<MGPlace>)in.clone();
	}
	public void removeIn(MGPlace t) {
		in.remove(t);
	}

	public void removeOut(MGPlace t) {
		out.remove(t);
	}

	public boolean addIn(DefaultConnection con) {
		MGPlace t = (MGPlace)con.getFirst();
		if (in.contains(t))
			return false;
		in.add(t);
		connections.add(con);
		return true;
	}

	public boolean addOut(DefaultConnection con) {
		MGPlace t = (MGPlace)con.getSecond();
		if (out.contains(t))
			return false;
		out.add(t);
		connections.add(con);
		return true;
	}

	
	public MGTransition(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f));
		in = new LinkedList<MGPlace>();
		out = new LinkedList<MGPlace>();
	}

	public void doDraw(Painter p) {
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL);

		if (selected)
			p.setFillColor(selectedTransitionOutlineColor);
		else
		if (canWork)
			p.setFillColor(userTransitionOutlineColor);
		else
		if (critical && show_critical)
			p.setFillColor(MGModel.criticalColor);
		else
			p.setFillColor(transitionOutlineColor);
	
		p.drawRect(-0.05f, 0.05f, 0.05f, -0.05f);		
		
		if (selected)
			p.setFillColor(selectedTransitionColor);
		else
		if (canFire)
			p.setFillColor(enabledTransitionColor);
		else
			p.setFillColor(transitionColor);
		
		p.drawRect(-0.04f, 0.04f, 0.04f, -0.04f);
		
		super.doDraw(p);
	}

	@Override
	public BasicEditable getChildAt(Vec2 point) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void update(Mat4x4 matView) {
		// TODO Auto-generated method stub

	}
	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("transition");
		Element te = (Element) nl.item(0);
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