package workcraft.util;

import java.awt.Color;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import workcraft.XmlSerializable;

public class Colorf implements XmlSerializable {
	static final String _xmlelementtag = "colorf";
	
	public float a, r, g, b;
	
	public static  float clamp(float x) {
		if (x<0)
			return 0;
		if (x>1)
			return 1;
		return x;
	}
	
	public Colorf() {
		fromRGBA(1.0f, 1.0f, 1.0f, 1.0f);	
	}
	
	public Colorf (Colorf other) {
		copy(other);
	}
	
	public Colorf (Color c) {
		float comp[] = new float[4];
		c.getRGBComponents(comp);
		r = comp[0];
		g = comp[1];
		b= comp[2];
		a = comp[3];
	}
	
	public void copy(Colorf other) {
		a = other.a;
		r = other.r;
		g = other.g;
		b = other.b;
	}
		
	public Colorf(float r, float g, float b, float a) {
		fromRGBA(r,g,b,a);		
	}
	
	public static Colorf lerp(Colorf c1, Colorf c2, float a) {
		return new Colorf( c1.r * a + c2.r*(1.0f-a), c1.g * a + c2.g*(1.0f-a), c1.b * a + c2.b*(1.0f-a), c1.a * a + c2.a*(1.0f-a) );		
	}
	
	public void fromRGBA (byte r, byte g, byte b, byte a) {
		this.a = (float)a/255.0f; this.r = (float)r/255.0f;
		this.g = (float)g/255.0f; this.b = (float)b/255.0f;
	}
	
	public void fromRGBA (float r, float g, float b, float a) {
		this.a = a; this.r = r;
		this.g = g; this.b = b;
	}

	public void fromXmlDom(Element element) {
		float r,g,b,a;
		String data = element.getAttribute("r");
		if (data!=null) {
			r = Float.parseFloat(data);								
		}
		data = element.getAttribute("g");
		if (data!=null) {
			g = Float.parseFloat(data);								
		}
		data = element.getAttribute("b");
		if (data!=null) {
			b = Float.parseFloat(data);								
		}
		data = element.getAttribute("a");
		if (data!=null) {
			a = Float.parseFloat(data);								
		}
	}

	public Element toXmlDom(Element parent_element) {
		Document d = parent_element.getOwnerDocument();
		Element e = d.createElement(_xmlelementtag);
		e.setAttribute("r", Float.toString(r));
		e.setAttribute("g", Float.toString(g));
		e.setAttribute("b", Float.toString(b));
		e.setAttribute("a", Float.toString(a));
		parent_element.appendChild(e);
		return e;
	}
	
	public Color Color() {
		return new Color(r, g, b, a);		
	}
	
	public String toString() {
		return "Color{"+r+";"+g+";"+b+";"+a+"}";
	}
}