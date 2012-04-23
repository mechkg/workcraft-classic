package workcraft.common;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.editor.BasicEditable;
import workcraft.util.Colorf;
import workcraft.util.Vec2;
import workcraft.visual.Painter;
import workcraft.visual.TextAlign;

public class LabelledConnection extends DefaultConnection
{
	private static Colorf labelColor = new Colorf (0.0f, 0.0f, 0.0f, 1.0f);
	
	private String label;
	
	public LabelledConnection()
	{
		this(null, null);
	}

	public LabelledConnection(BasicEditable first, BasicEditable second)
	{
		super(first, second);
		label = "no label";
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}
	
	public List<String> getEditableProperties()
	{
		List<String> list = super.getEditableProperties();

		list.add("str,Label,getLabel,setLabel");

		return list;
	}
	
	public void draw(Painter p)
	{
		super.draw(p);
		
		updateStretch();
		p.pushTransform();
		p.setIdentityTransform();

		Vec2 v = getPointOnConnection(0.5f);
		
		v.setY(v.getY() + 0.03f);		

		p.setTextColor(labelColor);
		p.drawString(label, v, 0.04f, TextAlign.CENTER);
		
		p.popTransform();
	}
	
	public Element toXmlDom(Element parent_element)
	{
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("label");
		ppe.setAttribute("text", getLabel());
		ee.appendChild(ppe);
		return ee;
	}

	public void fromXmlDom(Element element)
	{
		NodeList nl = element.getElementsByTagName("label");
		Element ne = (Element) nl.item(0);
		setLabel(ne.getAttribute("text"));
		super.fromXmlDom(element);
	}	
}
