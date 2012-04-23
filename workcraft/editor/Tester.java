package workcraft.editor;


import workcraft.util.Colorf;
import workcraft.util.Vec2;
import workcraft.visual.Drawable;
import workcraft.visual.GeometryUtil;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.PrimitiveType;
import workcraft.visual.ShapeMode;
import workcraft.visual.VertexBuffer;
import workcraft.visual.VertexFormat;
import workcraft.visual.VertexFormatException;
import workcraft.visual.shapes.Bezier;
import workcraft.visual.shapes.CompoundPath;
import workcraft.visual.shapes.Shape;
import workcraft.visual.shapes.Vertex;

public class Tester implements Drawable {
	private static Colorf color = new Colorf (0.0f, 0.0f, 1.0f, 1.0f);
	private static Colorf color2 = new Colorf (0.7f, 0.7f, 0.7f, 1.0f);
	private static Shape shape; 

	public Tester() {
		if (shape == null) {
			CompoundPath p = new CompoundPath();
			
			p.addElement(new Vertex(0.0f, 0.0f));
			p.addElement(new Vertex(0.25f, 0.0f));
			p.addElement(new Vertex(0.5f, 0.5f));
			p.addElement(new Vertex(0.25f, 1.0f));
			p.addElement(new Vertex(0.0f, 1.0f));
			p.addElement(new Vertex(0.25f, 0.75f));
			p.addElement(new Vertex(0.25f, 0.25f));
			
			p.setClosed(true);
			shape = new Shape(p);
		}
	}

	public void draw(Painter p) {
		p.setFillColor(color2);
		p.setLineColor(color);
		p.setLineMode(LineMode.SOLID);
		p.setLineWidth(0.01f);
		p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
		p.drawShape(shape);
		p.drawRect(-2, 1, -1, -1);
		p.drawCircle(0.5f, new Vec2(1.0f, 1.0f));
	}
}
