package workcraft.chetri;

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

public class EditableChetriTransition extends BasicEditable {
	public static final UUID _modeluuid = UUID.fromString("e9137910-8535-11db-b606-0800200c9a66");
	public static final String _displayname = "Transition";
	private static VertexBuffer geometry = null;

	private LinkedList<EditableChetriPlace> out;
	private LinkedList<EditableChetriPlace> in;

	public boolean canFire = false;
	public boolean canWork = false;
	
	public int token = 0;
	
	public String script;
	
	private static Colorf transitionColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);
	private static Colorf selectedTransitionColor = new Colorf(1.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf enabledTransitionColor = new Colorf(0.6f, 0.6f, 1.0f, 1.0f);
	private static Colorf transitionOutlineColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf selectedTransitionOutlineColor = new Colorf(1.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf userTransitionOutlineColor = new Colorf(0.0f, 0.6f, 0.0f, 1.0f);
	

	public LinkedList<EditableChetriPlace> getOut() {
		return (LinkedList<EditableChetriPlace>)out.clone();
	}

	public LinkedList<EditableChetriPlace> getIn() {
		return (LinkedList<EditableChetriPlace>)in.clone();
	}
	public void removeIn(EditableChetriPlace t) {
		in.remove(t);
	}

	public void removeOut(EditableChetriPlace t) {
		out.remove(t);
	}

	public boolean addIn(DefaultConnection con) {
		EditableChetriPlace t = (EditableChetriPlace)con.getFirst();
		if (in.contains(t))
			return false;
		in.add(t);
		connections.add(con);
		return true;
	}

	public boolean addOut(DefaultConnection con) {
		EditableChetriPlace t = (EditableChetriPlace)con.getSecond();
		if (out.contains(t))
			return false;
		out.add(t);
		connections.add(con);
		return true;
	}

	public EditableChetriTransition() {
		boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f));
		in = new LinkedList<EditableChetriPlace>();
		out = new LinkedList<EditableChetriPlace>();
	}

	public void draw(Painter p) {
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL);

		if (selected)
			p.setFillColor(selectedTransitionOutlineColor);
		else
			if (canWork)
				p.setFillColor(userTransitionOutlineColor);
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
		
		if (token > 0) 
		{
			p.setFillColor(transitionOutlineColor);
			p.drawRect(-0.025f, 0.025f, 0.025f, -0.025f);
		}		
		
		super.draw(p);
	}

	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		list.add("int,Tokens,getTokens,setTokens");
		list.add("str,Script,getScript,setScript");
		return list;
	}
	
	public Integer getTokens()
	{
		return token;
	}
	
	public void setTokens(Integer t)
	{
		token = t;
	}
	
	public String getScript()
	{
		return script;
	}
	
	public void setScript(String t)
	{
		script = t;
	}
	
	public void fromXmlDom(Element element) {
		try {
			super.fromXmlDom(element);
		} catch (DuplicateIdException e) {
			e.printStackTrace();
		}
		NodeList nl = element.getElementsByTagName("transition");
		Element te = (Element) nl.item(0);
		token = Integer.parseInt(te.getAttribute("token"));
		script = te.getAttribute("script");
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element); 
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("transition");
		ppe.setAttribute("token", Integer.toString(token));
		ppe.setAttribute("script", script);
		ee.appendChild(ppe);
		return ee;
	}
	
	public void simAction(int flag) {
		if (flag == MouseEvent.BUTTON1) {
			canWork = !canWork;
		}
	}

	@Override
	public BasicEditable getChildAt(Vec2 point) {
		// TODO Auto-generated method stub
		return null;
	}


}