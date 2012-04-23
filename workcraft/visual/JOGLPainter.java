package workcraft.visual;
import workcraft.editor.BoundingBox;
import workcraft.util.*;
import workcraft.visual.shapes.*;

import javax.media.opengl.*;

import java.nio.*;
import java.util.WeakHashMap;

import com.sun.opengl.util.*;

public class JOGLPainter implements Painter {

	public class ShapeCacheEntry {
		float outlineWidth;
		boolean closed;
		protected VertexBuffer hairlineOutlineCache = null;
		protected VertexBuffer solidOutlineCache = null;
		protected VertexBuffer fillCache = null;
	}

	private static Shape unitCircle = null;
	private static Rect rectPath = null;
	private static Shape unitRect = null;
	private static Circle circlePath = null;

	private BlendMode blendMode = BlendMode.CONSTANT_ALPHA;
	private Colorf clearColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);;
	private float constantAlpha = 0.5f;
	private Colorf fillColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	private Colorf textColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);;
	private GL gl;
	private Colorf lineColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);;
	private LineMode lineMode = LineMode.HAIRLINE;
	private float lineWidth = 0.01f;
	private int shapeMode = ShapeMode.FILL_AND_OUTLINE;
	private TextRenderer textRenderer = new TextRenderer();
	
	private ViewState view; //for culling only

	private WeakHashMap<Shape, ShapeCacheEntry> shapeCaches = new WeakHashMap<Shape, ShapeCacheEntry>();

	public JOGLPainter(GL gl, ViewState view) {
		this.gl = gl;
		this.view = view;
		if (unitCircle == null) {
			circlePath = new Circle();
			unitCircle = new Shape(new Circle());
		}
		if (unitRect == null) {
			rectPath = new Rect();
			unitRect = new Shape(new Rect());
		}
	}

	public void blendDisable() {
		gl.glDisable(GL.GL_BLEND);

	}

	public void blendEnable() {
		gl.glEnable(GL.GL_BLEND);
	}

	public void clear() {
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
	}
	
	private LineMode checkedLineMode() {
		if(view.getWindowScale()*lineWidth<1.5f)
			return LineMode.HAIRLINE;
		return lineMode;
	}
	
	public void draw(GLSelfDrawable d) {
		d.draw(this.gl);
	}

	public void drawBezierCurve(Bezier curve) {
		drawBezierCurve(curve, 50);
	}

	public void drawBezierCurve (Bezier curve, int segments)  {
		gl.glColor4f(lineColor.r, lineColor.g, lineColor.b, lineColor.a);
		if (checkedLineMode() == LineMode.HAIRLINE) {
			drawPrimitives(GeometryUtil.createHairlineLines(curve.getVertices(segments)), PrimitiveType.LINE_STRIP);
		} else {
			drawPrimitives(GeometryUtil.createSolidLines(curve.getVertices(segments), PrimitiveType.LINE_STRIP, lineWidth), PrimitiveType.TRIANGLE_STRIP);
		}
	}

	public void drawCircle(float radius, float cx, float cy) {
		ShapeCacheEntry cache = getShapeCache(unitCircle);

		if ((shapeMode & ShapeMode.FILL_AND_OUTLINE) == ShapeMode.FILL_AND_OUTLINE) {
			gl.glColor4f (lineColor.r, lineColor.g, lineColor.b, lineColor.a);

			pushTransform();

			if (checkedLineMode() == LineMode.HAIRLINE) {
				translate(cx, cy);
				scale (radius, radius);
				drawShape(unitCircle);
			} else {
				translate(cx, cy);
				scale (radius+lineWidth*0.5f, radius+lineWidth*0.5f);
				gl.glColor4f (lineColor.r, lineColor.g, lineColor.b, lineColor.a);
				drawPrimitives(cache.fillCache, PrimitiveType.TRIANGLE_LIST);
				popTransform();
				pushTransform();
				translate(cx, cy);
				scale (radius-lineWidth*0.5f, radius-lineWidth*0.5f);
				gl.glColor4f (fillColor.r, fillColor.g, fillColor.b, fillColor.a);
				drawPrimitives(cache.fillCache, PrimitiveType.TRIANGLE_LIST);
			}

			popTransform();
		} else {
			if ((shapeMode & ShapeMode.FILL)!=0) {
				gl.glColor4f (fillColor.r, fillColor.g, fillColor.b, fillColor.a);
				pushTransform();
				translate(cx, cy);
				scale (radius, radius);
				drawPrimitives(cache.fillCache, PrimitiveType.TRIANGLE_LIST);
				popTransform();
			}
			if ((shapeMode & ShapeMode.OUTLINE) != 0) {
				gl.glColor4f (lineColor.r, lineColor.g, lineColor.b, lineColor.a);
				if (checkedLineMode() == LineMode.HAIRLINE) {
					pushTransform();
					translate(cx, cy);
					drawPrimitives(cache.hairlineOutlineCache, (cache.closed)?PrimitiveType.LINE_LOOP:PrimitiveType.LINE_STRIP);
					popTransform();
				} else {
					circlePath.setCenter(new Vec2(cx, cy));
					circlePath.setRadius(radius);
					Shape s = new Shape(circlePath);
					drawShape(s);
				}
			}
		}
	}

	public void drawCircle(float radius, Vec2 center) {
		if (center!=null)
			drawCircle(radius, center.getX(), center.getY());
		else
			drawCircle(radius, 0f, 0f);
	}

	private static Vec2[] _dlv = new Vec2[] { new Vec2(), new Vec2() };
	
	
	private void drawLine() {
		gl.glColor4f(lineColor.r, lineColor.g, lineColor.b, lineColor.a);
		if (checkedLineMode() == LineMode.HAIRLINE) {
			drawPrimitives(GeometryUtil.createHairlineLines(_dlv), PrimitiveType.LINE_LIST);
		} else {
			drawPrimitives(GeometryUtil.createSolidLines(_dlv, PrimitiveType.LINE_LIST, lineWidth), PrimitiveType.TRIANGLE_LIST);
		}
	}
	
	public void drawLine(float x0, float y0, float x1, float y1) {
		_dlv[0].setXY(x0, y0);
		_dlv[1].setXY(x1, y1);
		drawLine();		
		
	}	
	public void drawLine (Vec2 from, Vec2 to) {
		_dlv[0].copy(from);
		_dlv[1].copy(to);
		drawLine();
	}

	public void drawLines(Vec2[] vertices) {
		if (vertices.length<2)
			return;
		gl.glColor4f(lineColor.r, lineColor.g, lineColor.b, lineColor.a);
		if (checkedLineMode() == LineMode.HAIRLINE) {
			drawPrimitives(GeometryUtil.createHairlineLines(vertices), PrimitiveType.LINE_LIST);
		} else {
			drawPrimitives(GeometryUtil.createSolidLines(vertices, PrimitiveType.LINE_LIST, lineWidth), PrimitiveType.TRIANGLE_LIST);
		}
	}

	public void drawLines(Vec2[] vertices, int offset, int count) {
		Vec2[] v = new Vec2[count];
		System.arraycopy(vertices, offset, v, 0, count);
		drawLines(v);		
	}	

	public void drawPolyLine(Vec2[] points) {
		gl.glColor4f(lineColor.r, lineColor.g, lineColor.b, lineColor.a);
		if (checkedLineMode() == LineMode.HAIRLINE) {
			drawPrimitives(GeometryUtil.createHairlineLines(points), PrimitiveType.LINE_STRIP);
		} else {
			drawPrimitives(GeometryUtil.createSolidLines(points, PrimitiveType.LINE_STRIP, lineWidth), PrimitiveType.TRIANGLE_STRIP);
		}
	}

	public void drawPolyLine(Vec2[] vertices, int offset, int count) {
		Vec2[] v = new Vec2[count];
		System.arraycopy(vertices, offset, v, 0, count);
		drawPolyLine(v);		
	}

	public void drawPrimitives(VertexBuffer vb, PrimitiveType primitiveType) {
		drawPrimitives (vb, primitiveType, 0, vb.getVertexCount());
	}

	public void drawPrimitives(VertexBuffer vb, PrimitiveType primitiveType, int startVertex, int vertexCount) {
		FloatBuffer buffer = vb.buffer;
		int restore_pos = buffer.position();
		buffer.rewind();

		int offset = startVertex*vb.vertexSizeInFloats;

		if ( (vb.vertexFormat & VertexFormat.VF_POSITION) != 0) {
			buffer.position(offset);
			gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
			gl.glVertexPointer(2, GL.GL_FLOAT, vb.vertexSizeInFloats*BufferUtil.SIZEOF_FLOAT, buffer);
			offset += vb.vertexPositionSize;
		} else {
			gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
		}

		if ( (vb.vertexFormat & VertexFormat.VF_COLOR) != 0) {
			buffer.position(offset);
			gl.glEnableClientState (GL.GL_COLOR_ARRAY);
			gl.glColorPointer(4, GL.GL_FLOAT, vb.vertexSizeInFloats*BufferUtil.SIZEOF_FLOAT, buffer);
			offset += 4;
		} else {
			gl.glDisableClientState(GL.GL_COLOR_ARRAY);
		}

		if ( (vb.vertexFormat & VertexFormat.VF_TEXCOORD) != 0) {
			buffer.position(offset);
			gl.glEnableClientState (GL.GL_TEXTURE_COORD_ARRAY);
			gl.glTexCoordPointer(2, GL.GL_FLOAT, vb.vertexSizeInFloats*BufferUtil.SIZEOF_FLOAT, buffer);
			offset += 2;
		} else {
			gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
		}
		buffer.rewind();

		switch (primitiveType) {
		case POINTS:
			gl.glDrawArrays(GL.GL_POINTS, 0, vertexCount);
			break;
		case LINE_LIST:
			gl.glDrawArrays(GL.GL_LINES, 0, vertexCount);
			break;
		case LINE_LOOP:
			gl.glDrawArrays(GL.GL_LINE_LOOP, 0, vertexCount);
			break;
		case LINE_STRIP:
			gl.glDrawArrays(GL.GL_LINE_STRIP, 0, vertexCount);
			break;
		case TRIANGLE_LIST:
			gl.glDrawArrays(GL.GL_TRIANGLES, 0, vertexCount);
			break;
		case TRIANGLE_STRIP:
			gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, vertexCount);
			break;
		case TRIANGLE_FAN:
			gl.glDrawArrays(GL.GL_TRIANGLE_FAN, 0, vertexCount);
			break;
		default:
			System.err.println("Unknown primitive type encountered !");
		break;
		}
		buffer.position(restore_pos);

	}

	public void drawRect(float left, float top, float right, float bottom) {
		float w = (right - left) ;
		float h = (top - bottom);
		ShapeCacheEntry cache = getShapeCache(unitRect);

		if ((shapeMode & ShapeMode.FILL_AND_OUTLINE) == ShapeMode.FILL_AND_OUTLINE) {
			if (checkedLineMode() == LineMode.HAIRLINE) {
				pushTransform();
				translate (left + w*0.5f,bottom + h*0.5f);
				scale (w, h);
				drawShape(unitRect);
				popTransform();
			} else {
				float w0 = w + lineWidth;
				float h0 = h + lineWidth;
				float w1 = w - lineWidth;
				float h1 = h - lineWidth;

				gl.glColor4f (lineColor.r, lineColor.g, lineColor.b, lineColor.a);
				pushTransform();
				translate (left + w*0.5f,bottom + h*0.5f);
				scale (w0, h0);
				drawPrimitives(cache.fillCache, PrimitiveType.TRIANGLE_LIST);
				popTransform();

				gl.glColor4f (fillColor.r, fillColor.g, fillColor.b, fillColor.a);
				pushTransform();
				translate (left + w*0.5f,bottom + h*0.5f);
				scale (w1, h1);
				drawPrimitives(cache.fillCache, PrimitiveType.TRIANGLE_LIST);
				popTransform();
			}
		} else {
			if ((shapeMode & ShapeMode.FILL)!=0) {
				gl.glColor4f (fillColor.r, fillColor.g, fillColor.b, fillColor.a);
				pushTransform();
				translate (left + w*0.5f,bottom + h*0.5f);
				scale (w, h);
				drawPrimitives(cache.fillCache, PrimitiveType.TRIANGLE_LIST);
				popTransform();
			}
			if ((shapeMode & ShapeMode.OUTLINE) != 0) {
				gl.glColor4f (lineColor.r, lineColor.g, lineColor.b, lineColor.a);
				if (checkedLineMode() == LineMode.HAIRLINE) {
					pushTransform();
					translate (left + w*0.5f,bottom + h*0.5f);
					scale (w, h);
					drawPrimitives(cache.hairlineOutlineCache, cache.closed?PrimitiveType.LINE_LOOP:PrimitiveType.LINE_STRIP);
					popTransform();
				} else {
					rectPath.setCenter(new Vec2(left+w*0.5f, bottom+h*0.5f));
					rectPath.setHeight(h);
					rectPath.setWidth(w);
					Shape s = new Shape(rectPath);
					drawShape(s);
				}
			}
		}
	}


	public void drawRect(Vec2 lowerLeft, Vec2 upperRight) {
		drawRect(lowerLeft.getX(), upperRight.getY(), upperRight.getX(), lowerLeft.getY());
	}

	
	public void drawShape(Shape shape) {
		ShapeCacheEntry cache = getShapeCache(shape);

		if ((shapeMode&ShapeMode.FILL) != 0) {
			gl.glColor4f (fillColor.r, fillColor.g, fillColor.b, fillColor.a);
			drawPrimitives(cache.fillCache, PrimitiveType.TRIANGLE_LIST);
		} 
		if ((shapeMode&ShapeMode.OUTLINE) != 0) {
			gl.glColor4f (lineColor.r, lineColor.g, lineColor.b, lineColor.a);
			if (checkedLineMode() == LineMode.HAIRLINE)
				drawPrimitives(cache.hairlineOutlineCache,  (cache.closed)?PrimitiveType.LINE_LOOP:PrimitiveType.LINE_STRIP);
			else
				drawPrimitives(cache.solidOutlineCache, PrimitiveType.TRIANGLE_STRIP);
		}
	}

	public void drawString(String s, Vec2 start, float height, TextAlign align) {
		drawString(s, start, height, align, null);
	}

	public void drawString (String s, Vec2 start, float height, TextAlign align, String face) {
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		switch (align) {
		case LEFT:
			gl.glTranslatef (start.getX(), start.getY(), 0.0f);
			break;
		case CENTER:
			gl.glTranslatef (start.getX()-textRenderer.getStringWidth(s, height, face)*0.5f, start.getY(), 0.0f);
			break;
		case RIGHT:
			gl.glTranslatef (start.getX()-textRenderer.getStringWidth(s, height, face), start.getY(), 0.0f);
			break;
		}
		gl.glScalef (height, height, 1.0f);
		gl.glColor4f (textColor.r, textColor.g, textColor.b, textColor.a);
		textRenderer.drawString(s, this, face);
	}

	public GL getGl() {
		return gl;
	}

	protected ShapeCacheEntry getShapeCache(Shape shape) {
		ShapeCacheEntry cache = shapeCaches.get(shape);
		if (cache == null) {
			PathElement path = shape.getPath();
			cache = new ShapeCacheEntry();
			cache.outlineWidth = lineWidth;
			cache.solidOutlineCache = GeometryUtil.createSolidPath(path, cache.outlineWidth);
			cache.fillCache = GeometryUtil.createFilledPath(path);
			cache.hairlineOutlineCache = GeometryUtil.createHairlinePath(path);
			cache.closed = path.isClosed();
			shapeCaches.put(shape, cache);
		} else {
			PathElement path = shape.getPath();
			if (cache.closed != path.isClosed()) {
				cache.closed = path.isClosed();
				cache.outlineWidth = lineWidth;
				cache.solidOutlineCache = GeometryUtil.createSolidPath(path, cache.outlineWidth); 
				cache.hairlineOutlineCache = GeometryUtil.createHairlinePath(path);
			} else if (Math.abs(cache.outlineWidth-lineWidth) < Float.MIN_VALUE ) {
				cache.outlineWidth = lineWidth;
				cache.solidOutlineCache = GeometryUtil.createSolidPath(path, cache.outlineWidth); 
			}
		}
		return cache;		
	}

	public float getStringWidth(String s) {
		return getStringWidth (s, 1.0f, null);
	}

	public float getStringWidth(String s, float height) {
		return textRenderer.getStringWidth(s, height);
	}

	public float getStringWidth(String s, float height, String font_face) {
		return textRenderer.getStringWidth(s, height, font_face);
	}

	public void loadFonts(String directory) {
		textRenderer.loadFonts(directory);
	}

	public void popTransform() {
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPopMatrix();
	}

	public void pushTransform() {
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPushMatrix();
	}

	public void rotate(float angle) {
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glRotatef(angle, 0.0f, 0.0f, 1.0f);
	}

	public void scale(float x, float y) {
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glScalef(x, y, 1.0f);
	}

	public void scale(Vec2 s) {
		scale(s.getX(), s.getY());
	}

	public Vec2 screenToClient(Point p) {
		Vec2 r = null;
		return r;
	}

	public void setBlendConstantAlpha(float constantAlpha) {
		this.constantAlpha = constantAlpha;
		gl.glBlendColor(0.0f, 0.0f, 0.0f, constantAlpha);
	}

	public void setBlendMode(BlendMode blendMode) {
		this.blendMode = blendMode;

		switch (blendMode) {
		case CONSTANT_ALPHA:
			gl.glBlendFunc(GL.GL_CONSTANT_ALPHA, GL.GL_ONE_MINUS_CONSTANT_ALPHA);
			break;
		case SRC_ALPHA:
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			break;
		}
	}

	public void setClearColor(Colorf c) {
		clearColor = c;
		gl.glClearColor((float)clearColor.r, (float)clearColor.g, (float)clearColor.b, (float)clearColor.a);
	}

	public void setFillColor(Colorf fillColor) {
		this.fillColor.copy(fillColor);
	}

	public void setIdentityTransform() {
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	public void setLineColor(Colorf lineColor) {
		this.lineColor = lineColor;
	}

	public void setLineMode(LineMode lineMode) {
		this.lineMode = lineMode;		
	}

	public void setLineWidth(float lineWidth) {
		this.lineWidth = lineWidth; 
	}

	public void setProjection(Mat4x4 proj) {
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadTransposeMatrixf(proj.getArray(), 0);
	}

	public void setShapeMode(int shapeMode) {
		this.shapeMode = shapeMode;		
	}

	public void setTextColor(Colorf textColor) {
		this.textColor.copy(textColor);
	}

	public void setTransform(Mat4x4 transform) {
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadTransposeMatrixf(transform.getArray(), 0);
	}

	public void translate(float x, float y) {
		gl.glTranslatef(x, y, 0.0f);
	}

	public void translate(Vec2 t) {
		translate (t.getX(), t.getY());
	}

	public boolean cull(BoundingBox boundingBoxInViewSpace) {
		return view.cull(boundingBoxInViewSpace);
	}

}