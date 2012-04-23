package workcraft.editor;

import workcraft.util.Colorf;
import workcraft.util.Vec2;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;

public class EditableAnchor extends BasicEditable {
	public static Colorf anchorColor = new Colorf (0.75f, 0.0f, 0.0f, 1.0f);
	public static Colorf anchorSelectedColor = new Colorf (0.0f, 0.75f, 0.0f, 1.0f);
	public enum Type { normal, horizontal, vertical };
	public Type anchorType = Type.normal;
	
	public int tagIndex = -1;
	
	public EditableAnchor(Vec2 center) {
		super();
		boundingBox.setExtents(new Vec2(-0.015f, -0.015f), new Vec2(0.015f, 0.015f));
		setCenter(center);
	}
	
	public void setCenter(Vec2 center) {
		Vec2 nc = new Vec2(center);
		nc.negate();
		transform.translateAbs(nc);
	}

	@Override
	public BasicEditable getChildAt(Vec2 point) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void doDraw(Painter p) {
		p.setShapeMode(ShapeMode.FILL);
		p.setFillColor((selected)?anchorSelectedColor:anchorColor);
		Vec2 tr = transform.getTranslation2d();
		tr.negate();
		p.drawRect(tr.getX()-0.01f, tr.getY()+0.01f, tr.getX()+0.01f, tr.getY()-0.01f);
	}

}
