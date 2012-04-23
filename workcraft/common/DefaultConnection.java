package workcraft.common;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.ModelBase;
import workcraft.editor.BasicEditable;
import workcraft.editor.EditableAnchor;
import workcraft.editor.EditableConnection;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.GeometryUtil;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.shapes.Bezier;
import workcraft.visual.shapes.CompoundPath;
import workcraft.visual.shapes.Shape;
import workcraft.visual.shapes.Vertex;

public class DefaultConnection extends EditableConnection  {
	private static Shape arrowShape = null;
	private static Colorf connectionColor = new Colorf (0.0f, 0.0f, 0.5f, 1.0f);
	private static Colorf connectionNoArrowColor = new Colorf (0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf connectionSelectedColor = new Colorf (1.0f, 0.0f, 1.0f, 1.0f);
	public static float hitThreshold = 0.05f;
	public static float polyWeldThreshold = 0.05f;

	public static float arrowLength = 0.015f;
	public static float arrowWidth = 0.004f;
	
	public Colorf colorOverride = null; 
	
	private Vec2 arrowDirection = null;
	

	public enum Type { straightConnection, polylineConnection, bezierConnection };

	public Boolean drawArrow = true;
	public Type connectionType = Type.straightConnection;

//	public Vec2 firstFixedDir = null;
//	public Vec2 secondFixedDir = null;

	public Vec2 [] internal;

	protected Vec2 v1 = null;
	protected Vec2 v2 = null;
	private Vec2 stretch = null;
	
	
	public DefaultConnection() {
		this(null, null);
	}

	public DefaultConnection (BasicEditable first, BasicEditable second) {
		super(first, second);
		if (arrowShape==null) {
			CompoundPath p = new CompoundPath();
			p.addElement(new Vertex(0.0f, 0.0f));
			p.addElement(new Vertex(-arrowLength, arrowWidth));
			p.addElement(new Vertex(-arrowLength, -arrowWidth));
			//p.addElement(new Bezier(new Vec2(-0.0250f, 0.005f), new Vec2(-0.0175f, 0.0f), new Vec2(-0.0175f, 0.0f), new Vec2(-0.0250f, -0.005f) ));
			p.setClosed(true);
			arrowShape = new Shape(p);
		}
	}

	public void setObjects(BasicEditable first, BasicEditable second) {
		this.first = first;
		this.second = second;
	}

	protected void updateStretch() {
		v1 = new Vec2();
		v2 = new Vec2();
		
		first.transform.getLocalToViewMatrix().transform(v1);
		second.transform.getLocalToViewMatrix().transform(v2);

		stretch = new Vec2(v2);
		stretch.sub(v1);
	}

	public List<String> getEditableProperties() {
		LinkedList<String> list = new LinkedList<String>();

		list.add("enum,Type,getConnectionType,setConnectionType,Straight,Polyline,SmartBezier(tm)");
		list.add("bool,Draw arrow,isDrawArrow,setDrawArrow");

		return list;
	}

	public boolean acceptDynamicAnchors() {
		return (connectionType==Type.polylineConnection);
	}

	public void addDynamicAnchor(Vec2 point) {
		if(!acceptDynamicAnchors())
			return;
		int n = (internal==null)?0:internal.length;
		Vec2 v[] = new Vec2[n+2];
		v[0] = v1; v[n+1] = v2;
		for(int i=0; i<n; i++)
			v[i+1] = linearInnerToAnchor(internal[i], v1, v2, stretch);
		int addAfter = -1;
		float min = Float.MAX_VALUE;
		float minv = Float.MAX_VALUE;
		for(int i=0; i<n+1; i++) {
			float di = GeometryUtil.distToSegment(point, v[i], v[i+1]);
			Vec2 s = new Vec2(v[i]);
			s.sub(point);
			float dv = s.length();
			if(di<min) {
				min = di;
				addAfter = i;
			}
			if(dv<minv) {
				minv = dv;
			}
		}
		if(minv<polyWeldThreshold)
			return;
		Vec2 in[] = new Vec2[n+1];
		int i=0;
		for(; i<addAfter; i++)
			in[i] = internal[i];
		in[i] = linearAnchorToInner(point, v1, v2, stretch);
		for(; i<n; i++)
			in[i+1] = internal[i];
		internal = in;
	}

	public void removeAnchor(EditableAnchor anc) {
		if(internal==null)
			return;
		updateStretch();
		Vec2 an = anc.transform.getTranslation2d();
		an.negate();
		LinkedList<Vec2> lst = new LinkedList<Vec2>();
		for(int i=0; i<internal.length; i++) {
			Vec2 in = linearInnerToAnchor(internal[i], v1, v2, stretch);
			if(an.getX()!=in.getX() || an.getY()!=in.getY()) {
				lst.add(internal[i]);
			}
		}
		if(lst.size()!=internal.length)
			internal = lst.toArray(new Vec2[lst.size()]);
	}

	private Vec2 bezierAnchorToInner(Vec2 p, Vec2 v1, Vec2 stretch) {
		float beta = (float) Math.atan2(stretch.getX(), stretch.getY());
		float L = stretch.length();
		if(L<0.001f)
			L = 0.001f;
		Vec2 v = new Vec2(p);
		Mat4x4 mat = new Mat4x4();
		mat.setTranslate(-v1.getX(), -v1.getY(), 0.0f); mat.transform(v);
		mat.setRotateZ(-beta); mat.transform(v);
		v.mul(1/L);
		return v;
	}

	private Vec2 bezierInnerToAnchor(Vec2 p, Vec2 v1, Vec2 stretch) {
		float beta = (float) Math.atan2(stretch.getX(), stretch.getY());
		float L = stretch.length();
		Vec2 v = new Vec2(p);
		v.mul(L);
		Mat4x4 mat = new Mat4x4();
		mat.setRotateZ(beta); mat.transform(v);
		mat.setTranslate(v1.getX(), v1.getY(), 0.0f); mat.transform(v);
		return v;
	}

	private Vec2 linearInnerToAnchor(Vec2 p, Vec2 v1, Vec2 v2, Vec2 stretch) {
		Vec2 v = new Vec2();
		if(p.getX()<0.0f)
			v.setX(p.getX()+v1.getX());
		else if(p.getX()>1.0f)
			v.setX(p.getX()+v2.getX()-1.0f);
		else
			v.setX(p.getX()*stretch.getX()+v1.getX());
		if(p.getY()<0.0f)
			v.setY(p.getY()+v1.getY());
		else if(p.getY()>1.0f)
			v.setY(p.getY()+v2.getY()-1.0f);
		else
			v.setY(p.getY()*stretch.getY()+v1.getY());
		return v;
	}

	private Vec2 linearAnchorToInner(Vec2 p, Vec2 v1, Vec2 v2, Vec2 stretch) {
		Vec2 v = new Vec2();
		if(p.getX()<v1.getX())
			v.setX(p.getX()-v1.getX());
		else if(p.getX()>v2.getX())
			v.setX(p.getX()-v2.getX()+1.0f);
		else if(stretch.getX()==0)
			v.setX(0.0f);
		else
			v.setX((p.getX()-v1.getX())/stretch.getX());
		if(p.getY()<v1.getY())
			v.setY(p.getY()-v1.getY());
		else if(p.getY()>v2.getY())
			v.setY(p.getY()-v2.getY()+1.0f);
		else if(stretch.getY()==0)
			v.setY(0.0f);
		else
			v.setY((p.getY()-v1.getY())/stretch.getY());
		return v;
	}

	public void setInternalPoints(Vec2 [] internalPoints)
	{
		anchors.clear();
		internal = new Vec2[internalPoints.length];
		for(int i=0; i<internal.length; i++)
			anchors.add(new EditableAnchor(internalPoints[i]));
		updatePointsFromAnchors();
		anchors.clear();
	}

	public float hitTest(Vec2 pointInViewSpace) {
		updateStretch();

		switch(connectionType)
		{
		case straightConnection:
			float d = GeometryUtil.distToSegment(pointInViewSpace, v1, v2);
			return (d<=hitThreshold)?d:-1;

		case polylineConnection:
		{
			int n = (internal==null)?0:internal.length;
			Vec2 v[] = new Vec2[n+2];
			v[0] = v1; v[n+1] = v2;
			for(int i=0; i<n; i++)
				v[i+1] = linearInnerToAnchor(internal[i], v1, v2, stretch);
			float min = Float.MAX_VALUE;
			for(int i=0; i<n+1; i++) {
				float di = GeometryUtil.distToSegment(pointInViewSpace, v[i], v[i+1]);
				if(di<min) {
					min = di;
				}
			}
			return (min<=hitThreshold)?min:-1;
		}

		case bezierConnection:

			Vec2 in1 = bezierInnerToAnchor(internal[0], v1, stretch);
			Vec2 in2 = bezierInnerToAnchor(internal[1], v1, stretch);

			Bezier bezier = new Bezier(v1, in1, in2, v2);

			float subdivs = 50.0f; // TODO optimize plz
			float min = Float.MAX_VALUE;
			for(float s=0.0f; s<1.0f; s+=1.0f/subdivs) {
				Vec2 vi0 = bezier.Point(s);
				Vec2 vi1 = bezier.Point(s+1.0f/subdivs);
				float di = GeometryUtil.distToSegment(pointInViewSpace, vi0, vi1);
				if(di<min) {
					min = di;
				}
			}
			return (min<=hitThreshold)?min:-1;
		}
		return -1;
	}

	public void updatePointsFromAnchors() {
		updateStretch();
		switch(connectionType)
		{
		case polylineConnection:
			if(internal!=null) {
				for(int i=0; i<internal.length; i++) {
					Vec2 anc = anchors.get(i).transform.getTranslation2d();
					anc.negate();
					internal[i] = linearAnchorToInner(anc, v1, v2, stretch);
				}
			}
			break;
		case bezierConnection:
		{
			Vec2 anc1 = anchors.get(0).transform.getTranslation2d();
			anc1.negate();
			Vec2 anc2 = anchors.get(1).transform.getTranslation2d();
			anc2.negate();

			internal[0] = bezierAnchorToInner(anc1, v1, stretch);
			internal[1] = bezierAnchorToInner(anc2, v1, stretch);
		}
		break;
		}
	}

	public void select() {
		switch(connectionType)
		{
		case polylineConnection:
			if(internal!=null) {
				for(int i=0; i<internal.length; i++) {
					Vec2 in = linearInnerToAnchor(internal[i], v1, v2, stretch);
					EditableAnchor an = new EditableAnchor(in);
					an.tagIndex = i;
					anchors.add(an);
				}
			}
			break;
		case bezierConnection:
			updateStretch();

			Vec2 in1 = bezierInnerToAnchor(internal[0], v1, stretch);
			Vec2 in2 = bezierInnerToAnchor(internal[1], v1, stretch);

			anchors.add(new EditableAnchor(in1));
			anchors.add(new EditableAnchor(in2));

			break;
		}
		super.select();
	}

	public Vec2 getOutgoingVector()
	{
		Vec2 ret = null;
		switch(connectionType)
		{
		case straightConnection:
			ret = new Vec2(v2);
			ret.sub(v1);
			break;
		case polylineConnection:
			
			int n = (internal==null)?0:internal.length;
			
			if (n>0) {
				ret = linearInnerToAnchor(internal[0], v1, v2, stretch);
				ret.sub(v1);
			} else {
				ret = new Vec2(v2);
				ret.sub(v1);
			}
			break;
		case bezierConnection:
			
			ret = bezierInnerToAnchor(internal[0], v1, stretch);
			ret.sub(v1);
			
			break;
		}
		return ret;
	}

	public Vec2 getIncomingVector()
	{
		Vec2 ret = null;
		switch(connectionType)
		{
		case straightConnection:
			ret = new Vec2(v2);
			ret.sub(v1);
			break;
		case polylineConnection:
			
			int n = (internal==null)?0:internal.length;
			
			if (n>0) {
				
				ret = new Vec2(v2);
				ret.sub(linearInnerToAnchor(internal[n-1], v1, v2, stretch));
			} else {
				ret = new Vec2(v2);
				ret.sub(v1);
			}
			break;
		case bezierConnection:
			
			ret = new Vec2(v2);
			ret.sub(bezierInnerToAnchor(internal[1], v1, stretch));
			
			break;
		}
		return ret;
	}

	public void draw(Painter p)
	{
		updateStretch();

		Colorf connectionColor = DefaultConnection.connectionColor;
		if (colorOverride != null) connectionColor = colorOverride;
		
		if (first.highlight && second.highlight && ((ModelBase)first.getOwnerDocument()).isShowHighlight())
			connectionColor = (((ModelBase)first.getOwnerDocument()).getHighlightColor());
		
		p.pushTransform();
		p.setIdentityTransform();

		p.setLineMode(LineMode.HAIRLINE);
		if(selected) p.setLineColor(connectionSelectedColor);
		else
			if (drawArrow) p.setLineColor(connectionColor); else p.setLineColor(connectionNoArrowColor);

		// drawArrow = false; //*buba*
		switch(connectionType)
		{
		case straightConnection:

			p.drawLine(v1, v2);

			if (drawArrow) 
			{
				Vec2 v3 = this.getArrowPoint();
				v1 = arrowDirection.sub(v3);
				

				float a = (float)Math.atan2(v1.getY(), v1.getX());

				p.translate(v3);
				p.rotate(180.0f + a/(float)Math.PI*180.0f);
				p.scale(2.5f, 2.5f);

				p.setShapeMode(ShapeMode.FILL);
				p.setFillColor((selected)?connectionSelectedColor:connectionColor);

				p.drawShape(arrowShape);
			}

			break;

		case polylineConnection:

//			if(internal==null)
//			fixPolylineInternalPoints();

			int n = (internal==null)?0:internal.length;
			Vec2 [] v = new Vec2[n + 2];

			v[0] = v1;
			v[n+1] = v2;
			for(int i=0; i<n; i++)
				v[i+1] = linearInnerToAnchor(internal[i], v1, v2, stretch);

			p.drawPolyLine(v);

			if (drawArrow) 
			{
				p.pushTransform();
				
				Vec2 v3 = this.getArrowPoint();
				v1 = arrowDirection.sub(v3);

				float a = (float)Math.atan2(v1.getY(), v1.getX());

				p.translate(v3);
				p.rotate(180.0f + a/(float)Math.PI*180.0f);
				p.scale(2.5f, 2.5f);

				p.setShapeMode(ShapeMode.FILL);
				p.setFillColor((selected)?connectionSelectedColor:connectionColor);

				p.drawShape(arrowShape);
				p.popTransform();
			}

			break;

		case bezierConnection:

			Vec2 in1 = bezierInnerToAnchor(internal[0], v1, stretch);
			Vec2 in2 = bezierInnerToAnchor(internal[1], v1, stretch);

			Bezier bezier = new Bezier(v1, in1, in2, v2);

			p.drawBezierCurve(bezier);

			if(selected) {
				p.setLineColor(EditableAnchor.anchorColor);
				p.drawLine(v1, in1);
				p.drawLine(v2, in2);
				in1.negate();
				anchors.get(0).transform.translateAbs(in1);
				in2.negate();
				anchors.get(1).transform.translateAbs(in2);
			}

			if (drawArrow) 
			{
				p.pushTransform();
				Vec2 v3 = this.getArrowPoint();
				v1 = arrowDirection.sub(v3);
			
				float a = (float)Math.atan2(v1.getY(), v1.getX());

				p.translate(v3);
				p.rotate(180.0f + a/(float)Math.PI*180.0f);
				p.scale(2.5f, 2.5f);

				p.setShapeMode(ShapeMode.FILL);
				p.setFillColor((selected)?connectionSelectedColor:connectionColor);

				p.drawShape(arrowShape);
				p.popTransform();
			}

			break;
		}
		super.draw(p);

		p.popTransform();
	}

	private void internalsToXml(Element parent_element) {
		Document d = parent_element.getOwnerDocument();
		Element ie = d.createElement("internal-points");
		if(internal==null) {
			ie.setAttribute("number", "0");
		}
		else {
			ie.setAttribute("number", ""+internal.length);
			for (Vec2 v: internal) {
				v.toXmlDom(ie);
			}
		}
		parent_element.appendChild(ie);
	}

	private void internalsFromXml(Element e) {
		int n = Integer.parseInt(e.getAttribute("number"));
		if (n>0) {
			internal = new Vec2[n];
			NodeList nl = e.getElementsByTagName("vec2");
			for (int i=0; i<nl.getLength(); i++) {
				internal[i] = new Vec2();
				internal[i].fromXmlDom((Element)nl.item(i));
			}
		}
	}

	public Element toXmlDom(Element parent_element) {
		Element ce = createEditableConnectionXmlElement(parent_element);
		Document d = parent_element.getOwnerDocument();
		Element ppe = d.createElement("connection");
		switch (connectionType) {
		case bezierConnection:
			ppe.setAttribute("type", "bezier");
			internalsToXml(ppe);
			break;
		case polylineConnection:
			ppe.setAttribute("type", "polyline");
			internalsToXml(ppe);
			break;
		case straightConnection:
			ppe.setAttribute("type", "straight");
			break;
		}
		ppe.setAttribute("arrow", Boolean.toString(drawArrow));
		ce.appendChild(ppe);
		return ce;
	}

	public void fromXmlDom(Element element) {
		Element ppe = (Element)element.getElementsByTagName("connection").item(0);
		if (ppe.hasAttribute("type")) {
			String type = ppe.getAttribute("type");
			NodeList nl = ppe.getElementsByTagName("internal-points");
			if (type.equals("bezier")) {
				connectionType = Type.bezierConnection;
				internalsFromXml((Element)nl.item(0));
			} else if (type.equals("polyline")) {
				connectionType = Type.polylineConnection;
				internalsFromXml((Element)nl.item(0));			
			}
		}
		
		if (ppe.hasAttribute("arrow"))
			drawArrow = Boolean.parseBoolean(ppe.getAttribute("arrow"));

	}

	public Integer getConnectionType() {
		switch (connectionType) {
		case straightConnection:
			return 0;
		case polylineConnection:
			return 1;
		case bezierConnection:
			return 2;
		}
		return 0;
	}

	public void setConnectionType(Integer connectionType) {
		switch (connectionType) {
		case 0:
			if (this.connectionType != Type.straightConnection) {
				internal = null;
				anchors.clear();
				this.connectionType = Type.straightConnection;				
			}
			break;
		case 1:
			if (this.connectionType != Type.polylineConnection) {
				internal = null;
				anchors.clear();
				this.connectionType = Type.polylineConnection;				
			}
			break;
		case 2:
			if (this.connectionType != Type.bezierConnection) {
				Vec2[] intr = new Vec2[2];
				intr[0] = new Vec2();
				intr[1] = new Vec2();

				Vec2 v1 = new Vec2();
				Vec2 v2 = new Vec2();

				if (first != null && second != null) {
					first.transform.getLocalToViewMatrix().transform(v1);
					second.transform.getLocalToViewMatrix().transform(v2);
				}

				Vec2 v3 = new Vec2(v1);
				Vec2 v4 = new Vec2(v2);
				v1.mul(0.75f);
				v2.mul(0.75f);
				v3.mul(0.25f);
				v4.mul(0.25f);
				v1.add(v4);
				v2.add(v3);

				intr[0] = v1;
				intr[1] = v2;
				
				
				this.connectionType = Type.bezierConnection;
				setInternalPoints(intr);
				select();

				break;
			}
		}
	}

	public Boolean isDrawArrow() {
		return (drawArrow != null)?drawArrow:false;
	}

	public void setDrawArrow(Boolean drawArrow) {
		this.drawArrow = drawArrow;
	}
	
	public Vec2 getArrowPoint()
	{
		float t = 0.0f, dt = 1.0f;
		Vec2 res = null;
		while(dt > 1e-8)
		{
			dt /= 2.0f;
			
			t += dt;
			
			res = getPointOnConnection(t);
			
			if (second.hits(res)) t -= dt;
		}
		
		dt = 1.0f;
		
		while(dt > 1e-8)
		{
			dt /= 2.0f;
			
			t -= dt;
			
			Vec2 v = getPointOnConnection(t);
			
			if (v.sub(res).length() > arrowLength) t += dt;
		}
		
		arrowDirection = getPointOnConnection(t);
		
		return res;
	}

	protected Vec2 getPointOnConnection(float t)
	{
		Vec2 res = new Vec2();
		switch(connectionType)
		{
		case straightConnection:

			res.setX(v1.getX() * (1.0f - t) + v2.getX() * t);
			res.setY(v1.getY() * (1.0f - t) + v2.getY() * t);
			
		break;
		
		case bezierConnection:
			
			Vec2 in1 = bezierInnerToAnchor(internal[0], v1, stretch);
			Vec2 in2 = bezierInnerToAnchor(internal[1], v1, stretch);

			Bezier bezier = new Bezier(v1, in1, in2, v2);			
			
			res = bezier.Point(t);
			
		break;
		
		case polylineConnection:
			
			int n = (internal==null)?0:internal.length;
			Vec2 [] v = new Vec2[n + 2];

			v[0] = v1;
			v[n+1] = v2;
			for(int i=0; i<n; i++)
				v[i+1] = linearInnerToAnchor(internal[i], v1, v2, stretch);			
			
			res.setX(v[n].getX() * (1.0f - t) + v[n + 1].getX() * t);
			res.setY(v[n].getY() * (1.0f - t) + v[n + 1].getY() * t);

		break;
		
		}
		return res;
	}

	public Colorf getColorOverride() {
		return colorOverride;
	}

	public void setColorOverride(Colorf colorOverride) {
		this.colorOverride = colorOverride;
	}

}