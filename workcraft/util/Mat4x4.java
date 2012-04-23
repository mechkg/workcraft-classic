package workcraft.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import workcraft.XmlSerializable;

public class Mat4x4 implements XmlSerializable {
	public static final String _xmlelementtag = "mat4x4";

	private float _m[];
	private float _t1[];
	private float _t2[];
	private float _t3[];
	private float _t4[];

	public float get(int row, int col) {
		return _m[row*4+col];
	}

	public void set(int row, int col, float v) {
		_m[row*4+col] = v;
	}

	public void set(float[] x) {
		for (int i=0; i<16; i++)
			this._m[i] = x[i];
	}

	public void set(double[] x) {
		for (int i=0; i<16; i++)
			this._m[i] = (float)x[i];
	}

	public float[] getArray() {
		return _m;		
	}
	public void setIdentity() {
		for (int i=0; i<4; i++)
			for (int j=0; j<4; j++)
				_m[i*4+j] = (i==j)?1.0f:0.0f;
	}

	public void setScale(float sx, float sy, float sz) {
		for (int i=0; i<4; i++)
			for (int j=0; j<4; j++)
				_m[i*4+j] = 0;
		_m[0] = sx;
		_m[5] = sy;
		_m[10] = sz;
		_m[15] = 1.0f;
	}

	public void setTranslate(float tx, float ty, float tz) {
		for (int i=0; i<4; i++)
			for (int j=0; j<4; j++)
				_m[i*4+j] = (i==j)?1.0f:0.0f;
		_m[3] = tx;
		_m[7] = ty;
		_m[11] = tz;
	}

	public void setRotateZ(float angle) {
		for (int i=0; i<4; i++)
			for (int j=0; j<4; j++)
				_m[i*4+j] = (i==j)?1.0f:0.0f;
		_m[0] = (float)Math.cos(angle);
		_m[1] = (float)Math.sin(angle);
		_m[4] = -(float)Math.sin(angle);
		_m[5] = (float)Math.cos(angle);
	}

	private void alloc() {
		_m = new float[16];
		_t1 = new float[16];
		_t2 = new float[16];
		_t3 = new float[16];
		_t4 = new float[16];
	}

	public Mat4x4(){
		alloc();
		setIdentity();
	}

	public Mat4x4(Mat4x4 other) {
		alloc();
		set (other.getArray());
	}

	public void mul(Mat4x4 other) {
		float[] y = other.getArray();
		
		for (int i=0; i<4; i++)
			for (int j=0; j<4; j++) {
				float s = 0;
				for (int k=0; k<4; k++)
					s += _m[i*4+k]*y[k*4+j];
				_t4[i*4+j] = s; 
			}
		
		set (_t4);
	}

	

	public int g(int a, int b, int i, int j)
	{
		if (a >= i) a++;
		if (b >= j) b++;
		return b * 4 + a;
	}

	public float det3(int i, int j)
	{
		return _m[g(0,0,i,j)] * _m[g(1,1,i,j)] * _m[g(2,2,i,j)] +
		_m[g(0,1,i,j)] * _m[g(1,2,i,j)] * _m[g(2,0,i,j)] +
		_m[g(0,2,i,j)] * _m[g(1,0,i,j)] * _m[g(2,1,i,j)] -
		_m[g(2,0,i,j)] * _m[g(1,1,i,j)] * _m[g(0,2,i,j)] -
		_m[g(1,0,i,j)] * _m[g(0,1,i,j)] * _m[g(2,2,i,j)] -
		_m[g(0,0,i,j)] * _m[g(2,1,i,j)] * _m[g(1,2,i,j)];
	}

	public void  inv()
	{
		float [] res = _t1;

		//		transpose

		float det = _m[0] * det3(0, 0) - _m[4] * det3(0, 1) + _m[8] * det3(0, 2) - _m[12] * det3(0, 3);

		if (Math.abs(det) < 0.0000001f) System.err.println ("*punish*");


		for(int i = 0, s = 1; i < 16; i++, s = -s) res[i] = det3(i / 4, i % 4) / det;

		set(res);
	}

