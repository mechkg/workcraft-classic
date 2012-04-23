package workcraft.visual;

import java.util.Stack;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import workcraft.editor.BoundingBox;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.shapes.Bezier;
import workcraft.visual.shapes.CompoundPath;
import workcraft.visual.shapes.PathElement;
import workcraft.visual.shapes.Shape;

public class SVGPainter implements Painter {
	private Document doc;

	Stack<Element> gstack=new Stack<Element>();
	Element root, g;

	private BlendMode blendMode = BlendMode.CONSTANT_ALPHA;
	private float constantAlpha = 1.0f;
	private Colorf fillColor = new Colorf(0f, 0f, 0f, 1f);
	private Colorf lineColor = new Colorf(0f, 0f, 0f, 1f);
	private Colorf textColor = new Colorf(0f, 0f, 0f, 1f);
	private LineMode lineMode = LineMode.HAIRLINE;
	private int shapeMode = ShapeMode.FILL_AND_OUTLINE;
	private float lineWidth = 0.01f;
	private boolean blendEnabled = false;

	private String fillStyle="fill:rgb(0,0,0);";
	private String strokeStyle="stroke:rgb(0,0,0);";
	private String strokeWidthStyle="stroke-width:0.002;";
	private String opacityStyle="";

	public SVGPainter(Element root) {
		doc = root.getOwnerDocument();
		this.root = root;
		g = root;
	}

	public void blendDisable() {
		blendEnabled = false;
	}

	public void blendEnable() {
		blendEnabled = true;
	}

	private String buildPathDef(PathElement p, boolean first) {
		String r = "";
		if (p instanceof CompoundPath) {
			boolean first_e = true;
			for (PathElement e : ((CompoundPath)p).getElements()) {
				if (first_e) {
					r+=buildPathDef(e, first);
					first_e = false;
				} else
					r+=buildPathDef(e, false);
			}
		} else 
			if (p instanceof Bezier) {
				Bezier curve = (Bezier)p;
				if (first)
					r+=String.format("M%f,%f", curve.getP1().getX(), curve.getP1().getY());
				else
					r+=String.format("L%f,%f", curve.getP1().getX(), curve.getP1().getY());
				r+= String.format("C%f,%f %f,%f %f,%f",
						curve.getCp1().getX(), curve.getCp1().getY(),
						curve.getCp2().getX(), curve.getCp2().getY(),
						curve.getP2().getX(), curve.getP2().getY());
			}
			else
			{
				int n = p.getNumberOfVertices();
				Vec2[] vert = new Vec2[n];
				p.getVertices(vert, 0);
				for (int i=0; i<n; i++) {
					if (first && (i==0))
						r+=String.format("M%f,%f", vert[i].getX(), vert[i].getY());
					else
						r+=String.format("L%f,%f", vert[i].getX(), vert[i].getY());
				}
			}
		return r;
	}

	public void clear() {
	}

	public void drawBezierCurve(Bezier curve) {
		int shapeBack = shapeMode;
		setShapeMode(ShapeMode.OUTLINE);

		Element path = doc.createElement("path");

		path.setAttribute("d", String.format("M%f,%f C%f,%f %f,%f %f,%f",
				curve.getP1().getX(), curve.getP1().getY(),
				curve.getCp1().getX(), curve.getCp1().getY(),
				curve.getCp2().getX(), curve.getCp2().getY(),
				curve.getP2().getX(), curve.getP2().getY()));

		putElement(path);	
		setShapeMode(shapeBack);
	}

	public void drawCircle(float radius, float cx, float cy) {
		Element circle = doc.createElement("circle");

		circle.setAttribute("cx", Float.toString(cx));
		circle.setAttribute("cy", Float.toString(cy));
		circle.setAttribute("r", Float.toString(radius));

		putElement(circle);
	}

	public void drawCircle(float radius, Vec2 center) {
		if (center!=null)
			drawCircle(radius, center.getX(), center.getY());
		else
			drawCircle(radius, 0f, 0f);
	}

	public void drawLine(float x0, float y0, float x1, float y1) {
		int shapeBack = shapeMode;
		setShapeMode(ShapeMode.OUTLINE);
		Element line = doc.createElement("line");

		line.setAttribute("x1", Float.toString(x0));
		line.setAttribute("x2", Float.toString(x1));
		line.setAttribute("y1", Float.toString(y0));
		line.setAttribute("y2", Float.toString(y1));

		putElement(line);
		setShapeMode(shapeBack);
	}

