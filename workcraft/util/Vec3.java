package workcraft.util;
import java.lang.Cloneable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import workcraft.XmlSerializable;

public class Vec3 implements Cloneable, XmlSerializable {
	public static final String _xmlelementtag = "vec3";
	private float x, y, z;
	private boolean length_dirty;
	private float length_cache; 
	
	public Vec3() {
		x = y = z = length_cache = 0.0f;
		length_dirty = false;
	}
	
	public Vec3(float x, float y, float z) {
		this.x = x; this.y = y;
		this.z = z;
		recalcLength();
	}
	
	public Vec3 (Vec3 other) {
		this.x = other.getX();
		this.y = other.getY();
		this.z = other.getZ();
		length_cache = other.length();
	}
	
	public Object clone() {
		return new Vec3(this);
	}
	
	private void recalcLength() {
		length_cache = (float)Math.sqrt(x*x+y*y+z*z);
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
	
	
	public float getY() {
		return y;
	}
	
	public void setZ(float z) {
		this.z = z;
		length_dirty = true;
	}
	
	public float getZ() {
		return z;
	}
	
	public void setXYZ(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		length_dirty = true;
	}
	
	public Vec3 add(Vec3 other) {
		this.x += other.getX();
		this.y += other.getY();
		this.z += other.getZ();
		length_dirty = true;
		return this;
	}
	
	public Vec3 sub(Vec3 other) {
		this.x -= other.getX();
		this.y -= other.getY();
		this.z -= other.getZ();
		length_dirty = true;
		return this;
	}
	
	public float dot(Vec3 other) {
		return this.x * other.getX() + this.y * other.getY() + this.y * other.getZ();
	}
	
	void normalize() {
		x /= length_cache;
		y /= length_cache;
		z /= length_cache;
		length_cache = 1.0f;
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
		data = element.getAttribute("z");
		if (data!=null) {
			setZ(Float.parseFloat(data));								
		}
	}
	
	public Element toXmlDom(Element parent_element) {
		Document d = parent_element.getOwnerDocument();
		Element e = d.createElement("vec3");
		e.setAttribute("x", Float.toString(x));
		e.setAttribute("y", Float.toString(y));
		e.setAttribute("z", Float.toString(z));
		parent_element.appendChild(e);
		return e;
	}
}