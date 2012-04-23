package workcraft.gate;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.UnsupportedComponentException;
import workcraft.editor.BasicEditable;
import workcraft.util.Vec2;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.shapes.Bezier;
import workcraft.visual.shapes.CompoundPath;
import workcraft.visual.shapes.Shape;
import workcraft.visual.shapes.Vertex;

public class ComplexGate extends BasicGate {
	public static final UUID _modeluuid = UUID.fromString("6f704a28-e691-11db-8314-0800200c9a66");
	public static final String _displayname = "Complex gate";

	private boolean complexOrAnd = false;
	private Shape and_shape = null;
	private Shape or_shape = null;
	protected float subrad = 0.05f;
	
	private class SubGate {
		public float y;
		public LinkedList<GateContact> in;
		public SubGate() {
			in = new LinkedList<GateContact>();
		}
	}
	private LinkedList<SubGate> subGates;
	private int nSingle;
	private int aveContacts;
	
	public ComplexGate(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		subGates = new LinkedList<SubGate>();
		max_inputs = 0;
		min_outputs = 1;
		max_outputs = 1;
		initContacts();
		setConfig("22");
	}
	
	private Shape createOrShape() {
		float rx = (complexOrAnd)?subrad:rad*0.85f;
		float ry = (complexOrAnd)?subrad*0.95f:rad;
		CompoundPath p = new CompoundPath();
		p.addElement(new Bezier( new Vec2(-rx, -ry), new Vec2(0.0f, -ry),
			new Vec2(rx*0.75f, -ry*0.5f), new Vec2(rx, 0.0f) ));
		p.addElement(new Bezier( new Vec2(rx, 0.0f), new Vec2(rx*0.75f, ry*0.5f),
			new Vec2(0.0f, ry), new Vec2(-rx, ry) ));
		p.addElement(new Bezier( new Vec2(-rx, ry), new Vec2(-rx*0.5f, ry*0.5f),
			new Vec2(-rx*0.5f, -ry*0.5f), new Vec2(-rx, -ry) ));
		p.setClosed(true);
		return new Shape(p);
	}
	
	private Shape createAndShape() {
		float rx = ((!complexOrAnd)?subrad:rad)*0.75f;
		float ry = (!complexOrAnd)?subrad*0.9f:rad;;
		CompoundPath p = new CompoundPath();
		p.addElement(new Vertex(-rx, -ry));
		p.addElement(new Bezier(new Vec2(-rx*0.4f, -ry), new Vec2(rx*0.4f, -ry),
			new Vec2(rx, -ry*0.5f), new Vec2(rx, 0.0f) ));
		p.addElement(new Bezier(new Vec2(rx, 0.0f), new Vec2(rx, ry*0.5f),
			new Vec2(rx*0.4f, ry), new Vec2(-rx*0.4f, ry) ));
		p.addElement(new Vertex(-rx, ry));
		p.setClosed(true);
		return new Shape(p);
	}
	
	public void doDraw(Painter p) {
		super.doDraw(p);
		p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
		p.setLineMode(LineMode.SOLID);
		p.setLineWidth(0.0075f);
		p.setLineColor((selected)?selectedOutlineColor:(canWork)?userOutlineColor:outlineColor);
		p.setFillColor((selected)?selectedFillColor:(canFire)?enabledFillColor:(lostSignal)?lostSignalFillColor:fillColor);
		
		for(SubGate sg : subGates) {
			if(sg.in.size()<2)
				continue;
			p.setTransform(transform.getLocalToViewMatrix());
			p.translate(-rad*0.75f, -sg.y);
	        p.drawShape((complexOrAnd)?or_shape:and_shape);
		}
		
		p.setTransform(transform.getLocalToViewMatrix());
		p.translate(-((complexOrAnd)?-subrad:-subrad+rad*0.1f), 0.0f);
        p.drawShape((!complexOrAnd)?or_shape:and_shape);

		// draw invertion circles
		for(GateContact c : in) {
			c.doDrawInvert(p);
		}
		out.getFirst().doDrawInvert(p);
	}
	