	public void drawLine(Vec2 from, Vec2 to) {
		drawLine(from.getX(), from.getY(), to.getX(), to.getY());
	}

	public void drawLines(Vec2[] vertices) {
		int shapeBack = shapeMode;
		setShapeMode(ShapeMode.OUTLINE);
		Element line = doc.createElement("path");

		String d = ""; 
		for (int i=0; i<vertices.length; i+=2) {
			d+="M"+Float.toString(vertices[i].getX())+","+Float.toString(vertices[i].getY());
			d+="L"+Float.toString(vertices[i+1].getX())+","+Float.toString(vertices[i+1].getY());
		}

		line.setAttribute("d", d);
		putElement(line);
		setShapeMode(shapeBack);
	}

	public void drawLines(Vec2[] vertices, int offset, int count) {
		int shapeBack = shapeMode;
		setShapeMode(ShapeMode.OUTLINE);
		Vec2[] v = new Vec2[count];
		System.arraycopy(vertices, offset, v, 0, count);
		drawLines(v);				
		setShapeMode(shapeBack);
	}
	
	public void setRootTransform() {
		root = g;
	}

	public void drawPolyLine(Vec2[] vertices) {
		int shapeBack = shapeMode;
		setShapeMode(ShapeMode.OUTLINE);

		Element line = doc.createElement("path");
		
		String path = "";
		boolean first = true;
		for (Vec2 v : vertices) {
			if (!first) {
				path +=" L";
				path += Float.toString(v.getX());
				path += ",";
				path += Float.toString(v.getY());
			}
			else {
				path = String.format("M%f,%f", v.getX(), v.getY()); 
				first = false;
			}
		}

		line.setAttribute("d", path);

		putElement(line);
		setShapeMode(shapeBack);
	}
	
	public void drawPolyLine(Vec2[] vertices, int offset, int count) {
		Vec2[] v = new Vec2[count];
		System.arraycopy(vertices, offset, v, 0, count);
		drawPolyLine(v);
	}

	public void drawRect(float left, float top, float right, float bottom) {
		Element rect = doc.createElement("rect");

		rect.setAttribute("x", Float.toString(left));
		rect.setAttribute("y", Float.toString(-bottom-(top-bottom)));
		rect.setAttribute("width", Float.toString(right-left));
		rect.setAttribute("height", Float.toString(top-bottom));

		putElement(rect);
	}


	public void drawRect(Vec2 lowerLeft, Vec2 upperRight) {
		drawRect(lowerLeft.getX(), upperRight.getY(), upperRight.getX(), lowerLeft.getY());
	}

	public void drawShape(Shape shape) {
		PathElement p = shape.getPath();
	
		Element path = doc.createElement("path");
		path.setAttribute("d", buildPathDef(p, true)+(p.isClosed()?"z":""));
				
		putElement(path);
	}

	public void drawString(String s, Vec2 location, float height, TextAlign align) {
		drawString(s, location, height, align, "Verdana");
		
	}

	public void drawString(String s, Vec2 location, float height, TextAlign align, String fontFace) {
		Element t = doc.createElement("text");
		pushTransform();
		setIdentityTransform();
		scale (1.0f, -1.0f);
		t.appendChild(doc.createTextNode(s));
		t.setAttribute("x", Float.toString(location.getX()));
		t.setAttribute("y", Float.toString(-location.getY()));
		t.setAttribute("font-size", Float.toString(height));
		
		String style = "";
		int r = (int)(255.0f*textColor.r);
		int g = (int)(255.0f*textColor.g);
		int b = (int)(255.0f*textColor.b);
		t.setAttribute("fill",  String.format("rgb(%d, %d, %d)", r,g,b));
		if (blendEnabled) {
			style += String.format("opacity:%f;", (blendMode == BlendMode.CONSTANT_ALPHA)?constantAlpha:textColor.a);
		}
		switch (align) {
		case LEFT:
			 t.setAttribute("text-anchor", "start");
			break;
		case CENTER:
			t.setAttribute("text-anchor", "middle");
			break;
		case RIGHT:
			t.setAttribute("text-anchor", "end");
		}
		
		t.setAttribute("style", style);
		
		this.g.appendChild(t);
		popTransform();
	}

