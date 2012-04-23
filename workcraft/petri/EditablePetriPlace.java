package workcraft.petri;

import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.UnsupportedComponentException;
import workcraft.common.DefaultConnection;
import workcraft.editor.BasicEditable;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.TextAlign;

public class EditablePetriPlace extends BasicEditable {
	public static final UUID _modeluuid = UUID.fromString("65f89260-641d-11db-bd13-0800200c9a66");
	public static final String _displayname = "Place";
	public static final String _hotkey = "p";
	public static final int _hotkeyvk = KeyEvent.VK_P;

	private static Colorf placeColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);
	private static Colorf selectedPlaceColor = new Colorf(1.0f, 0.9f, 0.9f, 1.0f);
	private static Colorf placeOutlineColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf selectedPlaceOutlineColor = new Colorf(0.5f, 0.0f, 0.0f, 1.0f);
	private static Colorf tokenColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);

	private int token_count;
	private Boolean tiny = false;

	private LinkedList<EditablePetriTransition> out;
	private LinkedList<EditablePetriTransition> in;
	public void dblClick() {
		if (getTokens()==0) {
			setTokens(1);
		} else
			setTokens(0);
			
	}

	public LinkedList<EditablePetriTransition> getOut() {
		return (LinkedList<EditablePetriTransition>)out.clone();
	}

	public LinkedList<EditablePetriTransition> getIn() {
		return (LinkedList<EditablePetriTransition>)in.clone();
	}

	public void removeIn(EditablePetriTransition t) {
		in.remove(t);
	}

	public void removeOut(EditablePetriTransition t) {
		out.remove(t);
	}
	
	
	@Override
	public boolean hits(Vec2 pointInViewSpace) {
		// don't draw incoming arrow when it is
		// the shorthand notation and exactly one input and one output 
		if (getIsDrawPlaceCircle())
			return true;
		/////////
		
		Vec2 v = new Vec2(pointInViewSpace);
		transform.getViewToLocalMatrix().transform(v);
		return v.length() < 0.05f*(tiny?0.4:1);
	}

	public boolean addIn(DefaultConnection con) {
		EditablePetriTransition t = (EditablePetriTransition)con.getFirst();
		if (in.contains(t))
			return false;
		in.add(t);
		connections.add(con);
		return true;
	}

	public boolean addOut(DefaultConnection con) {
		EditablePetriTransition t = (EditablePetriTransition)con.getSecond();
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

	public EditablePetriPlace(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f));
		token_count = 0;
		out = new LinkedList<EditablePetriTransition>();
		in = new LinkedList<EditablePetriTransition>();
	}
	
	private boolean getIsDrawPlaceCircle() {
		return getIsShorthandNotation() && 
			in!=null && in.size()==1 && 
			out!=null && out.size()==1; 
	}

	public void doDraw(Painter p) {

		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL);
		
		if (tiny ) {
			p.scale(0.4f, 0.4f);
		}
		
		if (selected)
			p.setFillColor(selectedPlaceOutlineColor);
		else
			p.setFillColor(placeOutlineColor);
		
		
		if ( getIsDrawPlaceCircle() ) {
			// shorthand notation does not draw places with exactly one input and one output
			
			if (selected) {
				p.setFillColor(selectedPlaceColor);
				p.drawCircle(0.04f, null);
			}
		} else {
			
			p.drawCircle(0.05f, null);
			
			if (selected)
				p.setFillColor(selectedPlaceColor);
			else
				p.setFillColor(placeColor);
			p.drawCircle(0.04f, null);
		}
		
		p.setFillColor(tokenColor);

		if (token_count == 1) {
			p.drawCircle(0.025f, null);
		} else {
			if (token_count < 6) {
				float R = 0.02f;
				float r = 0.0275f / (float)(Math.pow(1.2, token_count));
				float delta = (float)Math.PI*2.0f / (float)token_count;
				
				p.setShapeMode(ShapeMode.FILL);

				for (int i=0; i<token_count; i++) {
					p.pushTransform();
					p.translate((float)Math.cos(i*delta)*R, (float)Math.sin(i*delta)*R);
					p.drawCircle(r, null);
					p.popTransform();
				}
			} else {
				String n = Integer.toString(token_count);
				float h = 0.04f;
				Vec2 v = new Vec2(0, -h/3.5f);
				transform.getLocalToViewMatrix().transform(v);
				p.setTextColor(tokenColor);
				p.drawString(n, v, h, TextAlign.CENTER);
			}
		}
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

	
	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		list.add("int,Tokens,getTokens,setTokens");
		list.add("bool,Tiny place,getTiny,setTiny");
		list.add("color,^ Color,getPlaceColor,setPlaceColor");
		list.add("color,^ Token color,getTokenColor,setTokenColor");
		list.add("color,^ Outline color,getPlaceOutlineColor,setPlaceOutlineColor");
		return list;
	}

	@Override
	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("place");
		Element ne = (Element) nl.item(0);
		setTokens(Integer.parseInt(ne.getAttribute("tokens")));
		if (ne.hasAttribute("tiny"))
			setTiny (Boolean.parseBoolean(ne.getAttribute("tiny")));
		super.fromXmlDom(element);
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("place");
		ppe.setAttribute("tokens", Integer.toString(getTokens()));
		ppe.setAttribute("tiny", Boolean.toString(getTiny()));
		ee.appendChild(ppe);
		return ee;
	}

	public static Colorf getPlaceColor() {
		return placeColor;
	}

	public static void setPlaceColor(Colorf placeColor) {
		EditablePetriPlace.placeColor = placeColor;
	}

	public static Colorf getTokenColor() {
		return tokenColor;
	}

	public static void setTokenColor(Colorf tokenColor) {
		EditablePetriPlace.tokenColor = tokenColor;
	}

	public static Colorf getPlaceOutlineColor() {
		return placeOutlineColor;
	}

	public static void setPlaceOutlineColor(Colorf placeOutlineColor) {
		EditablePetriPlace.placeOutlineColor = placeOutlineColor;
	}

	public Boolean getTiny() {
		return tiny;
	}

	public void setTiny(Boolean tiny) {
		this.tiny = tiny;
//		if (tiny) boundingBox.setExtents(new Vec2(-0.05f * 0.6f, -0.05f * 0.6f), new Vec2(0.05f * 0.6f, 0.05f * 0.6f));
//		else boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f));
	}

}