package workcraft.visual.shapes;

import java.util.LinkedList;

import workcraft.util.Vec2;
import workcraft.visual.Drawable;
import workcraft.visual.GeometryUtil;
import workcraft.visual.Painter;
import workcraft.visual.PrimitiveType;
import workcraft.visual.VertexBuffer;
import workcraft.visual.VertexFormat;
import workcraft.visual.VertexFormatException;

public class CompoundPath implements PathElement{
	protected LinkedList<PathElement> elements = new LinkedList<PathElement>();
	private boolean closed = false;
	
	public void clear() {
		elements.clear();
	}
	public void addElement(PathElement e) {
		elements.add(e);
	}
	public void removeElement(PathElement e) {
		elements.remove(e);
	}
	
	public LinkedList<PathElement> getElements() {
		return elements;
	}
	
	public int getNumberOfVertices() {
		int v = 0;
		for (PathElement e : elements) {
			v += e.getNumberOfVertices();
		}
		return v;
	}
	public void getVertices(Vec2[] out, int offset) {
		for (PathElement e : elements) {
			e.getVertices(out, offset);
			offset += e.getNumberOfVertices();
		}
	}
	public void setClosed(boolean closed) {
			this.closed = closed;
	}
	public boolean isClosed() {
		return closed;
	}
}