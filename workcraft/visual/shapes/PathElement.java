package workcraft.visual.shapes;

import workcraft.util.Vec2;

public interface PathElement {
	public int getNumberOfVertices();
	public boolean isClosed();
	public void getVertices (Vec2[] out, int offset);
}
