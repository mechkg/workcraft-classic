package workcraft.visual;
import java.util.Arrays;
import java.util.List;

import workcraft.editor.BasicEditable;
import workcraft.util.*;
import javax.media.opengl.GL;

public class Grid implements Drawable {
	private Vec2[] gridlines;
	private float major_interval = 1.0f;
	private float minor_interval = 0.1f;
	private Colorf minor_color;
	private Colorf major_color;
	private Colorf highlight_color;
	private Vec2 visible_lower_left;
	private Vec2 visible_upper_right;
	private boolean gridlines_dirty;
	private int total_lines;
	
	private int minor_vertices, major_vertices;

	private boolean draw_highlight;
	private float hl_x, hl_y;
	
	private float[] x = null, y = null;
	
	public Grid() {
		minor_color = new Colorf (1.0f, 1.0f, 1.0f, 1.0f);
		major_color = new Colorf (1.0f, 0.0f, 0.0f, 1.0f);
		highlight_color = new Colorf (0.0f, 0.0f, 0.5f, 1.0f);
		visible_lower_left = new Vec2(-1.0f, -1.0f);
		visible_upper_right = new Vec2(1.0f, 1.0f);
		total_lines = 0;
		draw_highlight = false;
		hl_x = 0;
		hl_y = 0;
		gridlines_dirty = true;
		rebuild();
	}
	
	public void setMajorInterval (float interval) {
		if (interval > 0.0f)
			major_interval = interval;
		gridlines_dirty = true;
	}

	public void setMinorInterval (float interval) {
		if (interval > 0.0f)
			minor_interval = interval;
		gridlines_dirty = true;
	}
	
	public void setVisibleRange (Vec2 lower_left, Vec2 upper_right) {
		visible_lower_left.copy(lower_left);
		visible_upper_right.copy(upper_right);
		gridlines_dirty = true;
	}

	public void rebuild() {
		if (!gridlines_dirty)
			return;
			
		int minor_left = (int)Math.ceil(visible_lower_left.getX()/minor_interval);
		int minor_right = (int)Math.floor(visible_upper_right.getX()/minor_interval);
		int minor_lines_hor = minor_right - minor_left + 1; 

		int minor_top = (int)Math.floor(visible_upper_right.getY()/minor_interval);
		int minor_bottom = (int)Math.ceil(visible_lower_left.getY()/minor_interval);
		int minor_lines_ver = minor_top - minor_bottom + 1; 

		int major_left = (int)Math.ceil(visible_lower_left.getX()/major_interval);
		int major_right = (int)Math.floor(visible_upper_right.getX()/major_interval);
		int major_lines_hor = major_right - major_left + 1; 

		int major_top = (int)Math.floor(visible_upper_right.getY()/major_interval);
		int major_bottom = (int)Math.ceil(visible_lower_left.getY()/major_interval);
		int major_lines_ver = major_top - major_bottom + 1;
		
		if (
				minor_lines_hor < 0 ||
				minor_lines_ver < 0 ||
				major_lines_hor < 0 ||
				major_lines_ver < 0
			) {
			System.err.println ("Error while rebuilding grid (invalid visible area specified)");
			gridlines = null;
			return;
		}
		
		total_lines = minor_lines_hor+major_lines_hor+minor_lines_ver+major_lines_ver;
		
		int offset = 0;
		gridlines = new Vec2[total_lines*2+4];
		
		for (int x=minor_left; x<=minor_right; x++) {
			gridlines[offset++] = new Vec2((float)x*minor_interval, visible_upper_right.getY());
			gridlines[offset++] = new Vec2((float)x*minor_interval, visible_lower_left.getY());
		}

		for (int y=minor_bottom; y<=minor_top; y++) {
			gridlines[offset++] = new Vec2(visible_lower_left.getX(), (float)y*minor_interval);
			gridlines[offset++] = new Vec2(visible_upper_right.getX(), (float)y*minor_interval); 
		}
		
		minor_vertices = offset;
		

		for (int x=major_left; x<=major_right; x++) {
			gridlines[offset++] = new Vec2((float)x*major_interval, visible_upper_right.getY());
			gridlines[offset++] = new Vec2((float)x*major_interval, visible_lower_left.getY());
		}

		for (int y=major_bottom; y<=major_top; y++) {
			gridlines[offset++] = new Vec2(visible_lower_left.getX(), (float)y*major_interval);
			gridlines[offset++] = new Vec2(visible_upper_right.getX(), (float)y*major_interval);
		}
		
		major_vertices = offset - minor_vertices;
		
		updateHighlightLines();
		
		gridlines_dirty = false;
	}
	
	public void draw(Painter p) {
		rebuild();

		if (gridlines!=null)
		{
			p.setLineMode(LineMode.HAIRLINE);
			p.setLineColor(minor_color);
			p.drawLines(gridlines, 0, minor_vertices);
			p.setLineColor(major_color);
			p.drawLines(gridlines, minor_vertices, major_vertices);
			
			if (draw_highlight) {
				p.setLineColor(highlight_color);
				p.drawLines(gridlines, total_lines*2, 4);
			}
		}
	}

	public void setMinorColor(Colorf minor_color) {
		this.minor_color.copy(minor_color);
	}

	public Colorf getMinorColor() {
		return new Colorf(minor_color);
	}

	public void setMajorColor(Colorf major_color) {
		this.major_color.copy(major_color);
	}

	public Colorf getHighlightColor() {
		return new Colorf(highlight_color);
	}
	
	private void updateHighlightLines() {
		int offset = total_lines *2;
		gridlines[offset++] = new Vec2(hl_x, visible_upper_right.getY()); 
		gridlines[offset++] = new Vec2(hl_x, visible_lower_left.getY());
		gridlines[offset++] = new Vec2(visible_lower_left.getX(), hl_y); 
		gridlines[offset++] = new Vec2(visible_upper_right.getX(), hl_y);
	}
	
	public void setHighlightColor(Colorf highlight_color) {
		this.highlight_color.copy(highlight_color);
		updateHighlightLines();
	}
	
	public Vec2 getClosestGridPoint(Vec2 v)
	{
		hl_x = getClosestPoint(v.getX(), x);
		hl_y = getClosestPoint(v.getY(), y);
		updateHighlightLines();
		return new Vec2(hl_x, hl_y);	
	}
	
	private float getClosestPoint(float x, float[] a)
	{
		float best = minor_interval * Math.round(x / minor_interval);
	
		if (a == null)
			return best;
		
		int L = -1, R = a.length;

		while(R != L + 1)
		{
			int k = (L + R) / 2;
			if (a[k] < x) L = k; else R = k;
		}
		
		if (L != -1) if (Math.abs(a[L] - x) < Math.abs(best - x)) best = a[L];
		if (L + 1 < a.length) if (Math.abs(a[L + 1] - x) < Math.abs(best - x)) best = a[L + 1];
		
		return best;
	}

	public void highlightEnable(boolean enable) {
		draw_highlight = enable;
	}
	
	public void updateGuidelines(List<BasicEditable> nodes) {
		x = new float[nodes.size()]; y = new float[nodes.size()];
		int cnt = 0;
		for (BasicEditable n : nodes) {
			Vec2 v = n.transform.LocalToView(new Vec2(0.0f, 0.0f));
			x[cnt] = v.getX();
			y[cnt] = v.getY();
			cnt++;
		}
		Arrays.sort(x);
		Arrays.sort(y);
	}
}