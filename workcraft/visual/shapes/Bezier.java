package workcraft.visual.shapes;

import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import workcraft.InvalidConnectionException;
import workcraft.XmlSerializable;
import workcraft.util.Vec2;

public class Bezier implements XmlSerializable, PathElement {
	static final String _xmlelementtag = "bezier2d";
	
	private Vec2 cp2;
	private Vec2 cp1;
	private Vec2 p2;
	private Vec2 p1;
	private float ax, bx, cx, ay, by, cy;
	private boolean coeff_valid;
	private int segments = 50;
	
	public Bezier() {
		p1 = new Vec2(0.0f, 0.0f);
		p2 = new Vec2(0.0f, 0.0f);
		cp1 = new Vec2(0.0f, 1.0f);
		cp2 = new Vec2(1.0f, 0.0f);
		updateCoefficients();
	}
	
	public Bezier (Vec2 p1, Vec2 cp1, Vec2 cp2, Vec2 p2) {
		this.p1 = new Vec2(p1);
		this.cp1 = new Vec2(cp1);
		this.cp2 = new Vec2(cp2);
		this.p2 = new Vec2(p2);
		updateCoefficients();
	}
	
	private void updateCoefficients() {
		cx = 3.0f * (cp1.getX() - p1.getX());
		bx = 3.0f * (cp2.getX() - cp1.getX()) - cx;
		ax = p2.getX() - p1.getX() - cx - bx;

		cy = 3.0f * (cp1.getY() - p1.getY());
		by = 3.0f * (cp2.getY() - cp1.getY()) - cy;
		ay = p2.getY() - p1.getY() - cy - by;

		coeff_valid = true;
	}
	
	float approxLength(float t0, float t1) {
		if (!coeff_valid)
			updateCoefficients();
		
		float l = 0.0f;
		float t = t0;
		float p1x = p1.getX();
		float p1y = p1.getY();		
		float delta = (t1-t0)/(float)segments;
		
		float px = 0.0f, py = 0.0f;
		
		boolean first = true;
				
		for (int i=0; i<=segments; i++) {
			float tSquared = t * t;
			float tCubed = tSquared * t;
			
			float x = (ax * tCubed) + (bx * tSquared) + (cx * t) + p1x;
			float y = (ay * tCubed) + (by * tSquared) + (cy * t) + p1y;
			
			if (first)
				first = false;
			else
				l += Math.sqrt( (x-px)*(x-px)+(y-py)*(y-py));
			
			px = x;
			py = y;
			
			t += delta;
		}
		
		return l;
	}

	public Vec2 Point(float t) {
		if (!coeff_valid)
			updateCoefficients();
		float   tSquared, tCubed;
		tSquared = t * t;
		tCubed = tSquared * t;

		return new Vec2((ax * tCubed) + (bx * tSquared) + (cx * t) + p1.getX(), (ay * tCubed) + (by * tSquared) + (cy * t) + p1.getY());		
	}

	public float[] getPoints(int segments) {
		if (!coeff_valid)
			updateCoefficients();
		
		float res[] = new float[segments*2+2]; 		
		float delta = 1.0f / segments;
		
		float t = 0.0f;
		float p1x = p1.getX();
		float p1y = p1.getY();
		
		int ptr = 0;
		
		for (int i=0; i<=segments; i++) {
			float tSquared = t * t;
			float tCubed = tSquared * t;
			
			float x = (ax * tCubed) + (bx * tSquared) + (cx * t) + p1x;
			float y = (ay * tCubed) + (by * tSquared) + (cy * t) + p1y;
			
			res[ptr++] = x;
			res[ptr++] = y;
			
			t += delta;
		}
		
		return res;
	}
	
	public Vec2[] getVertices(int segments) {
		if (!coeff_valid)
			updateCoefficients();
		
		Vec2 res[] = new Vec2[segments+1]; 		
		float delta = 1.0f / segments;
		
		float t = 0.0f;
		float p1x = p1.getX();
		float p1y = p1.getY();
		
		int ptr = 0;
		
		for (int i=0; i<=segments; i++) {
			float tSquared = t * t;
			float tCubed = tSquared * t;
			
			float x = (ax * tCubed) + (bx * tSquared) + (cx * t) + p1x;
			float y = (ay * tCubed) + (by * tSquared) + (cy * t) + p1y;
			
			res[ptr++] = new Vec2(x, y);
			
			t += delta;
		}
		
		return res;
		
	}

	public void getVertices(Vec2[] out, int offset) {
		if (!coeff_valid)
			updateCoefficients();
		
		float delta = 1.0f / segments;
		
		float t = 0.0f;
		float p1x = p1.getX();
		float p1y = p1.getY();
		
		int ptr = offset;
		
		for (int i=0; i<=segments; i++) {
			float tSquared = t * t;
			float tCubed = tSquared * t;
			
			float x = (ax * tCubed) + (bx * tSquared) + (cx * t) + p1x;
			float y = (ay * tCubed) + (by * tSquared) + (cy * t) + p1y;
			
			out[ptr++] = new Vec2(x, y);
			
			t += delta;
		}
	}
	
	public void setP1(Vec2 p1) {
		this.p1.setXY(p1.getX(), p1.getY());
		coeff_valid = false;
	}
	
	public Vec2 getP1() {
		return new Vec2(p1);
	}

	public void setP2(Vec2 p2) {
		this.p2.setXY(p2.getX(), p2.getY());
		coeff_valid = false;
	}

	public Vec2 getP2() {
		return new Vec2(p2);
	}

	public void setCp1(Vec2 cp1) {
		this.cp1.setXY(cp1.getX(), cp1.getY());
		coeff_valid = false;
	}

	public Vec2 getCp1() {
		return new Vec2(cp1);
	}

	public void setCp2(Vec2 cp2) {
		this.cp2.setXY(cp2.getX(), cp2.getY());
		coeff_valid = false;
	}

	public Vec2 getCp2() {
		return new Vec2(cp2);
	}
	
	public void setControlPoints(Vec2 p1, Vec2 cp1, Vec2 cp2, Vec2 p2) {
		this.p1.setXY(p1.getX(), p1.getY());
		this.p2.setXY(p2.getX(), p2.getY());
		this.cp1.setXY(cp1.getX(), cp1.getY());
		this.cp2.setXY(cp2.getX(), cp2.getY());
		coeff_valid = false;
	}

	public void fromXmlDom(Element e) {
		NodeList ch = e.getElementsByTagName(Vec2._xmlelementtag);
		if (ch!=null && ch.getLength()>=4) {
			p1.fromXmlDom((Element)ch.item(0));	
			cp1.fromXmlDom((Element)ch.item(1));	
			cp2.fromXmlDom((Element)ch.item(2));	
			p2.fromXmlDom((Element)ch.item(3));	
		}
		coeff_valid = false;
	}

	public Element toXmlDom(Element parent_element) {
		Document d = parent_element.getOwnerDocument();
		Element e = d.createElement(_xmlelementtag);
		p1.toXmlDom(e);
		cp1.toXmlDom(e);
		cp2.toXmlDom(e);
		p2.toXmlDom(e);
		parent_element.appendChild(e);
		return e;
	}

	public int getNumberOfVertices() {
		return segments+1;
	}

	public void setSegments(int segments) {
		this.segments = segments;
	}

	public int getSegments() {
		return segments;
	}

	public boolean isClosed() {
		return false;
	}
}