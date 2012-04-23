package workcraft.cpog;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.common.DefaultConnection;
import workcraft.editor.BasicEditable;
import workcraft.logic.DNF;
import workcraft.logic.InvalidExpressionException;
import workcraft.util.Colorf;
import workcraft.util.Vec2;
import workcraft.visual.Painter;
import workcraft.visual.TextAlign;

public class Arc extends DefaultConnection
{
	private static Colorf connectionColor = new Colorf (0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf passiveConnectionColor = new Colorf(0.6f, 0.6f, 0.6f, 1.0f);
	
	private static Colorf conditionColor = new Colorf (0.7f, 0.1f, 0.0f, 1.0f);
	
	private Condition condition;
	
	private CPOGModel ownerDocument;
	
	public Boolean isActive()
	{
		return ((Vertex)first).isActive() && ((Vertex)second).isActive() && condition.evaluate();
	}

	public Arc(CPOGModel ownerDocument)
	{
		this(null, null, ownerDocument);
	}

	public Arc(BasicEditable first, BasicEditable second, CPOGModel ownerDocument)
	{
		super(first, second);
		this.ownerDocument = ownerDocument;
		colorOverride = connectionColor;
		condition = new Condition("1", ownerDocument);
	}

	public void refresh()
	{
		if (condition != null)
		{
			condition.refresh();
			if (isActive())
			{
				colorOverride = connectionColor;
			}
			else
			{
				colorOverride = passiveConnectionColor;
			}
		}
	}
	
	public String getCondition()
	{
		return condition.string;
	}

	public void setCondition(String condition)
	{
		this.condition.setCondition(condition);
		if (isActive())
		{
			colorOverride = connectionColor;
		}
		else
		{
			colorOverride = passiveConnectionColor;
		}
	}
	
	public List<String> getEditableProperties()
	{
		List<String> list = super.getEditableProperties();

		list.add("str,Condition,getCondition,setCondition");
		list.add("bool,Active,isActive,-");

		return list;
	}
	
	public void draw(Painter p)
	{
		super.draw(p);
		
		if (!condition.string.equals("1"))
		{
			updateStretch();
			p.pushTransform();
			p.setIdentityTransform();
	
			Vec2 v = getPointOnConnection(0.5f);
			
			v.setY(v.getY() + 0.03f);		
	
			p.setTextColor(conditionColor);
			p.drawString(condition.string, v, 0.05f, TextAlign.CENTER);
			
			p.popTransform();
		}
	}
	
	public Element toXmlDom(Element parent_element)
	{
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("condition");
		ppe.setAttribute("text", getCondition());
		ee.appendChild(ppe);
		return ee;
	}

	public void fromXmlDom(Element element)
	{
		NodeList nl = element.getElementsByTagName("condition");
		Element ne = (Element) nl.item(0);
		setCondition(ne.getAttribute("text"));
		super.fromXmlDom(element);
	}	
}
