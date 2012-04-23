package workcraft.visual.shapes;

import workcraft.editor.BoundingBox;
import workcraft.util.Vec2;
import workcraft.visual.Drawable;
import workcraft.visual.GeometryUtil;
import workcraft.visual.Painter;
import workcraft.visual.PrimitiveType;
import workcraft.visual.VertexBuffer;

public class Shape implements Drawable {
	protected BoundingBox boundingBox = new BoundingBox();
	protected PathElement path = null;
	
	public Shape(PathElement p) {
		this.path = p;
		updateBB();
	}

	private void updateBB() {
		boundingBox.reset();
		Vec2[] vertices = new Vec2[path.getNumberOfVertices()];
		path.getVertices(vertices, 0);
		for (Vec2 v: vertices)
			boundingBox.addPoint(v);
	}
	
	public PathElement getPath() {
		return path;
	}
	
	public void draw(Painter p) {
		p.drawShape(this);
	}
}