	public void popTransform() {
		g = gstack.pop();
	}

	public void pushTransform() {
		gstack.push(g);
	}

	private void putElement(Element e) {
		String style=fillStyle+strokeStyle;
		if ((shapeMode & ShapeMode.OUTLINE) != 0)
			style+=strokeWidthStyle;
		if (blendEnabled)
			style+=opacityStyle;
		e.setAttribute ("style", style);
		g.appendChild(e);
	}

	public void rotate(float angle) {
		Element new_g = doc.createElement("g");
		g.appendChild(new_g);
		g = new_g;
		g.setAttribute("transform", String.format("rotate (%f,0,0)", angle));
	}

	public void scale(float sx, float sy) {
		Element new_g = doc.createElement("g");
		g.appendChild(new_g);
		g = new_g;
		g.setAttribute("transform", String.format("scale (%f,%f)", sx, sy));
	}

	public void scale(Vec2 s) {
		scale (s.getX(), s.getY());
	}

	public void setBlendConstantAlpha(float constantAlpha) {
		this.constantAlpha = constantAlpha;
		updateOpacityStyle();
	}

	public void setBlendMode(BlendMode blendMode) {
		this.blendMode = blendMode;
	}

	public void setClearColor(Colorf clearColor) {

	}

	public void setFillColor(Colorf fillColor) {
		this.fillColor.copy(fillColor);
		updateFillStyle();
	}

	public void setIdentityTransform() {
		g = root;
	}

	public void setLineColor(Colorf lineColor) {
		this.lineColor.copy(lineColor);
		updateStrokeStyle();
	}

	public void setLineMode(LineMode lineMode) {
		this.lineMode = lineMode;
		updateStrokeStyle();
		updateStrokeWidthStyle();
	}

	public void setLineWidth(float lineWidth) {
		this.lineWidth = lineWidth;
		updateStrokeWidthStyle();
	}

	public void setShapeMode(int shapeMode) {
		this.shapeMode = shapeMode;
		updateFillStyle();
		updateStrokeStyle();
	}

	public void setTextColor(Colorf textColor) {
		this.textColor.copy (textColor);		
	}

	public void setTransform(Mat4x4 transform) {
		g = doc.createElement("g");
		float[] a = transform.getArray();
		g.setAttribute("transform", String.format("matrix(%f,%f,%f,%f,%f,%f)",a[0],a[4],a[1],a[5],a[3],a[7] ));
		root.appendChild(g);
	}

	public void translate(float x, float y) {
		Element new_g = doc.createElement("g");
		g.appendChild(new_g);
		g = new_g;
		g.setAttribute("transform", String.format("translate (%f,%f)", x, y));		
	}

	public void translate(Vec2 t) {
		translate(t.getX(), t.getY());
	}

	private void updateFillStyle () {
		if ((shapeMode & ShapeMode.FILL)!=0) {
			int r = (int)(255.0f*fillColor.r);
			int g = (int)(255.0f*fillColor.g);
			int b = (int)(255.0f*fillColor.b);
			fillStyle = String.format("fill:rgb(%d, %d, %d);", r,g,b);
		} else {
			fillStyle = "fill:none;";
		}
	}

	private void updateOpacityStyle() {
		if (blendMode == BlendMode.CONSTANT_ALPHA)
			opacityStyle += String.format("opacity:%f;", constantAlpha);
		else
			opacityStyle += String.format("stroke-opacity:%f;fill-opacity:%f;", lineColor.a, fillColor.a);
	}

	private void updateStrokeStyle () {
		if ((shapeMode & ShapeMode.OUTLINE)!=0) {
			int r = (int)(255.0f*lineColor.r);
			int g = (int)(255.0f*lineColor.g);
			int b = (int)(255.0f*lineColor.b);
			strokeStyle = String.format("stroke:rgb(%d, %d, %d);", r,g,b);
		} else {
			strokeStyle = "stroke:none;";
		}
	}

	private void updateStrokeWidthStyle () {
		if ((lineMode == LineMode.HAIRLINE))
			strokeWidthStyle="stroke-width:0.002;";
		else
			strokeWidthStyle = String.format("stroke-width:%f;", lineWidth);
	}

	public boolean cull(BoundingBox boundingBoxInViewSpace) {
		return false;
	}

}