	protected void updateContactOffsets() {
		if(fixed_radius>0) {
			subrad = fixed_radius*distContacts/2;
		}
		else {
			subrad = aveContacts*distContacts/2;
			if(subrad<0.05f)
				subrad = 0.05f;
		}
		rad = subrad*(subGates.size()-nSingle)+distContacts*nSingle/2;
		float xrad = rad*0.75f+subrad;
		boundingBox.setExtents(new Vec2(-xrad, -rad), new Vec2(xrad, rad));

		float ygstart = -rad;
		for(SubGate sg : subGates) {
			if(sg.in.size()<2) {
				sg.in.getFirst().setOffs(new Vec2(xrad+0.05f, ygstart+distContacts/2));
				if(!complexOrAnd) {
					float rx = rad*0.85f;
					float ry = rad;
					Bezier leftside = new Bezier(new Vec2(-rx, -ry), new Vec2(-rx*0.5f, -ry*0.5f),
					new Vec2(-rx*0.5f, ry*0.5f), new Vec2(-rx, ry));
					Vec2 pt = leftside.Point((ygstart+distContacts/2+rad)/2.0f/rad);
					pt.sub(new Vec2(-subrad+rad*0.1f, 0.0f));
					sg.in.getFirst().offsInvX = -pt.getX();
				}
				else {
					sg.in.getFirst().offsInvX = xrad-subrad*2;
				}
				ygstart += distContacts;
			}
			else {
				int i=0;
				float dc = 2*subrad/sg.in.size();
				float ystart = -((sg.in.size()-1)*dc)/2;
				sg.y = ygstart+subrad;
				for(GateContact c : sg.in) {
					Vec2 offs = new Vec2();
					offs.setXY(xrad+0.05f, ygstart+subrad+ystart+i*dc);
					c.setOffs(offs);
					i++;
				}
				if(complexOrAnd) {
					float rx = subrad;
					float ry = subrad*0.95f;
					Bezier leftside = new Bezier(new Vec2(-rx, -ry), new Vec2(-rx*0.5f, -ry*0.5f),
						new Vec2(-rx*0.5f, ry*0.5f), new Vec2(-rx, ry));
					i=0;
					for(GateContact c : sg.in) {
						Vec2 pt = leftside.Point((ystart+i*dc+subrad)/2.0f/subrad);
						pt.sub(new Vec2(rad*0.75f, sg.y));
						c.offsInvX = -pt.getX();
						i++;
					}
				}
				else {
					for(GateContact c : sg.in) {
						c.offsInvX = xrad-subrad*0.25f;
					}
				}
				ygstart += subrad*2;
			}
		}
		
		out.getFirst().setOffs(new Vec2(-xrad-0.05f, 0.0f));
		out.getFirst().offsInvX = -xrad;
		
		and_shape = createAndShape();
		or_shape = createOrShape();
	}

	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		list.add("enum,Gate type,getComplexType,setComplexType,AND-OR,OR-AND");
		list.add("str,Configuration,getConfig,setConfig");
		return list;
	}
	
	public Integer getComplexType() {
		return (complexOrAnd)?1:0;
	}
	
	public void setComplexType(Integer i) {
		complexOrAnd = (i==1);
		updateContactOffsets();
	}
	
	public String getConfig() {
		String res = "";
		for(SubGate s:subGates) {
			res += Integer.toString(s.in.size());
		}
		return res;
	}
	
	public void setConfig(String str) {
		if(str.length()<1)
			return;
		int ini = 0;
		LinkedList<SubGate> newSubGates = new LinkedList<SubGate>();
		nSingle = 0;
		aveContacts = 0;
		for(int i=0; i<str.length(); i++) {
			int c;
			try {
				c = Integer.parseInt(str.substring(i, i+1));
			}
			catch(NumberFormatException e) {
				c = 0;
			}
			if(c<1)
				return;
			SubGate sg = new SubGate();
			if(c==1)
				nSingle++;
			else
				aveContacts += c;
			for(int j=0; j<c; j++) {
				if(ini<in.size())
					sg.in.add(in.get(ini));
				else {
					try {
						sg.in.add(addInput());
					}
					catch(UnsupportedComponentException e) {
						e.printStackTrace();
					}
				}
				ini++;
			}
			newSubGates.add(sg);
		}
		subGates = newSubGates;
		aveContacts = (aveContacts>0)?Math.round((float)aveContacts/(float)(subGates.size()-nSingle)):0;
		while(ini<in.size()) {
			try {
				GateContact c = in.removeLast();
				ownerDocument.removeComponent(c);
				//ownerDocument.getServer().unregisterObject(c.getId());
			}
			catch(UnsupportedComponentException e) {
				e.printStackTrace();
			}
		}
		min_inputs = max_inputs = in.size();
		updateContactOffsets();
	}

	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("complex");
		Element ne = (Element) nl.item(0);
		complexOrAnd = Boolean.parseBoolean(ne.getAttribute("type"));
		setConfig(ne.getAttribute("config"));
		super.fromXmlDom(element);
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("complex");
		ppe.setAttribute("type", Boolean.toString(complexOrAnd));
		ppe.setAttribute("config", getConfig());
		ee.appendChild(ppe);
		return ee;
	}
	
	public void refresh() {
		GateContact.StateType res = (complexOrAnd)?GateContact.StateType.set:GateContact.StateType.reset;
		for(SubGate sg : subGates) {
			GateContact.StateType inres = (complexOrAnd)?GateContact.StateType.reset:GateContact.StateType.set;
			for(GateContact cin : sg.in) {
				if(cin.getState()==GateContact.StateType.reset) {
					if(!complexOrAnd) {
						inres = GateContact.StateType.reset;
						break;
					}
				}
				else {
					if(complexOrAnd) {
						inres = GateContact.StateType.set;
						break;
					}
				}
			}
			if(inres==GateContact.StateType.reset) {
				if(complexOrAnd) {
					res = GateContact.StateType.reset;
					break;
				}
			}
			else {
				if(!complexOrAnd) {
					res = GateContact.StateType.set;
					break;
				}
			}
		}
		out.getFirst().setState(res);
		super.refresh();
	}

	public Boolean isSet() {
		return (out.getFirst().getState()==GateContact.StateType.set);
	}
	
	private String getFunction(String self) {
		boolean first = true;
		String s = "";
		for(SubGate sg : subGates) {
			if(!first)
				s += (complexOrAnd)?" & ":" | ";
			first = false;
			boolean sfirst = true;
			s += "(";
			for(GateContact c : sg.in) {
				if(!sfirst)
					s += (!complexOrAnd)?" & ":" | ";
				sfirst = false;
				s += c.getSrcFunction(self);
			}
			s += ")";
		}
		
		if (out.getFirst().getInvertSignal())
			s = "not ("+s+")";
		
		return s;
	}
	
	public String getSetFunction() {
		return getFunction("ZERO");
	}

	public String getResetFunction() {
		return "not("+getFunction("ONE")+")";
	}
	
	public Integer getSubGatesNumber() {
		return subGates.size();
	}
	
	public Integer getSubGateInputsNumber(Integer nsub) {
		return subGates.get(nsub).in.size();
	}
	
	public GateContact getSubGateInput(Integer nsub, Integer ninput) {
		return subGates.get(nsub).in.get(ninput);
	}
}
