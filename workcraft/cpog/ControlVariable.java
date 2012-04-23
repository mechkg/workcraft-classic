package workcraft.cpog;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.UUID;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.ModelBase;
import workcraft.UnsupportedComponentException;
import workcraft.XmlSerializable;
import workcraft.common.DefaultConnection;
import workcraft.editor.BasicEditable;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.TextAlign;

public class ControlVariable extends BasicEditable implements XmlSerializable
{
	public static final UUID _modeluuid = UUID.fromString("25787b48-9c3d-11dc-8314-0800200c9a66");
	public static final String _displayname = "Control Variable";
	public static final String _hotkey = " x ";
	public static final int _hotkeyvk = KeyEvent.VK_X;		

	private static final Colorf frameColor = new Colorf (0.0f, 0.0f, 0.0f, 1.0f);
	private static final Colorf extFillColor = new Colorf (0.9f, 0.9f, 1.0f, 1.0f);
	private static final Colorf intFillColor = new Colorf (0.9f, 1.0f, 1.0f, 1.0f);
	private static final Colorf selectedColor = new Colorf (0.5f, 0.0f, 0.0f, 1.0f);
	private static final Colorf intColor = new Colorf (0.0f, 0.0f, 0.5f, 1.0f);

	public ControlVariable(BasicEditable parent) throws UnsupportedComponentException
	{
		super(parent);
		boundingBox.setExtents(new Vec2(-0.03f, -0.03f), new Vec2(0.03f, 0.03f));
		setInitialValue(false);
		setFinalValue(false);
	}

	public ControlVariable()
	{
		setInitialValue(false);
		setFinalValue(false);
	}

	private Vertex masterVertex = null;
	
	private Boolean initialValue = false;
	private Boolean currentValue = false;
	private Boolean finalValue = false;

	public Vertex getMasterVertex()
	{
		return masterVertex;
	}
	
	public boolean addVertex(DefaultConnection con)
	{
		Vertex t = (Vertex)con.getFirst();
		if (masterVertex != null && masterVertex != t)
			return false;
		connections.add(con);
		return true;
	}	

	public void setMasterVertex(Vertex controlVertex)
	{
		this.masterVertex = controlVertex;		
	}

	@Override
	public void doDraw(Painter p) {
		p.setTransform(transform.getLocalToViewMatrix());

		p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
		
	
		p.setLineMode(LineMode.HAIRLINE);
		//p.setLineWidth(0.005f);
		
		if (masterVertex != null)
		{
			p.setFillColor(intFillColor);
			p.setTextColor(intColor);
		}
		else
		{
			p.setFillColor(extFillColor);
			p.setTextColor(frameColor);
		}

		if (selected)
			p.setLineColor(selectedColor);
		else
			if (masterVertex != null)
				p.setLineColor(intColor);
			else
				p.setLineColor(frameColor);

		p.drawRect(-0.03f, 0.03f, 0.03f, -0.03f);
		
		Vec2 v0 = new Vec2(0.0f, -0.02f);
		transform.getLocalToViewMatrix().transform(v0);

		String s = "0";
		if (currentValue) s = "1";
		
		p.drawString(s, v0, 0.07f, TextAlign.CENTER);		

		super.doDraw(p);
	}

	public String getMasterVertexID()
	{
		if (masterVertex == null) return "";
		return masterVertex.getId(); 
	}
	
	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		
		list.add("bool,Inital value,getInitialValue,setInitialValue");
		list.add("bool,Final value,getFinalValue,setFinalValue");
		
		list.add("str,Master vertex,getMasterVertexID,-");
		return list;
	}
	
	@Override
	public BasicEditable getChildAt(Vec2 point) {
		return null;
	}


	@Override
	public void update(Mat4x4 matView) {
		// TODO Auto-generated method stub

	}
	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("signal");
		Element ne = (Element) nl.item(0);
		setInitialValue(Boolean.parseBoolean(ne.getAttribute("initial")));
		setFinalValue(Boolean.parseBoolean(ne.getAttribute("final")));
		super.fromXmlDom(element);
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("signal");
		ppe.setAttribute("initial", getInitialValue().toString());
		ppe.setAttribute("final", getFinalValue().toString());
		ee.appendChild(ppe);
		return ee;
	}

	
	public Boolean getInitialValue()
	{
		return initialValue;
	}

	public void setInitialValue(Boolean initialValue)
	{
		this.initialValue = initialValue;
		if (masterVertex == null || !masterVertex.fired) setCurrentValue(initialValue);
	}
	
	public Boolean getCurrentValue() 
	{
		return currentValue;
	}

	public void setCurrentValue(Boolean currentValue) 
	{
		this.currentValue = currentValue;
		((CPOGModel)ownerDocument).refreshControlValues();
	}

	public Boolean getFinalValue()
	{
		return finalValue;
	}

	public void setFinalValue(Boolean finalValue)
	{
		this.finalValue = finalValue;
		if (masterVertex != null && masterVertex.fired) setCurrentValue(initialValue);
	}

}