	public void invert() {
		//inv();


		float[] tmp = _t1;
		float[] src =_t2;
		float[] dst = _t3;  

		// Transpose matrix
		for (int i = 0; i < 4; i++) {
			src[i +  0] = _m[i*4 + 0];
			src[i +  4] = _m[i*4 + 1];
			src[i +  8] = _m[i*4 + 2];
			src[i + 12] = _m[i*4 + 3];
		}
		
		// Calculate pairs for first 8 elements (cofactors) 

		tmp[0] = src[10] * src[15];
		tmp[1] = src[11] * src[14];
		tmp[2] = src[9] * src[15];
		tmp[3] = src[11] * src[13];
		tmp[4] = src[9] * src[14];
		tmp[5] = src[10] * src[13];
		tmp[6] = src[8] * src[15];
		tmp[7] = src[11] * src[12];
		tmp[8] = src[8] * src[14];
		tmp[9] = src[10] * src[12];
		tmp[10] = src[8] * src[13];
		tmp[11] = src[9] * src[12];

		// Calculate first 8 elements (cofactors)

		dst[0] = tmp[0]*src[5] + tmp[3]*src[6] + tmp[4]*src[7];
		dst[0] -= tmp[1]*src[5] + tmp[2]*src[6] + tmp[5]*src[7];
		dst[1] = tmp[1]*src[4] + tmp[6]*src[6] + tmp[9]*src[7];
		dst[1] -= tmp[0]*src[4] + tmp[7]*src[6] + tmp[8]*src[7];
		dst[2] = tmp[2]*src[4] + tmp[7]*src[5] + tmp[10]*src[7];
		dst[2] -= tmp[3]*src[4] + tmp[6]*src[5] + tmp[11]*src[7];
		dst[3] = tmp[5]*src[4] + tmp[8]*src[5] + tmp[11]*src[6];
		dst[3] -= tmp[4]*src[4] + tmp[9]*src[5] + tmp[10]*src[6];
		dst[4] = tmp[1]*src[1] + tmp[2]*src[2] + tmp[5]*src[3];
		dst[4] -= tmp[0]*src[1] + tmp[3]*src[2] + tmp[4]*src[3];
		dst[5] = tmp[0]*src[0] + tmp[7]*src[2] + tmp[8]*src[3];
		dst[5] -= tmp[1]*src[0] + tmp[6]*src[2] + tmp[9]*src[3];
		dst[6] = tmp[3]*src[0] + tmp[6]*src[1] + tmp[11]*src[3];
		dst[6] -= tmp[2]*src[0] + tmp[7]*src[1] + tmp[10]*src[3];
		dst[7] = tmp[4]*src[0] + tmp[9]*src[1] + tmp[10]*src[2];
		dst[7] -= tmp[5]*src[0] + tmp[8]*src[1] + tmp[11]*src[2];

		// Calculate pairs for second 8 elements (cofactors)
		tmp[0] = src[2]*src[7];
		tmp[1] = src[3]*src[6];
		tmp[2] = src[1]*src[7];
		tmp[3] = src[3]*src[5];
		tmp[4] = src[1]*src[6];
		tmp[5] = src[2]*src[5];

		tmp[6] = src[0]*src[7];
		tmp[7] = src[3]*src[4];
		tmp[8] = src[0]*src[6];
		tmp[9] = src[2]*src[4];
		tmp[10] = src[0]*src[5];
		tmp[11] = src[1]*src[4];

		// Calculate second 8 elements (cofactors)
		dst[8] = tmp[0]*src[13] + tmp[3]*src[14] + tmp[4]*src[15];
		dst[8] -= tmp[1]*src[13] + tmp[2]*src[14] + tmp[5]*src[15];
		dst[9] = tmp[1]*src[12] + tmp[6]*src[14] + tmp[9]*src[15];
		dst[9] -= tmp[0]*src[12] + tmp[7]*src[14] + tmp[8]*src[15];
		dst[10] = tmp[2]*src[12] + tmp[7]*src[13] + tmp[10]*src[15];
		dst[10]-= tmp[3]*src[12] + tmp[6]*src[13] + tmp[11]*src[15];
		dst[11] = tmp[5]*src[12] + tmp[8]*src[13] + tmp[11]*src[14];
		dst[11]-= tmp[4]*src[12] + tmp[9]*src[13] + tmp[10]*src[14];
		dst[12] = tmp[2]*src[10] + tmp[5]*src[11] + tmp[1]*src[9];
		dst[12]-= tmp[4]*src[11] + tmp[0]*src[9] + tmp[3]*src[10];
		dst[13] = tmp[8]*src[11] + tmp[0]*src[8] + tmp[7]*src[10];
		dst[13]-= tmp[6]*src[10] + tmp[9]*src[11] + tmp[1]*src[8];
		dst[14] = tmp[6]*src[9] + tmp[11]*src[11] + tmp[3]*src[8];
		dst[14]-= tmp[10]*src[11] + tmp[2]*src[8] + tmp[7]*src[9];
		dst[15] = tmp[10]*src[10] + tmp[4]*src[8] + tmp[9]*src[9];
		dst[15]-= tmp[8]*src[9] + tmp[11]*src[10] + tmp[5]*src[8];

		// Calculate determinant
		double det=src[0]*dst[0]+src[1]*dst[1]+src[2]*dst[2]+src[3]*dst[3];


		// Calculate matrix inverse
		det = 1.0f / det;
		for (int i = 0; i < 16; i++)
			_m[i] = (float)(dst[i] * det);
		 /*
		Mat4x4 notThis = new Mat4x4(this);
		//System.err.println (notThis.toString());
		
		notThis.mul(this);

		//System.err.println (notThis.toString());

		try {
			notThis.assertIdentity();
		} catch (Exception e) {
			// e.printStackTrace();
			//System.err.println (this.toString());
			System.err.println (notThis.toString());
		}*/

	}

