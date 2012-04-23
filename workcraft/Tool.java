package workcraft;

import java.util.UUID;
import workcraft.editor.Editor;

public interface Tool {
	public void init(WorkCraftServer server);
	public void deinit(WorkCraftServer server);
	public void run(Editor editor, WorkCraftServer server);
	public ToolType getToolType();
	public boolean isModelSupported(UUID modelUuid);
}