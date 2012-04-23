package workcraft.visual;

import workcraft.util.*;
import workcraft.visual.shapes.Bezier;
import workcraft.visual.shapes.PathElement;

import java.nio.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;

/**
 * Provides helper functions to support geometry generation operations.
 * 
 * @author Ivan Poliakov
 * 
 */
public class GeometryUtil {
	public static class ParallelLinesException extends Exception{}; 
	private static Vec2 v1 = new Vec2();
	private static Vec2 v2 = new Vec2();
	private static Vec2 v3 = new Vec2();
	private static Vec2 v4 = new Vec2();
	private static Vec2 v5 = new Vec2();
	private static Vec2 v6 = new Vec2();
	private static Vec2 v7 = new Vec2();
	private static Vec2 v8 = new Vec2();
	private static Vec2 p0 = new Vec2();
	private static Vec2 p1 = new Vec2();
	private static Vec2 p2 = new Vec2();
	private static Vec2 p3 = new Vec2();

	private static Vec2[] quad =  {
		new Vec2(),  new Vec2(),  new Vec2(), new Vec2()
	};


	/**
	 * Puts a rounded rectangle into the vertex buffer. The rectangle segments are generated in the form of a line strip.
	 * @param lower_left rectangle lower left corner
	 * @param upper_right rectangle upper right corner
	 * @param r rounded corner radius 
	 * @param segments number of segments to use for each rounded corner
	 * @param vb buffer to put vertex data into (must have enough free space). If null, no actual vertices will be added
	 * @return if vb is not null, number of vertices added. If vb is null,
	 *         amount of space needed in vertex buffer (in vertices)
	 *//*
	public static int addRoundRect2d(Vec2 lower_left, Vec2 upper_right,
			float r, int segments, VertexBuffer vb) {
		if (lower_left == null)
			lower_left = new Vec2();
		if (upper_right == null)
			upper_right = new Vec2();

		int vertices = 0;
		Vec2 p1 = new Vec2(), p2 = new Vec2(), cp;
		Bezier bezier = new Bezier();

		p1.setXY(lower_left.getX(), lower_left.getY() + r);
		p2.setXY(lower_left.getX() + r, lower_left.getY());
		bezier.setControlPoints(p1, lower_left, lower_left, p2);
		vertices += addBezierCurve(bezier, segments, vb);

		cp = new Vec2(upper_right);
		cp.setY(lower_left.getY());

		p1.setXY(upper_right.getX() - r, lower_left.getY());
		p2.setXY(upper_right.getX(), lower_left.getY() + r);
		bezier.setControlPoints(p1, cp, cp, p2);
		vertices += addBezierCurve(bezier, segments, vb);

		cp.setXY(upper_right.getX(), upper_right.getY());
		p1.setXY(upper_right.getX(), upper_right.getY() - r);
		p2.setXY(upper_right.getX() - r, upper_right.getY());
		bezier.setControlPoints(p1, upper_right, upper_right, p2);
		vertices += addBezierCurve(bezier, segments, vb);

		cp.setXY(lower_left.getX(), upper_right.getY());
		p1.setXY(lower_left.getX() + r, upper_right.getY());
		p2.setXY(lower_left.getX(), upper_right.getY() - r);
		bezier.setControlPoints(p1, cp, cp, p2);
		vertices += addBezierCurve(bezier, segments, vb);
		vertices += addVertex2d(lower_left.getX(), lower_left.getY() + r, vb);

		return vertices;
	}*/

	private static void buildQuadPoints (float x0, float y0, float x1, float y1, float width ) {
		v1.setXY(x1-x0, y1-y0);
		v1.normalize();
		v2.setXY(x0, y0);
		v3.setXY(x1, y1);

		v1.mul(width);
		v4.setXY(-v1.getY(), v1.getX());
		v5.setXY(v1.getY(), -v1.getX());

		quad[0].copy(v2);
		quad[0].add(v4);
		quad[1].copy(v2);
		quad[1].add(v5);
		quad[2].copy(v3);
		quad[2].add(v4);
		quad[3].copy(v3);
		quad[3].add(v5);
	}

