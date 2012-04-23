package workcraft.petri;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.common.ExternalProcess;
import workcraft.editor.Editor;

public class PetriKiller implements Tool {
	public static final String _modeluuid = "65f89260-641d-11db-bd13-0800200c9a66";
	public static final String _displayname = "Count states (Petri Killer)";

	public boolean isModelSupported(UUID modelUuid) {
		// TODO Auto-generated method stub
		return true;
	}

	public void init(WorkCraftServer server) {
		// TODO Auto-generated method stub
	}

	public void run(Editor editor, WorkCraftServer server) {
		PetriDotGSaver saver = new PetriDotGSaver();
		
		JFrame frame = (JFrame)server.python.get("_main_frame", JFrame.class);
		ExternalProcess p = new ExternalProcess(frame);
		
		try {
			saver.writeFile("tmp/_pk_.g", (PetriModel)editor.getDocument());
			p.run(new String[] {"util/pk", "tmp/_pk_.g"}, ".", "Analysis report", true);
			File f = new File("tmp/_pk_.g");
			f.delete();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	public void deinit(WorkCraftServer server) {
		// TODO Auto-generated method stub
		
	}

	public ToolType getToolType() {
		return ToolType.GENERAL;
	}
}
