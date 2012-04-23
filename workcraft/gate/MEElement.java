package workcraft.gate;

import java.util.UUID;

import workcraft.UnsupportedComponentException;
import workcraft.editor.BasicEditable;
import workcraft.util.Vec2;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.TextAlign;

public class MEElement extends BasicGate {
	public static final UUID _modeluuid = UUID.fromString("6f704a28-e691-11db-8314-0800200c9a66");
	public static final String _displayname = "ME-element";
	
	private static final float width = 1.0f;
	private static final float height = 1.2f;
	
	public MEElement(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		min_inputs = 2;
		max_inputs = 2;
		min_outputs = 2;
		max_outputs = 2;

		initContacts();
		boundingBox.setExtents(new Vec2(-rad * width, -rad * height), new Vec2(rad * width, rad * height));
	}
		
	public void doDraw(Painter p) {
		super.doDraw(p);
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
		p.setLineMode(LineMode.SOLID);
		p.setLineWidth(0.0075f);
		
		p.setLineColor((selected)?selectedOutlineColor:(canWork)?userOutlineColor:outlineColor);
		p.setFillColor((selected)?selectedFillColor:(canFire)?enabledFillColor:(lostSignal)?lostSignalFillColor:fillColor);
		p.drawRect(-rad * width, rad * height, rad * width, -rad * height);

		Vec2 center = new Vec2(0.0f, -0.02f);
		transform.getLocalToViewMatrix().transform(center);
		p.drawString("ME", center, 0.065f, TextAlign.CENTER);		
		
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
			c.offsInvX = rad * width;
		}
		for(GateContact c : out) {
			c.offsInvX = -rad * width;
		}
		boundingBox.setExtents(new Vec2(-rad * width, -rad * height), new Vec2(rad * width, rad * height));
	}
	
	public void refresh()
	{
		// implements biased ME-element behaviour: in case of two requests the first request is granted

		int state = 0;
		
		if (out.getFirst().getState() == GateContact.StateType.set) state = 1;
		if (out.getLast().getState() == GateContact.StateType.set) state = 2;

		if (state == 0)
		{
			if (in.getFirst().getState() == GateContact.StateType.set)
			{
				out.getFirst().setState(GateContact.StateType.set);
			}
			else
			if (in.getLast().getState() == GateContact.StateType.set)
			{
				out.getLast().setState(GateContact.StateType.set);
			}
		}
		
		if (state == 1)
		{
			if (in.getFirst().getState() == GateContact.StateType.reset)
			{
				out.getFirst().setState(GateContact.StateType.reset);
			}
		}
		
		if (state == 2)
		{
			if (in.getLast().getState() == GateContact.StateType.reset)
			{
				out.getLast().setState(GateContact.StateType.reset);
			}
		}
		
		super.refresh();
	}
	
	public Boolean isSetA()
	{
		return false;
	}
	
	public Boolean isSetB()
	{
		return false;
	}
	
	public String getSetFunctionA()
	{
		String s = "";
		
		s += in.getFirst().getSrcFunction("ZERO");
		
		s += " & ";
		
		s += this.getId() + "_B_sig0";

		return s;
	}
	
	public String getSetFunctionB()
	{
		String s = "";
		
		s += in.getLast().getSrcFunction("ZERO");
		
		s += " & ";
		
		s += this.getId() + "_A_sig0";

		return s;
	}

	public String getResetFunctionA()
	{		
		String s = "";
		
		s += "not (" + in.getFirst().getSrcFunction("ZERO") + ")";
		
		return s;
	}	
	
	public String getResetFunctionB()
	{		
		String s = "";
		
		s += "not (" + in.getLast().getSrcFunction("ZERO") + ")";
		
		return s;
	}	
}
