package workcraft.visual;

import workcraft.editor.BasicEditable;
// be qwe xru
public interface AnimationListener {
	public void animationStarted(BasicEditable n);
	public void animationEnded(BasicEditable n);
	public boolean isAnimationFinished();
}