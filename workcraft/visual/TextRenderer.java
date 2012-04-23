package workcraft.visual;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.GLUT;

public class TextRenderer {
	public class Font {
		private ByteBuffer alpha;
		private int tex_w;
		private int tex_h;
		private float descent;
		private int tex_name;
		
		boolean tex_loaded = false;

		private HashMap<Integer, float[]> char_map = new HashMap<Integer, float[]>();

		public Font(ByteBuffer alpha, float descent, int tex_w, int tex_h) {
			this.alpha = alpha;
			this.descent = descent;
			this.tex_w = tex_w;
			this.tex_h = tex_h;
		}

		public void loadTexture(GL gl) {
			int names[] = new int[1];
			gl.glGetError();
			gl.glGenTextures(1, names, 0);
			tex_name = names[0];
			gl.glBindTexture(GL.GL_TEXTURE_2D, tex_name);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_GENERATE_MIPMAP, GL.GL_TRUE);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
			alpha.rewind();
			gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_ALPHA8, tex_w, tex_h, 0, GL.GL_ALPHA, GL.GL_UNSIGNED_BYTE, alpha);
			
			int error = gl.glGetError();
			if (error != GL.GL_NO_ERROR)
				System.err.println("GL error "+error);
		}

		public void bindTexture(GL gl) {
			gl.glBindTexture(GL.GL_TEXTURE_2D, tex_name);
		
		}

		public void specifyChar(int code, float[] params) {
			char_map.put(code, params);
		}

		public float getStringWidth (String s, float height) {
			float offset = 0.0f;

			for (int i=0; i<s.length(); i++) {
				float [] params = char_map.get((int)s.charAt(i));

				float a, b, c;

				if (params!=null) {
					a = params[4]; b = params[5]; c = params[6];
				}
				else {
					a = 0.0f; b = 1.0f; c = 0.0f;
				}
				
				if (i!=0)
					offset += a;
				
				offset += b;
				
				if (i!=s.length()-1)
					offset += c;
			}
			
			return offset * height;
		}
		
		public void drawString(String s, JOGLPainter p) {
			GL gl = p.getGl();
			if (!tex_loaded) {
				loadTexture(gl);
				tex_loaded = true;
			}	
			
			VertexBuffer text;
			try {
				text = new VertexBuffer(VertexFormat.VF_POSITION|VertexFormat.VF_TEXCOORD, 2, 6*s.length());
			} catch (VertexFormatException e) {
				System.out.println(e);
				return;
			}

			float offset = 0.0f;

			for (int i=0; i<s.length(); i++) {
				float [] params = char_map.get((int)s.charAt(i));

				float su, sv, eu, ev, a, b, c;

				if (params!=null) {
					su = params[0]; sv=params[1]; eu=params[2]; ev=params[3];
					a = params[4]; b = params[5]; c = params[6];
				}
				else {
					su = 0.0f; sv=0.0f; eu=0.0f; ev=0.0f;
					a = 0.0f; b = 1.0f; c = 0.0f;
				}
				
				if (i!=0)
					offset += a;
				text.pos(offset, 0.0f - descent); text.texcoord(su, ev); text.next();
				text.pos(offset+b, 1.0f - descent); text.texcoord(eu, sv); text.next();
				text.pos(offset, 1.0f - descent); text.texcoord(su, sv); text.next();
				text.pos(offset, 0.0f - descent); text.texcoord(su, ev); text.next();
				text.pos(offset+b, 0.0f - descent); text.texcoord(eu, ev); text.next();
				text.pos(offset+b, 1.0f - descent); text.texcoord(eu, sv); text.next();
				offset += b+c;
			}
			
			gl.glEnable(GL.GL_TEXTURE_2D);
			bindTexture(gl);
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			p.drawPrimitives(text, PrimitiveType.TRIANGLE_LIST);
			gl.glDisable(GL.GL_BLEND);
			gl.glDisable(GL.GL_TEXTURE_2D);
		}
	}

	private HashMap<String, Font> fonts = new HashMap<String, Font>();

	public void drawString(String s, JOGLPainter p, String font_face) {
		Font font = fonts.get(font_face);
		if (font==null)
		{
			font = fonts.get("_default");
			if (font==null) {
				System.err.println ("Can't draw string: no fonts loaded!");
				return;
			}
		}
		font.drawString(s, p);
	}
	
	public float getStringWidth( String s, float height, String font_face) {
		Font font = fonts.get(font_face);
		if (font==null)
		{
			font = fonts.get("_default");
			if (font==null) {
				System.err.println ("Can't draw string: no fonts loaded!");
				return 0.0f;
			}
		}
		return font.getStringWidth(s, height);
	}

	public float getStringWidth( String s, float height) {
		return getStringWidth (s, height, null);	
	}
	
	public void loadFonts(String directory) {
		File dir = new File(directory);

		if (!dir.exists() || !dir.isDirectory()) {
			System.err.println("Invalid font directory: " + directory);
			return;
		}
		
		System.out.println("Loading fonts from "+directory+"...");

		File files[] = dir.listFiles(new FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory())
					return false;
				return f.getPath().endsWith("xml");
			}
		});

		boolean first = true;

		for (File f : files) {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			Document doc;
			DocumentBuilder db;

			try {
				db = dbf.newDocumentBuilder();
				doc = db.parse(f);
			} catch (ParserConfigurationException e) {
				System.err.println(e.getMessage());
				continue;
			} catch (IOException e) {
				System.err.println(e.getMessage());
				continue;
			} catch (SAXException e) {
				System.err.println(e.getMessage());
				continue;
			}

			Element root = doc.getDocumentElement();

			if (root.getTagName() != "font")
				continue;

			File image_file = new File(directory+File.separator+root.getAttribute("texture"));
			String font_face = root.getAttribute("face");
			boolean inverse = Boolean.parseBoolean(root.getAttribute("invert"));
			
			System.out.print("\t"+font_face);

			BufferedImage image;

			try {
				image = ImageIO.read(image_file);
			} catch (IOException e) {
				System.err.println(e.toString()+" ("+image_file.getPath()+")");
				continue;
			}

			int h = image.getHeight();
			int w = image.getWidth();
			int rgb[] = new int[3];

			ByteBuffer data = BufferUtil.newByteBuffer(w * h);
			
			WritableRaster raster =  image.getRaster();

			System.out.print("...reading font texture...");
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++) {
				//	System.out.println(x+" "+y);
					raster.getPixel(x, y, rgb);
					if (inverse)
						data.put((byte) ( 255 - (rgb[0] + rgb[1] + rgb[2]) / 3));
					else
						data.put((byte) ( (rgb[0] + rgb[1] + rgb[2]) / 3));
				}
			
			
			System.out.println("OK");

			NodeList nl = root.getElementsByTagName("char");

			Font font = new Font(data, Float.parseFloat(root.getAttribute("descent")), w, h);
			
			for (int i = 0; i < nl.getLength(); i++) {
				
				Element e = (Element) nl.item(i);
				float[] params = new float[7];
				params[0] = Float.parseFloat(e.getAttribute("su"));
				params[1] = Float.parseFloat(e.getAttribute("sv"));
				params[2] = Float.parseFloat(e.getAttribute("eu"));
				params[3] = Float.parseFloat(e.getAttribute("ev"));
				params[4] = Float.parseFloat(e.getAttribute("a"));
				params[5] = Float.parseFloat(e.getAttribute("b"));
				params[6] = Float.parseFloat(e.getAttribute("c"));
				int code = Integer.parseInt(e.getAttribute("code"));
				font.specifyChar(code, params);
			}

			if (first) {
				fonts.put("_default", font);
				first = false;
			}

			fonts.put(font_face, font);
		}
	}
}