	public void randomize() {
		for (int i=0; i<16; i++)
			_m[i] = (float)Math.random();
	}

	public void assertIdentity() throws Exception {
		for (int i=0; i<4; i++)
			if ( Math.abs(_m[i*4+i] -1.0f) > 0.0001f )
				throw new Exception("Identity assertion failed!");
		for (int i=0; i<16; i++)
			if ( (i%4) != (i/4))
				if ( Math.abs(_m[i]) > 0.0001f )
					throw new Exception("Identity assertion failed!");
	}

	private void transform() {
		for (int i=0; i<4; i++)
		{
			_t2[i] = 0.0f;			
			for (int j=0; j<4; j++) {
				_t2[i] += _m[i*4+j] * _t1[j];				
			}
		}
	}

	/**
	 * Transforms a 2d vector with this matrix by expanding it to (x, y, 0, 1) 
	 * @param v 2d vector to transform
	 */
	public void transform(Vec2 v) {
		_t1[0] = v.getX(); _t1[1] = v.getY(); _t1[2] = 0.0f; _t1[3] = 1.0f;
		transform();
		v.setXY((float)_t2[0],(float) _t2[1]);		
	}
	/**
	 * Transforms a 3d vector with this matrix by expanding it to (x, y, z, 1) 
	 * @param v 3d vector to transform
	 */
	public void transform(Vec3 v) {
		_t1[0] = v.getX(); _t1[1] = v.getY(); _t1[2] = v.getZ(); _t1[3] = 1.0f;
		transform();
		v.setXYZ((float)_t2[0],(float) _t2[1], (float)_t2[2]);		
	}

	public String getXmlElementName() {
		return null;
	}

	public void fromXmlDom(Element element) {
		for (int i=0; i<16; i++) {
			String data = element.getAttribute("m"+i);
			if (data!=null) {
				_m[i] = Float.parseFloat(data);						
			}
		}		
	}

	public Element toXmlDom(Element parent_element) {
		Document d = parent_element.getOwnerDocument();
		Element e = d.createElement(_xmlelementtag);
		for (int i=0; i<16; i++) {
			e.setAttribute("m"+i, Float.toString(_m[i]));
		}	
		parent_element.appendChild(e);
		return e;
	}

	public String toString() {
		String r ="{";
		for (int i=0; i<16; i++) { 
			if ((i%4) == 0)
				r+="\n";
			else
				r+=", ";
			r+=_m[i];				
		}
		r+="\n}";
		return r;
	}
}