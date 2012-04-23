package workcraft.gate;

import java.util.UUID;

import workcraft.Model;
import workcraft.UnsupportedComponentException;
import workcraft.editor.BasicEditable;
import workcraft.util.Vec2;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.shapes.Bezier;
import workcraft.visual.shapes.CompoundPath;
import workcraft.visual.shapes.Shape;

public class XorGate extends BasicGate  {
	public static final UUID _modeluuid = UUID.fromString("6f704a28-e691-11db-8314-0800200c9a66");
	public static final String _displayname = "XOR gate";
	
	private Shape shape = null;
	private Bezier shape2 = null;

	public XorGate(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		min_inputs = 2;
		min_outputs = 1;
		max_outputs = 1;
		initContacts();
		
	}
	
	private Shape createGateShape() {
		CompoundPath p = new CompoundPath();
		p.addElement(new Bezier( new Vec2(-rad*0.65f, -rad), new Vec2(-rad*0.65f+(rad), -rad),
			new Vec2(rad-(rad)*0.25f, -(rad)*0.5f), new Vec2(rad, 0.0f) ));
		p.addElement(new Bezier( new Vec2(rad, 0.0f), new Vec2(rad-(rad)*0.25f, (rad)*0.5f),
			new Vec2(-rad*0.65f+(rad), rad), new Vec2(-rad*0.65f, rad) ));
		p.addElement(new Bezier( new Vec2(-rad*0.65f, rad), new Vec2(-rad*0.65f+(rad)*0.5f, rad-(rad)*0.5f),
			new Vec2(-rad*0.65f+(rad)*0.5f, -rad+(rad)*0.5f), new Vec2(-rad*0.65f, -rad) ));
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
		
		p.drawBezierCurve(shape2);

		// draw invertion circles
		for(GateContact c : in) {
			c.offsInvX+=rad*0.35f;
			c.doDrawInvert(p);
			c.offsInvX-=rad*0.35f;
		}
		out.getFirst().doDrawInvert(p);
	}
	
	protected void updateContactOffsets() {
		super.updateContactOffsets();
		shape = createGateShape();
		shape2 = new Bezier( new Vec2(-rad, rad), new Vec2(-rad+(rad)*0.5f, rad-(rad)*0.5f),
				new Vec2(-rad+(rad)*0.5f, -rad+(rad)*0.5f), new Vec2(-rad, -rad) );
		Bezier leftside = new Bezier( new Vec2(-rad*0.65f, rad), new Vec2(-rad*0.65f+(rad)*0.5f, rad-(rad)*0.5f),
				new Vec2(-rad*0.65f+(rad)*0.5f, -rad+(rad)*0.5f), new Vec2(-rad*0.65f, -rad) );
		for(GateContact c : in) {
			Vec2 pt = leftside.Point((c.getOffs().getY()+rad)/2.0f/rad);
			c.offsInvX = -pt.getX();
		}
		out.getFirst().offsInvX = -rad;
	}
	
	public void refresh() {
		GateContact.StateType res = GateContact.StateType.reset;
		boolean f = true; 
		for(GateContact cin : in) {
			if(f) {
				res = cin.getState();
				f = false;
			}
			else if(cin.getState()==GateContact.StateType.set && res==GateContact.StateType.set) {
				res = GateContact.StateType.reset;
			}
			else if(cin.getState()==GateContact.StateType.set && res==GateContact.StateType.reset) {
				res = GateContact.StateType.set;
			}
		}
		out.getFirst().setState(res);
		super.refresh();
	}
	
	public Boolean isSet() {
		return (out.getFirst().getState()==GateContact.StateType.set);
	}
	
	private String generateXorFunction(String Op1, String Op2) {
		String Opb1 = "("+Op1+")";
		String Opb2 = "("+Op2+")";
		return "not "+Opb2+" & "+Opb1+" | not "+Opb1+" & "+Opb2;
	}
	
	public String getSetFunction() {
		boolean first = true;
		String s = "";
		for(GateContact c : in) {
			if(first)
				s = c.getSrcFunction("ZERO");
			else
				s = generateXorFunction(s, c.getSrcFunction("ZERO"));
			first = false;
		}
		
		if (out.getFirst().getInvertSignal())
			s = "not ("+s+")";
		
		return s;
	}

	public String getResetFunction() {
		boolean first = true;
		String s = "";
		for(GateContact c : in) {
			if(first)
				s = c.getSrcFunction("ONE");
			else
				s = generateXorFunction(s, c.getSrcFunction("ONE"));
			first = false;
		}
		
		if (out.getFirst().getInvertSignal())
			s = "not ("+s+")";
		
		return "not ("+s+")";
	}
}
