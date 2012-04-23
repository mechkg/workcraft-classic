package workcraft.visual.shapes;

import workcraft.util.Vec2;

public class Vertex implements PathElement {
	private Vec2 v = new Vec2();
	
	public Vertex(float x, float y) {
		v.setXY(x, y);
	}

	public int getNumberOfVertices() {
		return 1;
	}

	public void getVertices(Vec2[] out, int offset) {
		out[offset] = new Vec2(v);
	}

	public boolean isClosed() {
		return false;
	}
}
