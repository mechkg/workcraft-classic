package workcraft.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.XmlSerializable;
import workcraft.editor.BoundingBox;

public class ViewState implements XmlSerializable {
	
	private float aspect, t_x, t_y, t_z, s;
	private float scale_factor;
	private Mat4x4 matProj, matTrans, matScale, matFinal, matFinalInv;
	private boolean final_dirty;
	private boolean final_inverse_dirty;
	private int vp_x, vp_y, w_2, h_2;
	
	public ViewState() {
		aspect = 1.0f;
		scale_factor = 1.1f;
		t_x = t_y = t_z = 0.0f;
		s = 1.0f;
		
		matProj = new Mat4x4();
		//matProj.set(0, 0, -1.0f);
		//matProj.set(1, 1, -1.0f);
		matScale = new Mat4x4();
		matTrans = new Mat4x4();
		matFinal = new Mat4x4();
		matFinalInv = new Mat4x4();
		
		final_dirty = false;
		final_inverse_dirty = false;
	}
	
	public Mat4x4 getFinalMatrix() {
		if (final_dirty) {
			matFinal.setIdentity();
			matFinal.mul(matProj);
			matFinal.mul(matScale);
			matFinal.mul(matTrans);			
			final_dirty = false;
			final_inverse_dirty = true;
		}
		return matFinal;		
	}
	
	public Mat4x4 getFinalInvMatrix() {
		if (final_inverse_dirty) {
			matFinalInv.set(getFinalMatrix().getArray());
			matFinalInv.invert();
			final_inverse_dirty = false;
		}
		return matFinalInv;
	}
	
	public float getWindowScale() {
		return w_2*s;
	}
	public void scale(int steps) {
		s /= Math.pow(scale_factor, steps);
		if (s < 0.01f)
			s = 0.01f;
		if (s > 1000.0f)
			s = 1000.0f;
		matScale.setScale(s, s, s);
		final_dirty = true;
	}
	
	public void scale (int steps, int screen_x, int screen_y) {
		Vec2 x0 = WindowToView(screen_x, screen_y);
		scale(steps);
		getFinalMatrix();
		Vec2 x1 = WindowToView(screen_x, screen_y);
		x1.sub(x0);
		translate(x1.getX(), x1.getY());		
	}
	
	public Vec2 getVisibleLL() {
		return WindowToView(0, h_2*2-1);
	}
	
	public Vec2 getVisibleUR() {
		return WindowToView(w_2*2-1, 0);
	}
	
	public boolean cull (BoundingBox boundingBoxInViewSpace) {
		BoundingBox bb = new BoundingBox (WindowToView(0, h_2*2-1),WindowToView(w_2*2-1, 0));
		return !bb.intersects(boundingBoxInViewSpace);
	}
	
	public void viewport(int x, int y, int w, int h) {
		vp_x = x; vp_y = y;
		w_2 = w/2; h_2 = h/2;		
	}
	
	public void aspect(float a) {
		matProj.set(0, 0, 1.0f/a);
		final_dirty = true;
	}
	
	public Vec2 WindowToDevice (int window_x, int window_y) {
		return new Vec2 ( (float)(window_x-vp_x-w_2)/w_2, -(float)(window_y-vp_y-h_2)/h_2 );
	}
	
	public Vec2 DeviceToView(Vec2 device) {
		Vec2 r = new Vec2(device);
		getFinalInvMatrix().transform(r);
		return r;
	}
	
	public Vec2 ViewToDevice(Vec2 view) {
		Vec2 r = new Vec2(view);
		getFinalMatrix().transform(r);
		return r;
	}
	
	public Vec2 WindowToView (int window_x, int window_y) {
		return DeviceToView (WindowToDevice(window_x, window_y));
	}
	
	public Point DeviceToWindow (Vec2 device) {
		return new Point( (int)(w_2*device.getX() + vp_x + w_2), (int)(h_2*device.getY() + vp_y + h_2));
	}
	
	/**
	 * Recalculates translation matrix so that the view is translated by specified window-space values  
	 * @param screen_x
	 * @param screen_y
	 */
	public void translate (Point p1, Point p2) {
		// Convert translation into view space
		Vec2 v1 = WindowToView(p1.x, p1.y);
		Vec2 v2 = WindowToView(p2.x, p2.y);
		v2.sub(v1);
		translate (v2.getX(), v2.getY());
	}
	
	public void translate (float tx, float ty) {
		t_x += tx;
		t_y += ty;
		matTrans.setTranslate(t_x, t_y, 0.0f);
		final_dirty = true;
	}

	public void reset() {
		t_x = t_y = t_z = 0;
		s = 1.0f;
		matTrans = new Mat4x4();
		matScale = new Mat4x4();
		final_dirty = true;
		final_inverse_dirty = true;
	}
	
	public void fromXmlDom(Element element) {
		NodeList nl = element.getElementsByTagName("mat4x4");
		s = Float.parseFloat(element.getAttribute("s"));
		t_x = Float.parseFloat(element.getAttribute("tx"));
		t_y = Float.parseFloat(element.getAttribute("ty"));
		matScale.setScale(s, s, s);
		matTrans.setTranslate(t_x, t_y, 0);
		final_dirty = true;
		final_inverse_dirty = true;
	}

	public Element toXmlDom(Element parent_element) {
		Document d = parent_element.getOwnerDocument();
		Element ve = d.createElement("viewstate");
		ve.setAttribute("s", Float.toString(s));
		ve.setAttribute("tx", Float.toString(t_x));
		ve.setAttribute("ty", Float.toString(t_y));
		parent_element.appendChild(ve);
		return ve;
	}
}
