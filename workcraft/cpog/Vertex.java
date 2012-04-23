package workcraft.cpog;

import java.awt.event.KeyEvent;
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
import workcraft.common.LabelledConnection;
import workcraft.editor.BasicEditable;
import workcraft.editor.EditableConnection;
import workcraft.logic.DNF;
import workcraft.logic.InvalidExpressionException;
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

public class Vertex extends BasicEditable {
	public static final UUID _modeluuid = UUID.fromString("25787b48-9c3d-11dc-8314-0800200c9a66");
	public static final String _displayname = "Vertex";
	public static final String _hotkey = " v ";
	public static final int _hotkeyvk = KeyEvent.VK_V;		

	private static Colorf vertexColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);
	private static Colorf selectedVertexColor = new Colorf(1.0f, 0.9f, 0.9f, 1.0f);
	private static Colorf enabledVertexColor = new Colorf(0.8f, 0.8f, 1.0f, 1.0f);
	private static Colorf vertexOutlineColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf passiveVertexOutlineColor = new Colorf(0.6f, 0.6f, 0.6f, 1.0f);
	private static Colorf selectedVertexOutlineColor = new Colorf(0.5f, 0.0f, 0.0f, 1.0f);
	private static Colorf userVertexOutlineColor = new Colorf(0.0f, 0.6f, 0.0f, 1.0f);
	private static Colorf vertexFiredColor = new Colorf(0.3f, 0.3f, 0.5f, 1.0f);

	private Condition condition;
	
	boolean fired = false;
	
	public boolean canFire = false;
	public boolean canWork = false;

	private LinkedList<Vertex> out;
	private LinkedList<Vertex> in;
	private LinkedList<ControlVariable> vars;
	
	private String rho = null;
	
	public String getRho() {
		return rho;
	}

	public void setRho(String rho) {
		this.rho = rho;
	}

	public LinkedList<Vertex> getOut()
	{
		return (LinkedList<Vertex>)out.clone();
	}

	public LinkedList<Vertex> getIn()
	{
		return (LinkedList<Vertex>)in.clone();
	}

	public LinkedList<ControlVariable> getVars()
	{
		return (LinkedList<ControlVariable>)vars.clone();
	}

	public void removeIn(Vertex t)
	{
		in.remove(t);
	}

	public void removeOut(Vertex t)
	{
		out.remove(t);
	}
	
	public void removeVar(ControlVariable t)
	{
		vars.remove(t);
	}
	

	public Boolean isActive()
	{
		return condition.evaluate();
	}

	public boolean addIn(DefaultConnection con) {
		Vertex t = (Vertex)con.getFirst();
		if (in.contains(t))
			return false;
		in.add(t);
		connections.add(con);
		return true;
	}

	public boolean addOut(DefaultConnection con) {
		Vertex t = (Vertex)con.getSecond();
		if (out.contains(t))
			return false;
		out.add(t);
		connections.add(con);
		return true;
	}
	
	public boolean addVar(DefaultConnection con)
	{
		ControlVariable t = (ControlVariable)con.getSecond();
		if (vars.contains(t))
			return false;
		vars.add(t);
		connections.add(con);
		return true;
	}
	
	@Override
	public boolean hits(Vec2 pointInViewSpace)
	{
		Vec2 v = new Vec2(pointInViewSpace);
		transform.getViewToLocalMatrix().transform(v);
		return v.length() < 0.05f;
	}

	public String getCondition()
	{
		return condition.string;
	}	

	public void setCondition(String condition)
	{
		if (!this.condition.setCondition(condition)) return;
		
		String label = getLabel();
		if (label.lastIndexOf(": ") != -1)
		{
			label = label.substring(0, label.lastIndexOf(": "));
		}
		if (!condition.equals("1")) label = label + ": " + condition;
		setLabel(label);
	}

	public Vertex(BasicEditable parent) throws UnsupportedComponentException
	{
		super(parent);
		boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f));
		condition = new Condition("1", (CPOGModel) ownerDocument);
		setRho("1");
		
		out = new LinkedList<Vertex>();
		in = new LinkedList<Vertex>();
		vars = new LinkedList<ControlVariable>();
	}

	public void refresh()
	{
		if (condition != null) condition.refresh();
	}
	
	public void draw(Painter p)
	{		
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL);

		if (selected)
			p.setFillColor(selectedVertexOutlineColor);
		else
			if (canWork)
				p.setFillColor(userVertexOutlineColor);
			else
				if (isActive())
					p.setFillColor(vertexOutlineColor);
				else
					p.setFillColor(passiveVertexOutlineColor);

		p.drawCircle(0.05f, null);

		if (selected)
			p.setFillColor(selectedVertexColor);
		else
			if (canFire)
				p.setFillColor(enabledVertexColor);
			else
				if (fired)
					p.setFillColor(vertexFiredColor);
				else
				p.setFillColor(vertexColor);
		
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

	public String getControlledSet()
	{
		String res = "";
		for(ControlVariable v : vars)
		if (res.length() > 0) res = res + ", " + v.getId(); else res = v.getId();
		return res;
	}
	
	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		list.add("str,Condition,getCondition,setCondition");
		list.add("bool,Active,isActive,-");
		list.add("str,Restriction function,getRho,setRho");
		list.add("str,Controlled set,getControlledSet,-");
		return list;
	}


	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("vertex");
		Element ne = (Element) nl.item(0);
		setCondition(ne.getAttribute("condition"));
		setRho(ne.getAttribute("rho"));
		super.fromXmlDom(element);
	}
	
	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("vertex");
		ppe.setAttribute("condition", getCondition());
		ppe.setAttribute("rho", getRho());
		ee.appendChild(ppe);
		return ee;
	}
			
	public void simAction(int flag)
	{
		if (flag == MouseEvent.BUTTON1 && !fired && isActive())
		{
			canWork = !canWork;
		}
	}

	public void fire()
	{
		fired = true;
		for(ControlVariable v : vars) v.setCurrentValue(v.getFinalValue());
	}

}