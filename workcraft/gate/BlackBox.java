package workcraft.gate;

import java.util.UUID;

import workcraft.UnsupportedComponentException;
import workcraft.editor.BasicEditable;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;

public class BlackBox extends BasicGate {
	public static final UUID _modeluuid = UUID.fromString("6f704a28-e691-11db-8314-0800200c9a66");
	public static final String _displayname = "Black box";
	
	public BlackBox(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		initContacts();
	}
		
	public void doDraw(Painter p) {
		super.doDraw(p);
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
		p.setLineMode(LineMode.SOLID);
		p.setLineWidth(0.0075f);
		
		p.setLineColor((selected)?selectedOutlineColor:(canWork)?userOutlineColor:outlineColor);
		p.setFillColor((selected)?selectedFillColor:(canFire)?enabledFillColor:(lostSignal)?lostSignalFillColor:fillColor);
		p.drawRect(-rad*0.8f, rad, rad*0.8f, -rad);

		// draw invertion circles
		for(GateContact c : in) {
			c.doDrawInvert(p);
		}
		for(GateContact c : out) {
			c.doDrawInvert(p);
		}
	}
	
	public void setFixedContacts(int ins, int outs) throws UnsupportedComponentException {
		min_inputs = ins;
		max_inputs = ins;
		min_outputs = outs;
		max_outputs = outs;
		initContacts();
	}

	protected void updateContactOffsets() {
		super.updateContactOffsets();
		for(GateContact c : in) {
			c.offsInvX = rad*0.8f;
		}
		for(GateContact c : out) {
			c.offsInvX = -rad*0.8f;
		}
	}
}
