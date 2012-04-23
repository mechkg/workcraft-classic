package workcraft.visual.shapes;

import workcraft.util.Vec2;

public class Circle implements PathElement {
	private int segments = 50;
	private Vec2 center = new Vec2();
	private float radius = 1.0f;

	public int getNumberOfVertices() {
		return segments;
	}

	public void getVertices(Vec2[] out, int offset) {
			float d = 2.0f * (float) Math.PI / segments;
			float a = d;
			
			out[offset++] = new Vec2(center.getX() + radius, center.getY());

			for (int i = 0; i < segments-1; i++) {
				out[offset++] = new Vec2(center.getX() + radius * (float) Math.cos(a), center.getY() + radius * (float) Math.sin(a));
				a += d;
			}
	}

	public boolean isClosed() {
		return true;
	}

	public void setSegments(int segments) {
		this.segments = segments;
		if (segments < 2)
			this.segments = 2;
		if (segments > 1000)
			this.segments = 1000;
	}

	public int getSegments() {
		return segments;
	}

	public void setCenter(Vec2 center) {
		this.center.copy(center);
	}

	public Vec2 getCenter() {
		return (Vec2)center.clone();
	}

	public void setRadius(float radius) {
		this.radius = radius;
		if (radius<0.0f)
			this.radius = 0.0f;
	}

	public float getRadius() {
		return radius;
	}
}
