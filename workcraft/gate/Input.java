package workcraft.gate;

import java.awt.event.MouseEvent;
import java.util.List;
import java.util.UUID;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.UnsupportedComponentException;
import workcraft.common.DefaultConnection;
import workcraft.editor.BasicEditable;
import workcraft.util.Colorf;
import workcraft.util.Vec2;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.shapes.CompoundPath;
import workcraft.visual.shapes.Shape;
import workcraft.visual.shapes.Vertex;

public class Input extends BasicGate {
	public static final UUID _modeluuid = UUID.fromString("6f704a28-e691-11db-8314-0800200c9a66");
	public static final String _displayname = "Input";
	
	private static Colorf setFillColor = new Colorf(0.0f, 1.0f, 0.0f, 1.0f);
	
	private GateContact.StateType state = GateContact.StateType.reset;

	private static Shape shape = null;

	private static Shape createGateShape() {
		CompoundPath p = new CompoundPath();
		p.addElement(new Vertex(-0.02f, -0.015f));
		p.addElement(new Vertex(0.02f, -0.015f));
		p.addElement(new Vertex(0.04f, 0.0f));
		p.addElement(new Vertex(0.02f, 0.015f));
		p.addElement(new Vertex(-0.02f, 0.015f));
		p.setClosed(true);
		return new Shape(p);
	}

	public Input(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		rad = 0.015f;
		min_inputs = 0;
		max_inputs = 0;
		min_outputs = 1;
		max_outputs = 1;
		initContacts();
	}
	
	private void doAutoRotate() {
		// set rotation depending on outgoing connection
		DefaultConnection con = null;
		if (out.size()>0&&out.getFirst().connections.size()>0)
			con = (DefaultConnection)(out.getFirst().connections.getFirst());
		
		// determine the connection end
		if (con!=null) {
			
			Vec2 ov = con.getOutgoingVector();
			
			float xx = ov.getX();
			float yy = ov.getY();
			
			if (xx>yy) {
				if (xx>-yy)
					setRotate(0);
				else
					setRotate(3);
			} else {
				if (xx>-yy)
					setRotate(1);
				else
					setRotate(2);
			}
		}
	}
	
	public void doDraw(Painter p) {
		super.doDraw(p);
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
		p.setLineMode(LineMode.SOLID);
		p.setLineWidth(0.005f);
		p.setLineColor((selected)?selectedOutlineColor:outlineColor);
		p.setFillColor((state==GateContact.StateType.set)?setFillColor:(selected)?selectedFillColor:fillColor);
		p.drawShape(shape);
	}
	
	public void acceptTransform() {
		super.acceptTransform();
		doAutoRotate();
	}

	protected void updateContactOffsets() {
		if(shape==null)
			shape = createGateShape();
		boundingBox.setExtents(new Vec2(-0.02f, -0.015f), new Vec2(0.04f, 0.015f));
		for(GateContact c : out) {
			Vec2 offs = new Vec2(0.0f, 0.0f);
			c.setOffs(offs);
		}
	}
	
	@Override
	public BasicEditable getChildAt(Vec2 point) {
		return null;
	}

	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		list.add("enum,Signal,getState,setState,reset,set");
		return list;
	}
	
	public Integer getState() {
		return state.ordinal();
	}
	
	public void setState(Integer state) {
		this.state = GateContact.StateType.values()[state];
	}
	
	public void refresh() {
		out.getFirst().setState(state);
		super.refresh();
		fire();
	}
	
	public void switchState() {
		state = (state==GateContact.StateType.reset)?
				GateContact.StateType.set:GateContact.StateType.reset;
	}
	
	public void simAction(int flag) {
		if (flag == MouseEvent.BUTTON1) {
			switchState();
			((GateModel)getOwnerDocument()).addTraceEvent(getId());
		}
	}
	
	public Boolean isSet() {
		return (out.getFirst().getState()==GateContact.StateType.set);
	}
	
	public String getSetFunction() {
		return "ALWAYS";
	}

	public String getResetFunction() {
		return "ALWAYS";
	}
	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("input");
		if(nl!=null) {
			Element ne = (Element) nl.item(0);
			if(ne!=null) {
				setState(new Integer(ne.getAttribute("signal")));
			}
		}
		super.fromXmlDom(element);
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		org.w3c.dom.Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("input");
		ppe.setAttribute("signal", getState().toString());
		ee.appendChild(ppe);
		return ee;
	}
}
