package workcraft.visual;

import workcraft.editor.BoundingBox;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.shapes.Bezier;
import workcraft.visual.shapes.Shape;

/**
 * Basic vector graphics drawing interface; provides functions to manage affine
 * transformations and draw elementary shapes, lines, and text to render objects
 * in editor view.
 * 
 * @author Ivan Poliakov (ivan.poliakov@ncl.ac.uk)
 */
public interface Painter {
	/**
	 * Saves active transformation matrix to the stack. The active matrix itself
	 * is not changed, so all further transformations will be applied to it.
	 */
	void pushTransform();

	/**
	 * Restores transformation matrix from top of the stack. The currently
	 * active matrix will be replaced.
	 */
	void popTransform();

	/**
	 * Sets provided matrix as currently active transform. The currently active
	 * matrix will be replaced. (Matrix is stored in row-major order)
	 * 
	 * @param transform
	 *            transformation matrix
	 */
	void setTransform(Mat4x4 transform);

	/**
	 * Sets the transformation matrix to identity matrix, thus resetting all
	 * previously applied transformations.
	 */
	void setIdentityTransform();

	/**
	 * Multiplies active transformation matrix by a scaling matrix.
	 * 
	 * @param sx
	 *            x-axis (horizontal) scale factor
	 * @param sy
	 *            y-axis (vertical) scale factor
	 */
	void scale(float sx, float sy);

	/**
	 * Multiplies active transformation matrix by a scaling matrix.
	 * 
	 * @param s
	 *            scale vector, x component is x-axis scale factor, y component
	 *            is y-axis scale factor
	 */
	void scale(Vec2 s);

	/**
	 * Multiplies active transformation matrix by a translation matrix.
	 * 
	 * @param x
	 *            x-axis (horizontal) translation
	 * @param y
	 *            y-axis (vertical) translation
	 */
	void translate(float x, float y);

	/**
	 * Multiplies active transformation matrix by a translation matrix.
	 * 
	 * @param t
	 *            translation vector
	 */
	void translate(Vec2 t);

	/**
	 * Multiplies active transformation matrix by a rotation matrix. Rotation is
	 * done around the origin (0,0).
	 * 
	 * @param angle
	 *            rotation angle (in radians)
	 */
	void rotate(float angle);

	/**
	 * Sets the fill color. This color is used to draw solid shapes (e.g. filled
	 * rectangles).
	 * 
	 * @param fillColor
	 *            new fill color
	 */
	void setFillColor(Colorf fillColor);

	/**
	 * Sets the line color. This color is used to draw lines and shape outlines
	 * (for both hairline and solid lines).
	 * 
	 * @param lineColor
	 *            new line color
	 */
	void setLineColor(Colorf lineColor);

	/**
	 * Sets the clear color. This color is used to clear the drawing area before
	 * any rendering is done.
	 * 
	 * @param clearColor
	 */
	
	void setTextColor(Colorf textColor);

	/**
	 * Sets the text color. This color is used to draw text.
	 * 
	 * @param textColor
	 */
	
	void setClearColor(Colorf clearColor);

	/**
	 * Sets the line width to be used to draw solid lines (i.e. lines which have
	 * thickness that can be scaled).
	 * 
	 * @param lineWidth
	 *            width (thickness) in world units
	 */
	void setLineWidth(float lineWidth);

	/**
	 * Sets line drawing mode. Following modes are defined: HAIRLINE - lines
	 * will be drawn 1 pixel wide regardless of scale transformation SOLID -
	 * lines will have width equal to current line width (see setLineWidth)
	 * 
	 * @param lineMode
	 *            line drawing mode
	 */
	void setLineMode(LineMode lineMode);

	/**
	 * Sets shape drawing mode flags. Following flags are currently used:
	 * <ul>
	 * <li> OUTLINE - draw shape outline using current line mode and color.</li>
	 * <li> FILL - draw filled shape using current fill color.</li>
	 * <li> FILL_AND_OUTLINE - draw filled shape using current fill color, and
	 * then draw its outline using current line mode and color. </li>
	 * </ul>
	 * 
	 * @param shapeMode
	 *            shape drawing mode
	 */
	void setShapeMode(int shapeMode);

	/**
	 * Clears the drawing area using current clear color.
	 */
	void clear();
	
	void drawLine(float x0, float y0, float x1, float y1);
	
	void drawLine(Vec2 from, Vec2 to);
	/**
	 * Draws a series of one or more connected line segments. The
	 * first vertex specifies the first segment's start point while the second
	 * vertex specifies the first segment's endpoint and the second segment's
	 * start point. In general, the ith vertex (for i > 1) specifies the
	 * beginning of the ith segment and the end of the i-1st. The last vertex
	 * specifies the end of the last segment.
	 * @param vertices array of vertices
	 */
	void drawPolyLine(Vec2[] vertices);
	void drawPolyLine(Vec2[] vertices, int offset, int count);
	/**
	 * Draws a bezier curve using current line mode and color.
	 * @param curve curve object
	 */
	void drawLines(Vec2[] vertices);
	void drawLines(Vec2[] vertices, int offset, int count);
	
	void drawBezierCurve(Bezier curve);
	
	/**
	 * Draws a rectangle using current shape and line modes.
	 * @param left left border x-coordinate
	 * @param top top border y-coordinate
	 * @param right right border x-coordinate
	 * @param bottom bottom border y-coordinate
	 */
	void drawRect(float left, float top, float right, float bottom);

	/**
	 * Draws a rectangle using current shape and line modes.
	 * @param lowerLeft lower left vertex
	 * @param upperRight upper right vertex
	 */
	void drawRect(Vec2 lowerLeft, Vec2 upperRight);

	/**
	 * Draws a circle using current shape and line modes.
	 * @param center center
	 * @param radius radius in world units
	 */
	void drawCircle(float radius, Vec2 center);
	
	/**
	 * Draws a circle using current shape and line modes.
	 * @param center center
	 * @param radius radius in world units
	 */
	void drawCircle(float radius, float cx, float cy);	

	/**
	 * Draws a string using default font and current color starting from specified point and using given height.
	 * @param s string to draw
	 * @param location string location (lower left point of the string)
	 * @param height maximum string height in world units
	 */
	void drawString(String s, Vec2 location, float height, TextAlign align);
	
	/**
	 * Draws a string using specified font, current color, starting from specified point and using given height. If the text renderer doesn't support the requested font, the string is drawn using default font. 
	 * @param s string to draw
	 * @param location string location (lower left point of the string)
	 * @param height maximum string height in world units
	 * @param fontFace desired font face
	 */
	void drawString(String s, Vec2 location, float height, TextAlign align, String fontFace);
	
	/**
	 * Enable alpha-blending (transparency).
	 */
	void blendEnable();

	/**
	 * Set the alpha-blending mode. Following values can be used:
	 * <ul>
	 * <li>CONSTANT_ALPHA - blend using currently set constant alpha (transparency factor)</li>
	 * <li>SRC_ALPHA - blend using alpha values contained in the source (that is, the graphics which are to be drawn) image</li>
	 * </ul>
	 * @param blendMode alpha-blending mode
	 */
	void setBlendMode(BlendMode blendMode);

	/**
	 * The constant alpha (transparency factor) to be used with CONSTANT_ALPHA blending mode.
	 * @param constantAlpha alpha value
	 */
	void setBlendConstantAlpha(float constantAlpha);

	/**
	 * Disable alpha-blending (transparency).
	 */
	void blendDisable();
	void drawShape(Shape shape);
	
	boolean cull(BoundingBox boundingBoxInViewSpace);
}