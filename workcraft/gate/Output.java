package workcraft.gate;

import java.util.List;
import java.util.UUID;

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

public class Output extends BasicGate {
	public static final UUID _modeluuid = UUID.fromString("6f704a28-e691-11db-8314-0800200c9a66");
	public static final String _displayname = "Output";
	
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

	public Output(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		rad = 0.015f;
		min_inputs = 1;
		max_inputs = 1;
		min_outputs = 0;
		max_outputs = 0;
		initContacts();
	}
	

	public void acceptTransform() {
		super.acceptTransform();
		doAutoRotate();
	}

	private void doAutoRotate() {
		// set rotation depending on incoming connection
		DefaultConnection con = null;
		if (in.size()>0&&in.getFirst().connections.size()>0)
			con = (DefaultConnection)(in.getFirst().connections.getFirst());
		
		// determine the connection end
		if (con!=null) {
			
			Vec2 iv = con.getIncomingVector();
			
			float xx = iv.getX();
			float yy = iv.getY();
			
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
		state = in.getFirst().getState();
		super.refresh();
		fire();
	}
	
	public void simAction(int flag) {
		// do nothing
	}

	public Boolean isSet() {
		return (state==GateContact.StateType.set);
	}
	
	public String getSetFunction() {
		return in.getFirst().getSrcFunction("ONE");
	}

	public String getResetFunction() {
		String s = "not "+in.getFirst().getSrcFunction("ZERO");
		return s;
	}
}
