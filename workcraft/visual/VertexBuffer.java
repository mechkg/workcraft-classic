package workcraft.visual;

import java.nio.FloatBuffer;
import javax.media.opengl.*;
import com.sun.opengl.util.*;
import workcraft.util.*;

public class VertexBuffer {
	protected float _x = 0.0f, _y = 0.0f;
	protected float _a = 1.0f, _r = 1.0f, _g = 1.0f, _b = 1.0f;
	protected float _u = 0.0f, _v = 0.0f;
	
	protected FloatBuffer buffer;

	protected int vertexFormat;
	protected int vertexCount;
	protected int vertexPositionSize;
	protected int vertexSizeInFloats;
	
	protected boolean pos = false; 
	protected boolean col = false;
	protected boolean tex = false;
	
	public FloatBuffer getBuffer () {
		return buffer;
	}
	
	public VertexBuffer (int vertex_format, int vertex_pos_size, int vertex_count) throws VertexFormatException {
		vertexSizeInFloats = 0;
		this.vertexFormat = vertex_format;
		if ( (vertex_format & VertexFormat.VF_POSITION ) != 0)
		{
			if (vertex_pos_size < 2 && vertex_pos_size >3 )
				throw new VertexFormatException("Vertex position must be either 2D or 3D");
			vertexSizeInFloats += vertex_pos_size;
			this.vertexPositionSize = vertex_pos_size;
		}
		else 
			throw new VertexFormatException("Vertex format must at least include VF_POSITION");
		
		if ( ( vertex_format & VertexFormat.VF_COLOR) != 0) {
			vertexSizeInFloats += 4;
		}

		if ( ( vertex_format & VertexFormat.VF_TEXCOORD) != 0) {
			vertexSizeInFloats += 2;
		}
		
		buffer = BufferUtil.newFloatBuffer(vertexSizeInFloats * vertex_count);
		
		this.vertexCount = vertex_count;
	}
	
	public int getVertexCount() {
		return vertexCount;
	}
	
	public void setBufferPosition(int position_in_vertices) {
		buffer.position(position_in_vertices*vertexSizeInFloats);
	}
	
	public void pos (float x, float y) {
		_x = x;
		_y = y;
		pos = true;
	}
	
	public void color(Colorf color) {
		_a = color.a;	_r = color.r; _g = color.g; _b = color.b;
		col = true;
	}
	
	public void texcoord(float u, float v) {
		_u = u;
		_v = v;
		tex = true;
	}
	
	public void readPos(Vec2 out) {
		if ((vertexFormat & VertexFormat.VF_POSITION) != 0 ){
			out.setXY(buffer.get(), buffer.get());
			buffer.position(buffer.position()-2);
		}
	}
			
	public void next () {
		// TODO: resize buffer if needed
		
		if ( (vertexFormat & VertexFormat.VF_POSITION) != 0) {
			if (pos) {
				buffer.put(_x);
				buffer.put(_y);
			} else {
				buffer.position(buffer.position()+2);
			}
		}
		
		if ( (vertexFormat & VertexFormat.VF_COLOR) != 0) {
			if (col) {
				buffer.put(_r);
				buffer.put(_g);
				buffer.put(_b);
				buffer.put(_a);
			} else {
				buffer.position(buffer.position()+4);
			}
		} 
		if ( (vertexFormat & VertexFormat.VF_TEXCOORD) != 0) {
			if (tex) {
				buffer.put(_u);
				buffer.put(_v);
			} else {
				buffer.position(buffer.position()+2);
			}
		}
		
		tex = col = pos = false;
	}
}