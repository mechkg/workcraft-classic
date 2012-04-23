package workcraft.editor;

import workcraft.util.Vec2;

public class BoundingBox {
	private Vec2 lower_left;
	private Vec2 upper_right;
	
	public BoundingBox() {
		lower_left = new Vec2();
		upper_right = new Vec2();
		lower_left.setXY(Float.MAX_VALUE, Float.MAX_VALUE);
		upper_right.setXY(-Float.MAX_VALUE, -Float.MAX_VALUE);
	}
	
	public BoundingBox(Vec2 extent1, Vec2 extent2) {
		this();
		setExtents(extent1, extent2);
	}
	
	public BoundingBox(Vec2 [] points) {
		this();
		buildFromPoints(points);
	}
	
	public BoundingBox(BoundingBox other) {
		this.lower_left  = other.lower_left;
		this.upper_right = other.upper_right; 
	}
	
	public BoundingBox setPointArea(Vec2 center, float dist) {
		lower_left.setXY(center.getX()-dist, center.getY()-dist);
		upper_right.setXY(center.getX()+dist, center.getY()+dist);
		return this;
	}

	public BoundingBox setExtents(Vec2 extent1, Vec2 extent2) {
		float x0, x1, y0, y1;

		if (extent1.getX() > extent2.getX()) {
			x0 = extent2.getX();
			x1 = extent1.getX();
		} else {
			x1 = extent2.getX();
			x0 = extent1.getX();
		}
		
		if (extent1.getY() > extent2.getY()) {
			y0 = extent2.getY();
			y1 = extent1.getY();
		} else {
			y1 = extent2.getY();
			y0 = extent1.getY();
		}
		
		lower_left.setXY(x0, y0);
		upper_right.setXY(x1, y1);
		
		return this;
	}
	
	public BoundingBox buildFromPoints (Vec2[] points) {
		float x0 =Float.MAX_VALUE,  x1= -Float.MAX_VALUE, y0 = Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
		
		for (Vec2 p : points) {
			float x = p.getX(), y = p.getY();
			if (x < x0)
				x0 = x;
			if (x > x1)
				x1 = x;
			if (y < y0)
				y0 = y;
			if (y > y1)
				y1 = y;
		}
		
		lower_left.setXY(x0, y0);
		upper_right.setXY(x1, y1);
		
		return this;
	}
	
	public Vec2 getLowerLeft() {
		return (Vec2)lower_left.clone();
	}

	public Vec2 getUpperRight() {
		return (Vec2)upper_right.clone();
	}
	
	public BoundingBox addPoint(Vec2 point) {
		if (point.getX() < lower_left.getX()) {
			lower_left.setX(point.getX());
		}

		if (point.getY() < lower_left.getY()) {
			lower_left.setY(point.getY());
		}
		
		if (point.getX() > upper_right.getX()) {
			upper_right.setX(point.getX());
		}

		if (point.getY() > upper_right.getY()) {
			upper_right.setY(point.getY());
		}
		
		return this;
	}
	
	public BoundingBox merge (BoundingBox other) {
		addPoint(other.getLowerLeft());
		addPoint(other.getUpperRight());
		return this;
	}
	
	public boolean isInside(Vec2 point) {
		if (
				point.getX() >= lower_left.getX() &&
				point.getY() >= lower_left.getY() &&
				point.getX() <= upper_right.getX() &&
				point.getY() <= upper_right.getY()
			)
			return true;
		else
			return false;
	}
	
	public boolean intersects(BoundingBox other) {
		Vec2 oll = other.getLowerLeft();
		Vec2 our = other.getUpperRight();
		if (
				upper_right.getX() < oll.getX() ||
				lower_left.getX() > our.getX() ||
				upper_right.getY() < oll.getY() ||
				lower_left.getY() > our.getY()
			)
			return false;
		else
			return true;
	}
	
	public void reset() {
		float x0 =Float.MAX_VALUE,  x1= Float.MIN_VALUE, y0 = Float.MAX_VALUE, y1 = Float.MIN_VALUE;
		lower_left.setXY(x0, y0);
		upper_right.setXY(x1, y1);	
	}
}