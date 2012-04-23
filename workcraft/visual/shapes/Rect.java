package workcraft.visual.shapes;
import workcraft.util.Vec2;

public class Rect implements PathElement {
	private Vec2 center = new Vec2();
	private float width = 1.0f;
	private float height = 1.0f;
	
	public int getNumberOfVertices() {
		return 4;
	}

	public void getVertices(Vec2[] out, int offset) {
		out[offset+0] = new Vec2(center.getX()-width*0.5f, center.getY()-height*0.5f);
		out[offset+1] = new Vec2(center.getX()+width*0.5f, center.getY()-height*0.5f);
		out[offset+2] = new Vec2(center.getX()+width*0.5f, center.getY()+height*0.5f);
		out[offset+3] = new Vec2(center.getX()-width*0.5f, center.getY()+height*0.5f);		
	}

	public boolean isClosed() {
		return true;
	}

	public void setWidth(float width) {
		this.width = width;
		if (this.width <= 0.0f)
			this.width = Float.MIN_VALUE;
	}

	public float getWidth() {
		return width;
	}

	public void setCenter(Vec2 center) {
		this.center.copy(center);
	}

	public Vec2 getCenter() {
		return new Vec2(center);
	}

	public void setHeight(float height) {
		this.height = height;
		if (this.height <= 0.0f)
			this.height = Float.MIN_VALUE;
	}

	public float getHeight() {
		return height;
	}
}