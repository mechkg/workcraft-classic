package workcraft.editor;
import java.util.List;
import workcraft.util.Vec2;
import workcraft.visual.Drawable;

public abstract class EditableNode implements PropertyEditable, Drawable  {
	public abstract BoundingBox getBoundingBoxInViewSpace();
	public abstract boolean hits(Vec2 pointInViewSpace);
	public abstract boolean hitsBB(Vec2 pointInViewSpace);
}