package workcraft.mg;

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

public class MGPlace extends BasicEditable {
	public static final UUID _modeluuid = UUID.fromString("4fbdb830-dba3-11db-8314-0800200c9a66");
	public static final String _displayname = "Place";

	private static Colorf placeColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);
	private static Colorf selectedPlaceColor = new Colorf(1.0f, 0.9f, 0.9f, 1.0f);
	private static Colorf placeOutlineColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf selectedPlaceOutlineColor = new Colorf(0.5f, 0.0f, 0.0f, 1.0f);
	private static Colorf tokenColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);

	private int token_count;
	
	protected boolean critical = false;
	protected static boolean show_critical = true;
	
	protected int flag = 0;
	
	private GaussianDelay delay = new GaussianDelay();

	private LinkedList<MGTransition> out;
	private LinkedList<MGTransition> in;
	
	public LinkedList<MGTransition> getOut()
	{
		return (LinkedList<MGTransition>)out.clone();
	}

	public LinkedList<MGTransition> getIn()
	{
		return (LinkedList<MGTransition>)in.clone();
	}

	public void removeIn(MGTransition t) {
		in.remove(t);
	}

	public void removeOut(MGTransition t) {
		out.remove(t);
	}

	public boolean addIn(DefaultConnection con) {
		MGTransition t = (MGTransition)con.getFirst();
		if (in.contains(t))
			return false;
		in.add(t);
		connections.add(con);
		return true;
	}

	public boolean addOut(DefaultConnection con) {
		MGTransition t = (MGTransition)con.getSecond();
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
	
	public void setDelayMean(Double t)
	{
		delay.mean = t;
	}

	public double getDelayMean()
	{
		return delay.mean;
	}
	
	public void setDelayDev(Double t)
	{
		delay.dev = t;
	}

	public double getDelayDev()
	{
		return delay.dev;
	}

	public boolean getShowCritical()
	{
		return show_critical;
	}

	public void setShowCritical(Boolean b)
	{
		show_critical = b;
	}

	public MGPlace(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f));
		token_count = 0;
		out = new LinkedList<MGTransition>();
		in = new LinkedList<MGTransition>();
	}
	
	public void doDraw(Painter p) {
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL);

		if (selected)
			p.setFillColor(selectedPlaceOutlineColor);
		else
		if (critical && show_critical)
			p.setFillColor(MGModel.criticalColor);
		else
			p.setFillColor(placeOutlineColor);

		p.drawCircle(0.05f, null);

		if (selected)
			p.setFillColor(selectedPlaceColor);
		else
			p.setFillColor(placeColor);

		p.drawCircle(0.04f, null);
		
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
		list.add("double,delay.mean,getDelayMean,setDelayMean");
		list.add("double,delay.dev,getDelayDev,setDelayDev");
		list.add("color,^ Color,getPlaceColor,setPlaceColor");
		list.add("color,^ Token color,getTokenColor,setTokenColor");
		list.add("color,^ Outline color,getPlaceOutlineColor,setPlaceOutlineColor");
		return list;
	}

	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("place");
		Element ne = (Element) nl.item(0);
		setTokens(Integer.parseInt(ne.getAttribute("tokens")));
		setDelayMean(Double.parseDouble(ne.getAttribute("delay.mean")));
		setDelayDev(Double.parseDouble(ne.getAttribute("delay.dev")));
		super.fromXmlDom(element);
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("place");
		ppe.setAttribute("tokens", Integer.toString(getTokens()));
		ppe.setAttribute("delay.mean", Double.toString(getDelayMean()));
		ppe.setAttribute("delay.dev", Double.toString(getDelayDev()));
		ee.appendChild(ppe);
		return ee;
	}

	public static Colorf getPlaceColor() {
		return placeColor;
	}

	public static void setPlaceColor(Colorf placeColor) {
		MGPlace.placeColor = placeColor;
	}

	public static Colorf getTokenColor() {
		return tokenColor;
	}

	public static void setTokenColor(Colorf tokenColor) {
		MGPlace.tokenColor = tokenColor;
	}

	public static Colorf getPlaceOutlineColor() {
		return placeOutlineColor;
	}

	public static void setPlaceOutlineColor(Colorf placeOutlineColor) {
		MGPlace.placeOutlineColor = placeOutlineColor;
	}

}