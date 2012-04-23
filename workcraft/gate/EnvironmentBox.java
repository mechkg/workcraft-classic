package workcraft.gate;

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
import workcraft.editor.BasicEditable;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.TextAlign;

public class EnvironmentBox extends BasicEditable implements XmlSerializable {
	public static final UUID _modeluuid = UUID.fromString("6f704a28-e691-11db-8314-0800200c9a66");
	public static final String _displayname = "Environment";
	public static final String _hotkey = "e";
	public static final int _hotkeyvk = KeyEvent.VK_E;		

	private static final Colorf frameColor = new Colorf (0.0f, 0.0f, 0.0f, 1.0f);
	private static final Colorf fillColor = new Colorf (0.9f, 0.9f, 1.0f, 1.0f);
	private static final Colorf activeFillColor = new Colorf (0.9f, 1.0f, 1.0f, 1.0f);
	private static final Colorf selectedColor = new Colorf (0.5f, 0.0f, 0.0f, 1.0f);
	private static final Colorf activeColor = new Colorf (0.0f, 0.0f, 0.5f, 1.0f);
	private boolean active = false;

	private String environmentStg ="";

	public EnvironmentBox(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		boundingBox.setExtents(new Vec2(-0.1f, -0.05f), new Vec2(0.1f, 0.05f));
	}

	@Override
	public void doDraw(Painter p) {
		p.setTransform(transform.getLocalToViewMatrix());

		p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
		
		p.setLineMode(LineMode.SOLID);
		if (active) {
			p.setLineWidth(0.01f);
			p.setFillColor(activeFillColor);
			p.setTextColor(activeColor);
		}
		else {
			p.setLineWidth(0.005f);
			p.setFillColor(fillColor);
			p.setTextColor(frameColor);
		}

		if (selected)
			p.setLineColor(selectedColor);
		else
			if (highlight && ((ModelBase)ownerDocument).isShowHighlight())
				p.setLineColor(((ModelBase)ownerDocument).getHighlightColor());
			else
				if (active)
					p.setLineColor(activeColor);
				else
					p.setLineColor(frameColor);

		p.drawRect(-0.0975f, 0.0475f, 0.0975f, -0.0475f);
		Vec2 v0 = new Vec2(0.0f, -0.025f);
		transform.getLocalToViewMatrix().transform(v0);

		
		
		p.drawString("ENV", v0, 0.08f, TextAlign.CENTER);

		super.doDraw(p);
	}

	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		list.add("bool,Active,isActive,setActive");
		list.add("xwdfile,STG source,getEnvironmentStg,setEnvironmentStg");
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
		NodeList nl = element.getElementsByTagName("environment");
		Element te = (Element) nl.item(0);
		environmentStg = te.getAttribute("environment-stg-path");
		active = Boolean.parseBoolean(te.getAttribute("active"));
		super.fromXmlDom(element);
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("environment");
		ppe.setAttribute("environment-stg-path", environmentStg);
		ppe.setAttribute("active", Boolean.toString(active));
		ee.appendChild(ppe);
		return ee;
	}

	public String getEnvironmentStg() {
		return environmentStg;
	}

	public void setEnvironmentStg(String environmentStg) {
		this.environmentStg = environmentStg;
	}

	public Boolean isActive() {
		
		return active;
	}

	public void setActive(Boolean active) {
		if (active)
			((GateModel)ownerDocument).setActiveInterface(this);
		this.active = active;
	}

}
