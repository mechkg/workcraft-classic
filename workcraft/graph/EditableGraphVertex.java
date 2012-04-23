package workcraft.graph;

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
import workcraft.visual.TextAlign;
import workcraft.visual.VertexBuffer;
import workcraft.visual.GeometryUtil;
import workcraft.visual.VertexFormat;
import workcraft.visual.PrimitiveType;
import workcraft.visual.VertexFormatException;

public class EditableGraphVertex extends BasicEditable {
	public static final UUID _modeluuid = UUID.fromString("dc03e740-a705-11db-befa-0800200c9a66");
	public static final String _displayname = "Vertex";

	private static Colorf vertexColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);
	private static Colorf coloredVertexColor = new Colorf(0.3f, 0.3f, 1.0f, 1.0f);
	private static Colorf selectedVertexColor = new Colorf(1.0f, 0.9f, 0.9f, 1.0f);
	private static Colorf enabledVertexColor = new Colorf(0.8f, 0.8f, 1.0f, 1.0f);
	private static Colorf vertexOutlineColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf selectedVertexOutlineColor = new Colorf(0.5f, 0.0f, 0.0f, 1.0f);
	private static Colorf userVertexOutlineColor = new Colorf(0.0f, 0.6f, 0.0f, 1.0f);

	private int color = 0;
	
	public boolean canFire = false;
	public boolean canWork = false;

	private LinkedList<EditableGraphVertex> out;
	private LinkedList<EditableGraphVertex> in;

	public LinkedList<EditableGraphVertex> getOut() {
		return (LinkedList<EditableGraphVertex>)out.clone();
	}

	public LinkedList<EditableGraphVertex> getIn() {
		return (LinkedList<EditableGraphVertex>)in.clone();
	}

	public void removeIn(EditableGraphVertex t) {
		in.remove(t);
	}

	public void removeOut(EditableGraphVertex t) {
		out.remove(t);
	}

	public boolean addIn(DefaultConnection con) {
		EditableGraphVertex t = (EditableGraphVertex)con.getFirst();
		if (in.contains(t))
			return false;
		in.add(t);
		connections.add(con);
		return true;
	}

	public boolean addOut(DefaultConnection con) {
		EditableGraphVertex t = (EditableGraphVertex)con.getSecond();
		if (out.contains(t))
			return false;
		out.add(t);
		connections.add(con);
		return true;
	}
	
	@Override
	public boolean hits(Vec2 pointInViewSpace) {
		Vec2 v = new Vec2(pointInViewSpace);
		transform.getViewToLocalMatrix().transform(v);
		return v.length() < 0.05f;
	}

	public void setColor(Integer t) {
		color = t;
	}

	public int getColor() {
		return color;
	}

	public EditableGraphVertex(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f));
		color = 0;
		out = new LinkedList<EditableGraphVertex>();
		in = new LinkedList<EditableGraphVertex>();
	}

	public void draw(Painter p) {
		
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL);

		if (selected)
			p.setFillColor(selectedVertexOutlineColor);
		else
			if (canWork)
				p.setFillColor(userVertexOutlineColor);
			else
				p.setFillColor(vertexOutlineColor);

		p.drawCircle(0.05f, null);

		if (selected && color == 0)
			p.setFillColor(selectedVertexColor);
		else
		if (color == 0)
		{
			if (canFire)
				p.setFillColor(enabledVertexColor);
			else
				p.setFillColor(vertexColor);
		}
		else
			p.setFillColor(coloredVertexColor);

		p.drawCircle(0.04f, null);
		
		super.draw(p);
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

	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		list.add("int,Color,getColor,setColor");
		return list;
	}


	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("vertex");
		Element ne = (Element) nl.item(0);
		setColor(Integer.parseInt(ne.getAttribute("color")));
		super.fromXmlDom(element);
	}
	
	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("vertex");
		ppe.setAttribute("color", Integer.toString(getColor()));
		ee.appendChild(ppe);
		return ee;
	}
			
	public void simAction(int flag) {
		if (flag == MouseEvent.BUTTON1) {
			canWork = !canWork;
		}
	}

}