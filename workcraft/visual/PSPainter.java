package workcraft.visual;

import java.io.PrintWriter;
import java.util.Stack;

import org.w3c.dom.Element;

import workcraft.editor.BoundingBox;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.shapes.Bezier;
import workcraft.visual.shapes.CompoundPath;
import workcraft.visual.shapes.PathElement;
import workcraft.visual.shapes.Shape;

public class PSPainter implements Painter {
	PrintWriter out;
	float scale;
	float llx;
	float lly;
	
	private BlendMode blendMode = BlendMode.CONSTANT_ALPHA;
	private float constantAlpha = 1.0f;
	private Colorf fillColor = new Colorf(0f, 0f, 0f, 1f);
	private Colorf lineColor = new Colorf(0f, 0f, 0f, 1f);
	private Colorf textColor = new Colorf(0f, 0f, 0f, 1f);
	private LineMode lineMode = LineMode.HAIRLINE;
	private int shapeMode = ShapeMode.FILL_AND_OUTLINE;
	private float lineWidth = 0.01f;
	private boolean blendEnabled = false;
	private boolean firstSetTransform = true;

	public PSPainter(PrintWriter out, float scale, float llx, float lly) {
		this.out = out;
		this.scale = scale;
		this.llx = llx;
		this.lly = lly;
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
					r+=String.format("%f %f moveto\n", curve.getP1().getX(), curve.getP1().getY());
				else
					r+=String.format("%f %f lineto\n", curve.getP1().getX(), curve.getP1().getY());
				r+= String.format("%f %f %f %f %f %f curveto\n",
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
						r+=String.format("%f %f moveto\n", vert[i].getX(), vert[i].getY());
					else
						r+=String.format("%f %f lineto\n", vert[i].getX(), vert[i].getY());
				}
			}
		return r;
	}

	public void clear() {
	}
	
	public void writeColor(Colorf color) {
		out.println (String.format("%f %f %f setrgbcolor", color.r, color.g, color.b));
	}
	
	public void writeLineProperties() {
		if (lineMode == LineMode.HAIRLINE)
			out.println (String.format("%f setlinewidth", 0.001f));
		else
			out.println (String.format("%f setlinewidth", lineWidth));
		
		writeColor(lineColor);		
	}
	
	public void writeStrokeAndFill() {
		if ( (shapeMode & ShapeMode.FILL ) != 0 ) {
			writeColor (fillColor);
			if ( (shapeMode & ShapeMode.OUTLINE) != 0 )
				out.println ("gsave");
			out.println ("fill");
			if ( (shapeMode & ShapeMode.OUTLINE) != 0 )
				out.println ("grestore");
		}
		
		if ( (shapeMode & ShapeMode.OUTLINE) != 0 ) {
			writeLineProperties();
			out.println ("stroke");
		}
		

	}

	public void drawBezierCurve(Bezier curve) {
//		int shapeBack = shapeMode;
	//	setShapeMode(ShapeMode.OUTLINE);
		
		
		
		out.println("newpath");
		out.println(String.format("%f %f moveto", curve.getP1().getX(), curve.getP1().getY()));
		out.println(String.format("%f %f %f %f %f %f curveto", curve.getCp1().getX(), curve.getCp1().getY(),
				curve.getCp2().getX(), curve.getCp2().getY(),
				curve.getP2().getX(), curve.getP2().getY()));
		
		writeLineProperties();		
		out.println("stroke");
		
		//setShapeMode(shapeBack);
	}

	public void drawCircle(float radius, float cx, float cy) {
		writeLineProperties();
		out.println("newpath");
		out.println(String.format("%f %f %f %d %d arc\n closepath", cx, cy, radius, 0, 360));
		writeStrokeAndFill();

	}

	public void drawCircle(float radius, Vec2 center) {
		if (center!=null)
			drawCircle(radius, center.getX(), center.getY());
		else
			drawCircle(radius, 0f, 0f);
	}

	public void drawLine(float x0, float y0, float x1, float y1) {
		writeLineProperties();
		out.println("newpath");
		out.println(String.format("%f %f moveto\n%f %f lineto", x0,y0,x1,y1 ));
		out.println("stroke");
	}

	public void drawLine(Vec2 from, Vec2 to) {
		drawLine(from.getX(), from.getY(), to.getX(), to.getY());
	}

	public void drawLines(Vec2[] vertices) {
		writeLineProperties();
		for (int i=0; i<vertices.length; i+=2) {
			out.println("newpath");
			out.println(String.format("%f %f moveto\n%f %f lineto", vertices[i].getX(), vertices[i].getY(), vertices[i+1].getX(), vertices[i+1].getY()));
			out.println("stroke");
		}
	}

	public void drawLines(Vec2[] vertices, int offset, int count) {
		Vec2[] v = new Vec2[count];
		System.arraycopy(vertices, offset, v, 0, count);
		drawLines(v);				
	}
	
	public void drawPolyLine(Vec2[] vertices) {
		writeLineProperties();
		out.println("newpath");
		
		String path = "";
		boolean first = true;
		for (Vec2 v : vertices) {
			if (!first)
				out.println(String.format("%f %f lineto", v.getX(), v.getY()));
			else {
				out.println(String.format("%f %f moveto", v.getX(), v.getY())); 
				first = false;
			}
		}
		
		out.println("stroke");
	}
	
	public void drawPolyLine(Vec2[] vertices, int offset, int count) {
		Vec2[] v = new Vec2[count];
		System.arraycopy(vertices, offset, v, 0, count);
		drawPolyLine(v);
	}

	public void drawRect(float left, float top, float right, float bottom) {
		
		out.println ("newpath");
		out.println (String.format("%f %f moveto", left, bottom));
		out.println (String.format("%f %f lineto", right, bottom));
		out.println (String.format("%f %f lineto", right, top));
		out.println (String.format("%f %f lineto", left, top));
		out.println ("closepath");
		
		writeStrokeAndFill();
	}


	public void drawRect(Vec2 lowerLeft, Vec2 upperRight) {
		drawRect(lowerLeft.getX(), upperRight.getY(), upperRight.getX(), lowerLeft.getY());
	}

	public void drawShape(Shape shape) {
		PathElement p = shape.getPath();
		
		out.println ("newpath");
		out.print (buildPathDef(p, true));
		if (p.isClosed())
			out.println("closepath");
			
		writeStrokeAndFill();
	}

	public void drawString(String s, Vec2 location, float height, TextAlign align) {
		drawString(s, location, height, align, "Verdana");
	}

	public void drawString(String s, Vec2 location, float height, TextAlign align, String fontFace) {
		pushTransform();
		setIdentityTransform();
		out.println("/Helvetica findfont");
		out.println(String.format("%f scalefont", height));
		out.println("setfont");
		writeColor(textColor);
		
		s.replace("(", "\\(");
		s.replace(")", "\\)");
		
		out.print("("+s+") ");
		
		
		switch (align) {
		case LEFT:
			out.print(String.format("%f %f moveto ", location.getX(), location.getY()));
			break;
		case RIGHT:
			out.print("dup stringwidth pop -1 mul "+location.getX()+" add "+location.getY()+" moveto ");
			break;
		case CENTER:
			out.print("dup stringwidth pop -0.5 mul "+location.getX()+" add "+location.getY()+" moveto ");
			break;
		}
		
		out.println(" show");
		
		popTransform();
	}

	public void popTransform() {
		out.println ("%popTransform");
		out.println ("setmatrix");

	}

	public void pushTransform() {
		out.println ("%pushTransform");
		out.println ("matrix currentmatrix");
	}

	public void rotate(float angle) {
		out.println (String.format("%f rotate", angle));

	}

	public void scale(float sx, float sy) {
		out.println (String.format("%f %f scale", sx, sy));
	}

	public void scale(Vec2 s) {
		scale (s.getX(), s.getY());
	}

	public void setBlendConstantAlpha(float constantAlpha) {
		this.constantAlpha = constantAlpha;
	}

	public void setBlendMode(BlendMode blendMode) {
		this.blendMode = blendMode;
	}

	public void setClearColor(Colorf clearColor) {

	}

	public void setFillColor(Colorf fillColor) {
		this.fillColor.copy(fillColor);
	}

	public void setIdentityTransform() {
		setTransform(new Mat4x4());
		//out.println (String.format("[%f 0.000000 0.000000 %f %f %f] setmatrix", scale, scale, llx*scale, lly*scale));
		//out.println (String.format("%f %f translate\n%f %f scale", llx, lly, scale, scale));
	}

	public void setLineColor(Colorf lineColor) {
		this.lineColor.copy(lineColor);
	}

	public void setLineMode(LineMode lineMode) {
		this.lineMode = lineMode;
	}

	public void setLineWidth(float lineWidth) {
		this.lineWidth = lineWidth;
	}

	public void setShapeMode(int shapeMode) {
		this.shapeMode = shapeMode;
	}

	public void setTextColor(Colorf textColor) {
		this.textColor.copy (textColor);		
	}

	public void setTransform(Mat4x4 transform) 
	{
		out.println ("%setTransform");
		if (firstSetTransform) {
			out.println ("/ctmSave [[]] def");
			out.println ("ctmSave 0 matrix currentmatrix put");
			firstSetTransform = false;
		} else {
			out.println ("ctmSave 0 get");
			out.println ("setmatrix");
		}
		float[] a = transform.getArray();
		out.println (String.format("[%f %f %f %f %f %f] concat",a[0]*scale,-a[1]*scale,-a[4]*scale,a[5]*scale, (a[3]+llx)*scale , (a[7]+lly)*scale));
	}

	public void translate(float x, float y) {
		out.println (String.format("%f %f translate", x, y));
	}

	public void translate(Vec2 t) {
		translate(t.getX(), t.getY());
	}
	public boolean cull(BoundingBox boundingBoxInViewSpace) {
		return false;
	}

}
