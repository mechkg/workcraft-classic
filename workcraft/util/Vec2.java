package workcraft.util;
import java.lang.Cloneable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import workcraft.XmlSerializable;
import workcraft.editor.BasicEditable;

public class Vec2 implements Cloneable, XmlSerializable  {
	public static final String _xmlelementtag = "vec2";
	
	private float x, y;
	private boolean length_dirty;
	private float length_cache;
	
	public Vec2() {
		x = y = length_cache = 0.0f;
		length_dirty = false;
	}
	
	public Vec2(float x, float y) {
		this.x = x;
		this.y = y;
		recalcLength();
	}
	
	public Vec2 (Vec2 other) {
		this.x = other.getX();
		this.y = other.getY();
		length_dirty = false;
		length_cache = other.length();
	}
	
	public void negate() {
		x = -x;
		y = -y;
	}
	
	public void div(float a) {
		x/=a;
		y/=a;
		length_dirty = true;
	}
	

	public void mul(float a) {
		x*=a;
		y*=a;
		length_dirty = true;
	}	
	
	public void copy(Vec2 other) {
		this.x = other.getX();
		this.y = other.getY();
		length_dirty = false;
		length_cache = other.length();
	}
	
	public Object clone() {
		return new Vec2(this);
	}
	
	private void recalcLength() {
		length_cache = (float)Math.sqrt(x*x+y*y);
	}
	
	public float length() {
		if (length_dirty)
		{
			recalcLength();
			length_dirty = false;
		}
		return length_cache;			
	}
	
	public void setX(float x) {
		this.x = x;
		length_dirty = true;
	}
	
	public float getX() {
		return x;
	}
	
	public void setY(float y) {
		this.y = y;
		length_dirty = true;
	}
	
	public void setXY(float x, float y) {
		this.x = x;
		this.y = y;
		length_dirty = true;
	}
	
	public float getY() {
		return y;
	}
	
	public Vec2 add(Vec2 other) {
		this.x += other.getX();
		this.y += other.getY();
		length_dirty = true;
		return this;
	}
	
	public Vec2 sub(Vec2 other) {
		this.x -= other.getX();
		this.y -= other.getY();
		length_dirty = true;
		return this;
	}
	
	public float dot(Vec2 other) {
		return this.x * other.getX() + this.y * other.getY();
	}
	
	public void normalize() {
		x /= length();
		y /= length();
		length_cache = 1.0f;
	}
	
	public String toString() {
		return String.format("{%f, %f}", x, y);
	}
	
	
	public void fromXmlDom(Element element) {
		String data = element.getAttribute("x");
		if (data!=null) {
			setX(Float.parseFloat(data));								
		}
		data = element.getAttribute("y");
		if (data!=null) {
			setY(Float.parseFloat(data));								
		}
	}
	
	public Element toXmlDom(Element parent_element) {
		Document d = parent_element.getOwnerDocument();
		Element e = d.createElement(_xmlelementtag);
		e.setAttribute("x", Float.toString(x));
		e.setAttribute("y", Float.toString(y));
		parent_element.appendChild(e);
		return e;
	}
}