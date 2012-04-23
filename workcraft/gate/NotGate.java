package workcraft.gate;

import java.util.UUID;

import workcraft.Model;
import workcraft.UnsupportedComponentException;
import workcraft.editor.BasicEditable;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.shapes.CompoundPath;
import workcraft.visual.shapes.Shape;
import workcraft.visual.shapes.Vertex;

public class NotGate extends BasicGate {
	public static final UUID _modeluuid = UUID.fromString("6f704a28-e691-11db-8314-0800200c9a66");
	public static final String _displayname = "Buffer";
	
	private Shape shape = null;
	
	public NotGate(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		min_inputs = 1;
		max_outputs = 1;
		min_outputs = 1;
		max_outputs = 1;
		initContacts();
		
	}
	
	private Shape createGateShape() {
		CompoundPath p = new CompoundPath();
		p.addElement(new Vertex(-rad, -rad));
		p.addElement(new Vertex(rad, 0.0f));
		p.addElement(new Vertex(-rad, rad));
		p.setClosed(true);
		return new Shape(p);
	}
	
	public void doDraw(Painter p) {
		super.doDraw(p);
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
		p.setLineMode(LineMode.SOLID);
		p.setLineWidth(0.0075f);
		
		p.setLineColor((selected)?selectedOutlineColor:(canWork)?userOutlineColor:outlineColor);
		p.setFillColor((selected)?selectedFillColor:(canFire)?enabledFillColor:(lostSignal)?lostSignalFillColor:fillColor);
		p.drawShape(shape);
		
		// draw invertion circles
		in.getFirst().doDrawInvert(p);
		out.getFirst().doDrawInvert(p);
	}
	
	protected void updateContactOffsets() {
		super.updateContactOffsets();
		shape = createGateShape();
		in.getFirst().offsInvX = rad;
		out.getFirst().offsInvX = -rad;
	}
	
	public void refresh() {
		GateContact.StateType res = in.getFirst().getState();
		out.getFirst().setState(res);
		super.refresh();
	}
	
	public Boolean isSet() {
		return (out.getFirst().getState()==GateContact.StateType.set);
	}
	
	public String getSetFunction() {
		String s = in.getFirst().getSrcFunction("ZERO"); 
		if (out.getFirst().getInvertSignal())
			s = "not ("+s+")";
		return s;
	}

	public String getResetFunction() {
		String s = "not "+in.getFirst().getSrcFunction("ONE");
		if (out.getFirst().getInvertSignal())
			s = "not ("+s+")";
		return s;
	}
}