	public static float distToSegment(Vec2 P, Vec2 p0, Vec2 p1) {
		Vec2 p = new Vec2(P);
		Vec2 v = new Vec2(p1); v.sub(p0);
		Vec2 w = new Vec2(p); w.sub(p0);
		float c1 = w.dot(v);
		if(c1<=0) {
			p.sub(p0);
			return p.length();
		}
		float c2 = v.dot(v);
		if(c2<=c1) {
			p.sub(p1);
			return p.length();
		}
		float b = c1/c2;
		v.mul(b);
		v.add(p0);
		p.sub(v);
		return p.length();
	}

	public static Vec2 lineIntersect(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) throws ParallelLinesException
	{
		Vec2 r = new Vec2();

		float a1 = y0 - y1;
		float b1 = x1 - x0;
		float c1 = x0*y1 - x1*y0;

		float a2 = y2 - y3;
		float b2 = x3 - x2;
		float c2 = x2*y3 - x3*y2;

		float det = a1*b2 - a2*b1;

		if (Math.abs(det) < Float.MIN_VALUE) throw new ParallelLinesException();

		float detx = -c1*b2 + c2*b1;
		float dety = -a1*c2 + a2*c1;

		r.setXY(detx/det, dety/det);

		return r;
	}

	private static boolean verticesCloseEnough(Vec2 v1, Vec2 v2) {
		if ( (Math.abs(v2.getX()-v1.getX()) < 1E-6f) &&
				(Math.abs(v2.getY()-v1.getY()) < 1E-6f ))
			return true;
		else
			return false;

	}

	private static Vec2[] weldVertices(Vec2 v[], boolean isClosed) {
		int c = 0;

		if (isClosed) {
			if (verticesCloseEnough(v[v.length-1], v[0])) {
				//System.out.println ("Welding "+v[v.length-1].toString()+", "+v[0].toString());
				c++;
			}
		}

		for (int i=1; i<v.length; i++)
			if (verticesCloseEnough(v[i-1], v[i])) {
				//System.out.println ("Welding "+v[i].toString()+", "+v[i-1].toString());
				c++;
			}


		Vec2[] r = new Vec2[v.length - c];
		int cnt = 0;

		if (isClosed) {
			if (!verticesCloseEnough(v[v.length-1], v[0])) {
				r[cnt++] = v[0];
			}
		} else
			r[cnt++] = v[0];

		for (int i=1; i<v.length; i++)
		{
			if (!verticesCloseEnough(v[i-1], v[i])) {
				r[cnt++] = v[i];
			}
		}

		return r;
	}

	public static float cross(Vec2 v1, Vec2 v2, Vec2 v3) {
		return v1.getX()*(v2.getY()-v3.getY())+v2.getX()*(v3.getY()-v1.getY())+v3.getX()*(v1.getY()-v2.getY());

	}

	public static boolean isPointInTriangle(Vec2 v1, Vec2 v2, Vec2 v3, Vec2 p) {
		float s1 = cross (v1, v2, p);
		float s2 = cross (v2, v3, p);
		float s3 = cross (v3, v1, p);


		//System.out.println ("\t"+s1+" "+s2+" "+s3);
		if ( 
				(s1<=0 && s2<=0 && s3<=0 ) ||
				(s1>=0 && s2>=0 && s3>=0)
		) {
			//System.out.println ("Point "+p.toString()+"is IN triangle "+v1.toString()+","+v2.toString()+","+v3.toString());

			return true;
		}
		else {
			//System.out.println ("Point "+p.toString()+"is NOT IN triangle "+v1.toString()+","+v2.toString()+","+v3.toString());	
			return false;
		}
	}

	public static VertexBuffer createSolidPath(PathElement path, float width) {
		Vec2[] v = new Vec2[path.getNumberOfVertices()];
		path.getVertices(v, 0);
		Vec2[] w = weldVertices(v, path.isClosed());

		return createSolidLines(w, (path.isClosed()?PrimitiveType.LINE_LOOP:PrimitiveType.LINE_STRIP), width);
	}

	public static VertexBuffer createHairlinePath(PathElement path) {
		VertexBuffer output = null;
		Vec2[] v = new Vec2[path.getNumberOfVertices()];
		path.getVertices(v, 0);
		Vec2[] w = weldVertices(v, path.isClosed());

		try {
			output = new VertexBuffer (VertexFormat.VF_POSITION, 2, w.length);
		} catch (VertexFormatException e) {}

		for (Vec2 vtx: w) {
			output.pos(vtx.getX(), vtx.getY());
			output.next();
		}

		return output;
	}

