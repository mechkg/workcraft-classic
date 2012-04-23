package workcraft.gate;

import java.util.List;
import java.util.UUID;

import workcraft.UnsupportedComponentException;
import workcraft.editor.BasicEditable;
import workcraft.util.Colorf;
import workcraft.util.Vec2;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;

public class Joint extends BasicGate {
	public static final UUID _modeluuid = UUID.fromString("6f704a28-e691-11db-8314-0800200c9a66");
	public static final String _displayname = "Joint";
	
	private static Colorf jointColor = new Colorf(0.0f, 0.0f, 0.0f, 0.0f);
	private static Colorf selectedFillColor = new Colorf(0.5f, 0.0f, 0.0f, 1.0f);

	public GateContact srcContact = null;

	private GateContact.StateType out_state = GateContact.StateType.reset;

	public Joint(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		rad = 0.01f;
		initContacts();
	}
	
	public void doDraw(Painter p) {
		for(GateContact c : in) {
			c.acceptTransform();
		}
		for(GateContact c : out) {
			c.acceptTransform();
		}
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL);
		if (selected)
			p.setFillColor(selectedFillColor);
		else
			p.setFillColor(jointColor);
		p.drawCircle(rad, null);
	}
	
	protected void updateContactOffsets() {
		boundingBox.setExtents(new Vec2(-0.02f, -0.02f), new Vec2(0.02f, 0.02f));
		for(GateContact c : in) {
			Vec2 offs = new Vec2(0.0f, 0.0f);
			c.setOffs(offs);
		}
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
		list.add("str,(Contacts),getConList,setConList");
		return list;
	}
	
	public String getConList() {
		String res = "";
		for(GateContact con : in)
			res += con.getId()+",";
		for(GateContact con : out)
			res += con.getId()+",";
		return res;
	}
	
	public void setConList(String str) {
		// do nothing
	}
	
	public void reset() {
		for(GateContact con : out)
			con.reset();
		super.reset();
		out_state = GateContact.StateType.reset;
	}
	
	public void setOutState(GateContact.StateType state) {
		if(state==GateContact.StateType.set)
			out_state = GateContact.StateType.set;
	}
	
	public boolean refreshJoint() {
		GateContact.StateType res = out_state;
		for(GateContact cin : in) {
			if(cin.getState()==GateContact.StateType.set) {
				res = GateContact.StateType.set;
				break;
			}
		}
		boolean fn_res = false;
		for(GateContact cout : in) {
			fn_res |= (cout.getState()!=res);
			cout.setState(res);
			cout.fire();
		}
		for(GateContact cout : out) {
			fn_res |= (cout.getState()!=res);
			cout.setState(res);
			cout.fire();
		}
		out_state = GateContact.StateType.reset;
		return fn_res;
	}
}
