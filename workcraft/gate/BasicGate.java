package workcraft.gate;

import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.editor.BasicEditable;
import workcraft.gate.GateContact.IOType;
import workcraft.util.Colorf;
import workcraft.util.Vec2;
import workcraft.visual.Painter;
import workcraft.DuplicateIdException;
import workcraft.Model;
import workcraft.UnsupportedComponentException;

public abstract class BasicGate extends BasicEditable {
	protected LinkedList<GateContact> out;
	protected LinkedList<GateContact> in;
	protected float rad = 0.05f;

	protected static Colorf fillColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);
	protected static Colorf selectedFillColor = new Colorf(1.0f, 0.9f, 0.9f, 1.0f);
	protected static Colorf outlineColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	protected static Colorf selectedOutlineColor = new Colorf(0.5f, 0.0f, 0.0f, 1.0f);
	protected static Colorf enabledFillColor = new Colorf(0.6f, 0.6f, 1.0f, 1.0f);
	protected static Colorf lostSignalFillColor = new Colorf(1.0f, 0.8f, 0.6f, 1.0f);
	protected static Colorf userOutlineColor = new Colorf(0.0f, 0.6f, 0.0f, 1.0f);

	protected static float distContacts = 0.05f;

	protected int min_inputs = 0;
	protected int max_inputs = -1;
	protected int min_outputs = 0;
	protected int max_outputs = -1;

	private int rotate = 0;
	protected int fixed_radius = 0;

	public boolean canFire = false;
	public boolean lostSignal = false;
	public boolean canWork = false;

	protected double delayMin = 1.0;
	protected double delayMax = 2.0;

	public BasicGate(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		in = new LinkedList<GateContact>();
		out = new LinkedList<GateContact>();
	}

	protected void initContacts() throws UnsupportedComponentException {
		for(int i=0; i<min_inputs; i++)
			addInput();
		for(int i=0; i<min_outputs; i++)
			addOutput();
		updateContactOffsets();
	}

	public void disposeContacts() throws UnsupportedComponentException {
		min_inputs = -1;
		min_outputs = -1;
		for(GateContact c : in) {
			ownerDocument.removeComponent(c);
			ownerDocument.getServer().unregisterObject(c.getId());
		}
		for(GateContact c : out) {
			ownerDocument.removeComponent(c);
			ownerDocument.getServer().unregisterObject(c.getId());
		}
		in.clear();
		out.clear();
		removeAllChildren();
	}

	protected GateContact addInput() throws UnsupportedComponentException {
		GateContact c = new GateContact(this, GateContact.IOType.input);
		in.add(c);
		// addChild(c);
		return c;
	}

	protected GateContact addOutput() throws UnsupportedComponentException {
		GateContact c = new GateContact(this, GateContact.IOType.output);
		out.add(c);
		// addChild(c);
		return c;
	}

	public void removeContact(GateContact c) {
		if(c.getIOType()==GateContact.IOType.input) {
			if(min_inputs<0 || in.size()<=min_inputs)
				return;
			in.remove(c);
			removeChild(c);
		}
		else {
			if(min_outputs<0 || out.size()<=min_outputs)
				return;
			out.remove(c);
			removeChild(c);
		}
		updateContactOffsets();
	}

	public GateContact getFreeInput() throws UnsupportedComponentException {
		for(GateContact c : in) {
			if(c.connections.size()==0)
				return c;
		}
		if(max_inputs<0 || in.size()<max_inputs) {
			GateContact c =  addInput();
			updateContactOffsets();
			return c;
		}
		return null;
	}

	public GateContact getFreeOutput() throws UnsupportedComponentException {
		for(GateContact c : out) {
			if(c.connections.size()==0)
				return c;
		}
		if(max_outputs<0 || out.size()<max_outputs) {
			GateContact c =  addOutput();
			updateContactOffsets();
			return c;
		}
		return null;
	}

	public void doDraw(Painter p) {
		p.setTransform(transform.getLocalToViewMatrix());
		for(GateContact c : in) {
			c.doDraw(p);
		}
		for(GateContact c : out) {
			c.doDraw(p);
		}

		super.doDraw(p);
	}

	protected void updateContactOffsets() {
		float dc = distContacts;
		if(fixed_radius>0) {
			rad = fixed_radius*distContacts/2;
			dc = 2*rad/Math.max(Math.max(in.size(), out.size()), 1);
		}
		else {
			rad = Math.max(in.size(), out.size())*distContacts/2;
			if(rad<0.05f)
				rad = 0.05f;
		}
		boundingBox.setExtents(new Vec2(-rad, -rad), new Vec2(rad, rad));
		int i=0;
		float ystart = -((in.size()-1)*dc)/2; 
		for(GateContact c : in) {
			Vec2 offs = new Vec2();
			offs.setXY(rad+0.05f, ystart+i*dc);
			c.setOffs(offs);
			i++;
		}
		i = 0;
		ystart = -((out.size()-1)*dc)/2; 
		for(GateContact c : out) {
			Vec2 offs = new Vec2();
			offs.setXY(-rad-0.05f, ystart+i*dc);
			c.setOffs(offs);
			i++;
		}
	}

	public void acceptTransform() {
		for(GateContact c : in) {
			c.acceptTransform();
		}
		for(GateContact c : out) {
			c.acceptTransform();
		}
	}

	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		list.add("enum,Rotate,getRotate,setRotate,0,90,180,270");
		list.add("int,Radius,getRadius,setRadius");
		list.add("double,Delay min,getDelayMin,setDelayMin");
		list.add("double,Delay max,getDelayMax,setDelayMax");
		list.add("str,Set function,getSetFunction,-");
		list.add("str,Reset function,getResetFunction,-");
		return list;
	}

	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("gate");
		Element ne = (Element) nl.item(0);
		fixed_radius = Integer.parseInt(ne.getAttribute("radius"));

		String s = ne.getAttribute("delay-min");
		if (s.length()>0)
			delayMin = Double.parseDouble(s);

		s = ne.getAttribute("delay-max");
		if (s.length()>0)
			delayMax = Double.parseDouble(s);

		try {
			nl = ne.getElementsByTagName("contact");
			for (int i=0; i<nl.getLength(); i++) {
				Element e = (Element)nl.item(i);
				GateContact.IOType io_type = IOType.valueOf(e.getAttribute("type"));
				int index = Integer.valueOf(e.getAttribute("index"));
				GateContact con = (io_type==GateContact.IOType.input)?
						((index>=in.size())?addInput():in.get(index)):
							((index>=out.size())?addOutput():out.get(index));
						con.fromXmlDom(e);
			}
		}
		catch(UnsupportedComponentException ex) {
			ex.printStackTrace();
		}
		super.fromXmlDom(element);
		updateContactOffsets();
		acceptTransform();
	}


	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("gate");
		ppe.setAttribute("radius", Integer.toString(fixed_radius));
		ppe.setAttribute("delay-min", Double.toString(delayMin));
		ppe.setAttribute("delay-max", Double.toString(delayMax));
		ee.appendChild(ppe);
		for (GateContact con: in) {
			con.toXmlDom(ppe);
		}
		for (GateContact con: out) {
			con.toXmlDom(ppe);
		}
		return ee;
	}

	@Override
	public BasicEditable getChildAt(Vec2 point) {
		for(GateContact c : in) {
			if(c.hitsBB(point))
				return c;
		}
		for(GateContact c : out) {
			if(c.hitsBB(point))
				return c;
		}
		return null;
	}

	public Integer getRotate() {
		return rotate;
	}

	public void setRotate(Integer rotate) {
		this.rotate = rotate;
		this.transform.rotateZ(rotate*90);
	}

	public Integer getRadius() {
		return fixed_radius;
	}

	public void setRadius(Integer radius) {
		fixed_radius = (radius>0)?radius:0;
		updateContactOffsets();
	}

	public List<GateContact> getOutputContacts() {
		return out;	
	}
	public List<GateContact> getInputContacts() {
		return in;
	}

	public void reset() {
		for(GateContact con : in)
			con.reset();
	}

	public void refresh() {
		boolean newCanFire = false;
		for(GateContact con : out) {
			if(con.isExcited()) {
				newCanFire = true;
				break;
			}
		}
		lostSignal = canFire && !newCanFire;
		canFire = newCanFire; 
	}

	public void fire() {
		for(GateContact con : out) {
			con.fire();
		}
		canFire = false;
		canWork = false;
	}

	public void simAction(int flag) {
		if (flag == MouseEvent.BUTTON1) {
			canWork = !canWork;
		}
	}	

	public String getSetFunction() {
		return "ONE";
	}

	public String getResetFunction() {
		return "ONE";
	}

	public Double getDelayMin() {
		return delayMin;
	}

	public void setDelayMin(Double delayMin) {
		this.delayMin = delayMin;
	}

	public Double getDelayMax() {
		return delayMax;
	}

	public void setDelayMax(Double delayMax) {
		this.delayMax = delayMax;
	}
	
	public String getPlusTag () {
		return "bd " + delayMin+ " " + delayMax;
		
	}

	public String getMinusTag () {
		return "bd 0 0";		
	}
}
