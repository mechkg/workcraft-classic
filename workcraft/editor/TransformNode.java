package workcraft.editor;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.InvalidConnectionException;
import workcraft.XmlSerializable;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.util.Vec3;

public class TransformNode implements XmlSerializable {
	private Mat4x4 matTrans, matScale, matRot, matFinal, matFinalInv, matNodeCache, matViewToLocal, matLocalToView;
	private boolean final_dirty, final_inverse_dirty, vtl_dirty, vtl_inv_dirty;
	
	private Vec3 _t;
	private Vec3 _s;

	private BasicEditable parent;

	public TransformNode(BasicEditable parent) {
		matScale = new Mat4x4();
		matTrans = new Mat4x4();
		matRot = new Mat4x4();
		matFinal = new Mat4x4();
		matFinalInv = new Mat4x4();
		matNodeCache = new Mat4x4();
		matViewToLocal = new Mat4x4();
		matLocalToView = new Mat4x4();
		
		_t = new Vec3(0.0f, 0.0f, 0.0f);
		_s = new Vec3(1.0f, 1.0f, 1.0f);

		final_dirty = false;
		final_inverse_dirty = false;
		vtl_dirty = false;
		vtl_inv_dirty = false;
		this.parent = parent;
	}
	
	public void invalidateMatrices() {
		final_dirty = true;
		final_inverse_dirty = true;
		vtl_dirty = true;
		vtl_inv_dirty = true;
		
		if (parent != null) {
			for (BasicEditable n : parent.getChildren()) {
				n.transform.invalidateMatrices();
			}
		}
	}
	
	public void updateTransformCache(Mat4x4 m) {
		matNodeCache.set(m.getArray());
		vtl_dirty = false;
		vtl_inv_dirty = false;
	}

	public void copy(TransformNode other) {
		Vec2 t = other.getTranslation2d();
		Vec3 s = other.getScale();
		
		translateAbs(t.getX(), t.getY(), 0.0f);
		scaleAbs(s.getX(), s.getY(), s.getZ());
	}

	public Mat4x4 getFinalMatrix() {
		if (final_dirty) {
			matFinal.setIdentity();
			matFinal.mul(matScale);
			matFinal.mul(matRot);
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
	
	public Mat4x4 getViewToLocalMatrix() {
		if (vtl_dirty) {
			matViewToLocal.setIdentity();
			matViewToLocal.mul(getFinalMatrix());
			if (parent != null)
			{
				if (parent.getParent() != null)
					matViewToLocal.mul(parent.getParent().transform.getViewToLocalMatrix());
			}
			vtl_dirty = false;
		}
		return matViewToLocal;
	}
	
	public Mat4x4 getLocalToViewMatrix() {
		if (vtl_inv_dirty) {
			matLocalToView.set(getViewToLocalMatrix().getArray());
			matLocalToView.invert();
			vtl_inv_dirty = false;
		}
		return matLocalToView;
	}
	
	public void scaleRel(float sx, float sy, float sz) {
		Mat4x4 m = new Mat4x4();
		m.setScale(sx, sy, sz);
		matScale.mul(m);
		_s.setXYZ(_s.getX()*sz, _s.getY()*sy, _s.getZ()*sz);
		invalidateMatrices();
	}

	public void scaleAbs(float sx, float sy, float sz) {
		matScale.setScale(sx, sy, sz);
		_s.setXYZ(sx, sy, sz);
		invalidateMatrices();
	}
	
	public Vec3 getTranslation() {
		return (Vec3)_t.clone();
	}
	
	public Vec3 getScale() {
		return (Vec3)_s.clone();
	}
	
	public Vec2 getTranslation2d() {
		return new Vec2(_t.getX(), _t.getY());
	}
	
	public void translateAbs(float tx, float ty, float tz) {
		//System.out.println(""+tx+" "+ty);
		//System.out.println ("matTrans before translateAbs: "+matTrans.toString());
		matTrans.setTranslate(tx, ty, tz);
		//System.out.println ("matTrans after translateAbs: "+matTrans.toString());		
		_t.setXYZ(tx, ty, tz);
		invalidateMatrices();
	}

	public void translateAbs(Vec3 v) {
		translateAbs(v.getX(), v.getY(), v.getZ());
	}

	public void translateAbs(Vec2 v) {
		translateAbs(v.getX(), v.getY(), 0.0f);
	}
	
	public void translateRel(float tx, float ty, float tz) {
		Vec3 v = new Vec3(tx, ty, tz);
		translateRel(v);
	}

	public void translateRel(Vec2 v) {
		Vec3 vv = new Vec3(v.getX(), v.getY(), 0.0f);
		translateRel(vv);
	}
	
	public void translateRel(Vec3 v) {
		_t.add(v);
		matTrans.setTranslate(_t.getX(), _t.getY(), _t.getZ());
		invalidateMatrices();
	}
	
	public void rotateZ(float angle) {
		matRot.setRotateZ(angle*(float)Math.PI/180.0f);
		// mmatTrans.mul(m);
		invalidateMatrices();
	}
	
	public Vec2 ViewToLocal(Vec2 point) {
		Vec2 v = new Vec2(point);
		 getViewToLocalMatrix().transform(v);
		 return v;
	}

	public Vec2 LocalToView(Vec2 point) {
		Vec2 v = new Vec2(point);
		 getLocalToViewMatrix().transform(v);
		 return v;
	}


	public void fromXmlDom(Element e) {
		NodeList nl = e.getElementsByTagName("vec3");
		_t.fromXmlDom((Element)nl.item(0));
		_s.fromXmlDom((Element)nl.item(1));
		
		scaleAbs(_s.getX(), _s.getY(), _s.getZ());
		translateAbs(_t.getX(), _t.getY(), _t.getZ());
	}

	public Element toXmlDom(Element e) {
		Document d = e.getOwnerDocument();
		Element te = d.createElement("transform");
		_t.toXmlDom(te);
		_s.toXmlDom(te);
		e.appendChild(te);
		return te;
	}

	public String getXmlElementName() {
		return "transform";
	}
}