	public static VertexBuffer createFilledPath(PathElement path) {
		Stack<Vec2> triangles = new Stack<Vec2>();

		Vec2[] pathVertices = new Vec2[path.getNumberOfVertices()];
		path.getVertices(pathVertices, 0);
		Vec2[] cleanedVertices = weldVertices(pathVertices, true);

		LinkedList<Vec2> vertices = new LinkedList<Vec2>();

		vertices.add(cleanedVertices[0]);
		for (Vec2 v : cleanedVertices)
			vertices.add(v);
		vertices.add(cleanedVertices[cleanedVertices.length-1]);

		while (vertices.size() > 3) { // until polygon is not a triangle
			ListIterator<Vec2> i = vertices.listIterator();

			Vec2 v1, v2, v3;
			i.next(); i.next();

			boolean earCut = false;

			for (int k=0;k<vertices.size()-3;k++) {

//				System.err.println(k);

				v1 = i.previous();
				i.next();
				v2 = i.next();
				v3 = i.next();

				i.previous();
				i.previous();

				/*System.out.println(v1.toString());
				System.out.println(v2.toString());
				System.out.println(v3.toString());*/

				boolean goodEar = true;

				if (cross(v1, v2, v3) < 0.0f)
					goodEar = false;

				if (goodEar) {
					ListIterator<Vec2> j = vertices.listIterator();
					j.next();
					for (int l=0;l<vertices.size()-2;l++) {
						Vec2 v = j.next();
						if (v==v1 || v==v2 || v==v3)
							continue;
						if (isPointInTriangle(v1, v2, v3, v)) {
							goodEar = false;
							break;
						}
					}
				}

				if (goodEar) {
					// System.out.println ("Cutting ear: "+v1+","+v2+","+v3);
					triangles.push(v1);
					triangles.push(v2);
					triangles.push(v3);
					i.remove();
					earCut = true;
					break;
				}

				i.next();

			}

			if (!earCut) {
				// System.err.println("Can't find ear o_O");
				break;
			}
		}

		try{
			VertexBuffer vb = new VertexBuffer(VertexFormat.VF_POSITION, 2, triangles.size());
			for (Vec2 v: triangles) {
				vb.pos(v.getX(), v.getY());
				vb.next();
			}
			return vb;
		} catch (VertexFormatException e) {}
		return null;
	}

	public static VertexBuffer createHairlineLines(Vec2[] input) {
		VertexBuffer output = null;
		try {
			output = new VertexBuffer(VertexFormat.VF_POSITION, 2, input.length);
			for (Vec2 v:input) {
				output.pos(v.getX(), v.getY());
				output.next();
			}
		}
		catch (VertexFormatException e) 
		{
			System.err.println(e);
			return null;
		}
		return output;
	}

