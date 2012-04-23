package workcraft.chetri;

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
import workcraft.visual.GeometryUtil;
import workcraft.visual.VertexFormat;
import workcraft.visual.PrimitiveType;
import workcraft.visual.VertexFormatException;

public class EditableChetriPlace extends BasicEditable  {
	public static final UUID _modeluuid = UUID.fromString("e9137910-8535-11db-b606-0800200c9a66");
	public static final String _displayname = "Place";

	private static Colorf placeColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);
	private static Colorf startPlaceColor = new Colorf(0.9f, 1.0f, 0.9f, 1.0f);
	private static Colorf finishPlaceColor = new Colorf(0.9f, 0.9f, 1.0f, 1.0f);
	private static Colorf selectedPlaceColor = new Colorf(1.0f, 0.9f, 0.9f, 1.0f);
	private static Colorf placeOutlineColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf selectedPlaceOutlineColor = new Colorf(0.5f, 0.0f, 0.0f, 1.0f);
	private static Colorf tokenColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);

	private int token_count;
	
	private int placeType = 0;

	private LinkedList<EditableChetriTransition> out;
	private LinkedList<EditableChetriTransition> in;

	public LinkedList<EditableChetriTransition> getOut() {
		return (LinkedList<EditableChetriTransition>)out.clone();
	}

	public LinkedList<EditableChetriTransition> getIn() {
		return (LinkedList<EditableChetriTransition>)in.clone();
	}

	public void removeIn(EditableChetriTransition t) {
		in.remove(t);
	}

	public void removeOut(EditableChetriTransition t) {
		out.remove(t);
	}

	public boolean addIn(DefaultConnection con) {
		EditableChetriTransition t = (EditableChetriTransition)con.getFirst();
		if (in.contains(t))
			return false;
		in.add(t);
		connections.add(con);
		return true;
	}

	public boolean addOut(DefaultConnection con) {
		EditableChetriTransition t = (EditableChetriTransition)con.getSecond();
		if (out.contains(t))
			return false;
		out.add(t);
		connections.add(con);
		return true;
	}

	public void setTokens(Integer t) {
		token_count = t;
	}

	public int getTokens() {
		return token_count;
	}

	public void setType(Integer t) {
		placeType = t;
	}

	public int getType() {
		return placeType;
	}

	public EditableChetriPlace() {
		super();
		boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f));
		token_count = 0;
		placeType = 0;
		out = new LinkedList<EditableChetriTransition>();
		in = new LinkedList<EditableChetriTransition>();
	}

	public void draw(Painter p) {
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL);

		if (selected)
		{
			p.setFillColor(selectedPlaceOutlineColor);
		}
		else
			p.setFillColor(placeOutlineColor);

		p.drawCircle(0.05f, null);

		if (selected)
			p.setFillColor(selectedPlaceColor);
		else
		{
			if (placeType==0)
			{
				p.setFillColor(placeColor);
			}
			else
			if (placeType==1)
			{
				p.setFillColor(startPlaceColor);
			}
			else
			{
				p.setFillColor(finishPlaceColor);
			}
		}

		p.drawCircle(0.04f, null);
		
		p.setFillColor(tokenColor);

		if (token_count >0) {
			p.drawCircle(0.025f, null);
		} 
		super.draw(p);
	}

	
	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		list.add("int,Type,getType,setType");
		list.add("int,Tokens,getTokens,setTokens");
		return list;
	}

	public void fromXmlDom(Element element) {
		try {
			super.fromXmlDom(element);
		} catch (DuplicateIdException e) {
			e.printStackTrace();
		}
		NodeList nl = element.getElementsByTagName("place");
		Element ne = (Element) nl.item(0);
		setTokens(Integer.parseInt(ne.getAttribute("tokens")));
		setType(Integer.parseInt(ne.getAttribute("type")));
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("place");
		ppe.setAttribute("tokens", Integer.toString(getTokens()));
		ppe.setAttribute("type", Integer.toString(getType()));
		ee.appendChild(ppe);
		return ee;
	}

	@Override
	public BasicEditable getChildAt(Vec2 point) {
		// TODO Auto-generated method stub
		return null;
	}
}