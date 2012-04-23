package workcraft.gate;

import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import workcraft.DuplicateIdException;
import workcraft.Model;
import workcraft.UnsupportedComponentException;
import workcraft.editor.BasicEditable;
import workcraft.editor.TransformNode;
import workcraft.util.Colorf;
import workcraft.util.Vec2;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;

public class GateContact extends BasicEditable {
	public enum StateType { reset, set };
	public enum IOType { input, output };
	private StateType state = StateType.reset;
	private StateType next_state = StateType.reset;
	private IOType io_type;
	private boolean invert_signal = false; 


	private Vec2 offs = null;
	public float offsInvX = 0.0f;

	public GateContact srcContact = null;

	private static Colorf lineColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf outlineColor = new Colorf(0.75f, 0.75f, 0.75f, 1.0f);
	private static Colorf selectedOutlineColor = new Colorf(0.5f, 0.0f, 0.0f, 1.0f);
	private static Colorf fillLowColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);
	private static Colorf fillHighColor = new Colorf(0.0f, 1.0f, 0.0f, 1.0f);

	private static Colorf outlineInvertColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf fillInvertColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);

	private static boolean showContactRects = true;

	public GateContact(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		boundingBox.setExtents(new Vec2(-0.01f, -0.01f), new Vec2(0.01f, 0.01f));
	}

	public GateContact(BasicGate parent, IOType type)
	throws UnsupportedComponentException {
		super(parent);
		boundingBox.setExtents(new Vec2(-0.01f, -0.01f), new Vec2(0.01f, 0.01f));
		io_type = type;
	}

	public void acceptTransform() {
		if(offs==null) {
			offs = new Vec2(0.0f, 0.0f);
		}
		transform.translateAbs(offs);
	}

	public void dblClick() {
		this.setInvertSignal(!getInvertSignal());
	}

	public void doDraw(Painter p) {
		acceptTransform();
		p.setTransform(transform.getLocalToViewMatrix());

		p.setLineMode(LineMode.HAIRLINE);
		p.setLineColor(lineColor);
		p.drawLine(0.0f, 0.0f, offs.getX()-offsInvX, 0.0f);

		if(!showContactRects)
			return;
		p.setShapeMode(ShapeMode.FILL);

		boolean set = (getWireState()==StateType.set)?true:false;

		p.setFillColor(set?fillHighColor:fillLowColor);
		p.drawRect(-0.01f, 0.01f, 0.01f, -0.01f);

		p.setShapeMode(ShapeMode.OUTLINE);
		p.setLineColor((selected)?selectedOutlineColor:outlineColor);
		p.drawRect(-0.01f, 0.01f, 0.01f, -0.01f);
	}

	public void doDrawInvert(Painter p) {
		if(invert_signal) {
			p.setTransform(transform.getLocalToViewMatrix());
			p.translate(this.offs.getX()-offsInvX, 0.0f);

			p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
			p.setLineMode(LineMode.SOLID);
			p.setLineWidth(0.005f);
			p.setFillColor(fillInvertColor);
			p.setLineColor(outlineInvertColor);
			p.drawCircle(0.012f, null);
		}
	}

	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		list.add("bool,Invert signal,getInvertSignal,setInvertSignal");
		list.add("bool,^Show contacts,getShowContactRects,setShowContactRects");
		return list;
	}

	public void fromXmlDom(Element element) throws DuplicateIdException {
		io_type = IOType.valueOf(element.getAttribute("type"));
		state = StateType.valueOf(element.getAttribute("state"));
		invert_signal = Boolean.valueOf(element.getAttribute("invert"));
		String id = element.getAttribute("id");

		HashMap<String, String> renamed = null;

		if (ownerDocument.getServer() != null) {

			if (ownerDocument.getServer().python.get("_pasting").__nonzero__())
				renamed = (HashMap<String, String>) ownerDocument.getServer().python.get("_renamed", HashMap.class);
		}

		if(renamed==null) {
			if(!id.equals(this.id)) {
				/*				if((BasicEditable) ownerDocument.getServer().getObjectById(this.id)==this)
					ownerDocument.getServer().unregisterObject(this.id);*/
				if (ownerDocument.getServer() != null)
					ownerDocument.getServer().unregisterObject(id);
				setId(id);
			}
		}
		else if(!id.equals(this.id))
			renamed.put(id, this.id);
	}

	public Element toXmlDom(Element parent_element) {
		Document d = parent_element.getOwnerDocument();
		Element ee = d.createElement("contact");
		ee.setAttribute("id", getId());
		parent_element.appendChild(ee);

		ee.setAttribute("type", io_type.name());
		ee.setAttribute("state", state.name());
		ee.setAttribute("invert", Boolean.toString(invert_signal));
		if(io_type==IOType.input) {
			ee.setAttribute("index", Integer.toString(getParentGate().in.indexOf(this)));
		}
		else {
			ee.setAttribute("index", Integer.toString(getParentGate().out.indexOf(this)));
		}
		return ee;
	}

	public Boolean getInvertSignal() {
		return invert_signal;
	}

	public void setInvertSignal(Boolean value) {
		invert_signal = value;
	}

	public Boolean getShowContactRects() {
		return showContactRects;
	}

	public void setShowContactRects(Boolean value) {
		showContactRects = value;
	}

	@Override
	public BasicEditable getChildAt(Vec2 point) {
		// TODO Auto-generated method stub
		return null;
	}

	public IOType getIOType() {
		return io_type;
	}

	public Vec2 getOffs() {
		return new Vec2(offs);
	}

	public void setOffs(Vec2 offs) {
		this.offs = new Vec2(offs); 
		acceptTransform();
	}

	public BasicGate getParentGate() {
		return (BasicGate)parent;
	}

	public StateType getState() {
		if(invert_signal)
			return (state==StateType.reset)?StateType.set:(state==StateType.set)?StateType.reset:state;
		return state;
	}

	public StateType getWireState() {
		if(io_type==IOType.output)
			return getState();
		else
			return state;
	}

	public void setState(StateType state) {
		next_state = state;
		if(io_type==IOType.input)
			this.state = next_state;
	}

	public String getSrc() {
		return getSrcFunction("ONE");
	}

	public void setSrc(String s) {
		// do nothing
	}

	public String getSrcFunction(String self) {
		String s = "";
		if(srcContact==null)
			s = "ZERO"; //self;
		else {
			if(srcContact.getParentGate()==this.getParentGate()) {
				s = self;
				s = (srcContact.getInvertSignal())?"not "+s:s;
			}
			else {
				s = srcContact.getParentGate().getId();
				
				if(srcContact.getParentGate().out.size() > 1)
				{
					int k = 0;
					for(k = 0; k < srcContact.getParentGate().out.size(); k++) if (srcContact == srcContact.getParentGate().out.get(k)) break;
					s += "_" + (char)('A' + k);
				}				
				
				s = (srcContact.getInvertSignal())?("s("+s+")"):("s("+s+")");				
				//s = (srcContact.getInvertSignal())?("r("+s+")"):("s("+s+")");
			}
		}
		if(invert_signal)
			s = "not "+s;
		return s;
	}

	public void reset() {
		state = StateType.reset;
		next_state = StateType.reset;
	}

	public void fire() {
		state = next_state;
	}

	public boolean isExcited() {
		return state!=next_state;
	}
}