	public static VertexBuffer createSolidLines(Vec2[] input, PrimitiveType inputPrimitiveType, float width) {
		VertexBuffer output = null;
		int i;
		try {
			switch (inputPrimitiveType) {
			case LINE_LIST:
				output = new VertexBuffer(VertexFormat.VF_POSITION, 2, (input.length)*3);
				i = 0;
				while (i < input.length) {
					v6.copy(input[i]);
					i++;
					v7.copy(input[i]);
					i++;
					buildQuadPoints (v6.getX(), v6.getY(), v7.getX(), v7.getY(), width*0.5f);
					output.pos(quad[0].getX(), quad[0].getY()); output.next();
					output.pos(quad[1].getX(), quad[1].getY()); output.next();
					output.pos(quad[3].getX(), quad[3].getY()); output.next();
					output.pos(quad[3].getX(), quad[3].getY()); output.next();
					output.pos(quad[2].getX(), quad[2].getY()); output.next();
					output.pos(quad[0].getX(), quad[0].getY()); output.next();
				}
				return output;
			case LINE_LOOP:
				int length = input.length+1;
				output = new VertexBuffer(VertexFormat.VF_POSITION, 2, length*2);
				if (output != null) {
					if (input.length < 2)
						return null;

					Vec2 inner[] = new Vec2[length];
					Vec2 outer[] = new Vec2[length];

					i = 0;

					v6.copy(input[i]);
					i++;
					v7.copy(input[i]);
					i++;

					buildQuadPoints (v6.getX(), v6.getY(), v7.getX(), v7.getY(), width*0.5f);

					int cnt=0;

					p0.copy(quad[0]);
					p1.copy(quad[1]);
					p2.copy(quad[2]);
					p3.copy(quad[3]);

					buildQuadPoints (input[input.length-1].getX(), input[input.length-1].getY(), v6.getX(), v6.getY(), width*0.5f);

					try {
						inner[cnt] = lineIntersect(p0.getX(), p0.getY(), p2.getX(), p2.getY(), quad[0].getX(), quad[0].getY(), quad[2].getX(), quad[2].getY());
						outer[cnt] = lineIntersect(p1.getX(), p1.getY(), p3.getX(), p3.getY(), quad[1].getX(), quad[1].getY(), quad[3].getX(), quad[3].getY());
					} catch (ParallelLinesException e) {
						inner[cnt] = new Vec2(p0);
						outer[cnt] = new Vec2(p1);
					}

					cnt++;

					while (i < length) {
						v6.copy(v7);
						if (i == length-1)
							v7.copy(input[0]);
						else
							v7.copy(input[i]);
						i++;

						buildQuadPoints (v6.getX(), v6.getY(), v7.getX(), v7.getY(), width*0.5f);

						try {
							inner[cnt] = lineIntersect(p0.getX(), p0.getY(), p2.getX(), p2.getY(), quad[0].getX(), quad[0].getY(), quad[2].getX(), quad[2].getY());
							outer[cnt] = lineIntersect(p1.getX(), p1.getY(), p3.getX(), p3.getY(), quad[1].getX(), quad[1].getY(), quad[3].getX(), quad[3].getY());
						} catch (ParallelLinesException e) {
							inner[cnt] = new Vec2(p2);
							outer[cnt] = new Vec2(p3);
						}

						cnt++;

						if (i == length) {
							inner[cnt] = new Vec2(inner[0]);
							outer[cnt] = new Vec2(outer[0]);
						} else {
							p0.copy(quad[0]);
							p1.copy(quad[1]);
							p2.copy(quad[2]);
							p3.copy(quad[3]);
						}
					}

					for (int j=0; j<length; j++) {
						output.pos(outer[j].getX(), outer[j].getY()); output.next();
						output.pos(inner[j].getX(), inner[j].getY()); output.next();

					}
				}
				return output;
			case LINE_STRIP:
				output = new VertexBuffer(VertexFormat.VF_POSITION, 2, (input.length)*2);
				if (output != null) {
					if (input.length < 2)
						return null;

					Vec2 inner[] = new Vec2[input.length];
					Vec2 outer[] = new Vec2[input.length];

					i = 0;

					v6.copy(input[i]);
					i++;
					v7.copy(input[i]);
					i++;

					buildQuadPoints (v6.getX(), v6.getY(), v7.getX(), v7.getY(), width*0.5f);

					int cnt=0;

					inner[cnt] = new Vec2(quad[0]);
					outer[cnt] = new Vec2(quad[1]);
					cnt++;

					p0.copy(quad[0]);
					p1.copy(quad[1]);
					p2.copy(quad[2]);
					p3.copy(quad[3]);


					while (i < input.length) {
						v6.copy(v7);
						v7.copy(input[i++]);

						buildQuadPoints (v6.getX(), v6.getY(), v7.getX(), v7.getY(), width*0.5f);

						try {
							inner[cnt] = lineIntersect(p0.getX(), p0.getY(), p2.getX(), p2.getY(), quad[0].getX(), quad[0].getY(), quad[2].getX(), quad[2].getY());
							outer[cnt] = lineIntersect(p1.getX(), p1.getY(), p3.getX(), p3.getY(), quad[1].getX(), quad[1].getY(), quad[3].getX(), quad[3].getY());
						} catch (ParallelLinesException e) {
							inner[cnt] = new Vec2(p2);
							outer[cnt] = new Vec2(p3);
						}

						cnt++;

						if (i == 	input.length) {
							inner[cnt] = new Vec2(quad[2]);
							outer[cnt] = new Vec2(quad[3]);
						} else {
							p0.copy(quad[0]);
							p1.copy(quad[1]);
							p2.copy(quad[2]);
							p3.copy(quad[3]);
						}
					}

					for (int j=0; j<input.length; j++) {
						output.pos(outer[j].getX(), outer[j].getY()); output.next();
						output.pos(inner[j].getX(), inner[j].getY()); output.next();

					}
				}
				return output;
			default:
				return null;
			}
		} catch (VertexFormatException e) 
		{
			System.err.println(e);
			return null;
		}
	}
}