package workcraft.stg;

import java.io.IOException;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.common.ExternalProcess;
import workcraft.editor.Editor;

public class STGPetrify implements Tool{
	public static final String _modeluuid = "10418180-D733-11DC-A679-A32656D89593";
	public static final String _displayname = "Petrify";


	void petrify(Editor editor, WorkCraftServer server) {
		STGModel model = (STGModel) (editor.getDocument());
		STGModel new_model = new STGModel();
		
		STGDotGSaver saver = (STGDotGSaver)server.getToolInstance(STGDotGSaver.class);
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net .g export tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		STGDotGLoader loader = (STGDotGLoader)server.getToolInstance(STGDotGLoader.class);
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net .g import tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		try {
			saver.writeFile("tmp/_net_.g", model);

			JFrame frame = (JFrame)server.python.get("_main_frame", JFrame.class);
			ExternalProcess p = new ExternalProcess(frame);

			p.run(new String[] {"util/petrify", "-o", "tmp/_new_net_.g", "tmp/_net_.g"}, ".", "Petrify", true);
			// 
			// check whether there was a problem during petrify process
			if (loader.readFile("tmp/_new_net_.g", new_model)) {
				editor.setDocument(new_model);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

//		File f = new File("tmp/_net_.g");
		//f.delete();
//		f = new File("tmp/_new_net_.g");
		//f.delete();
		
		
		
	}


	public void deinit(WorkCraftServer server) {
		// TODO Auto-generated method stub
		
	}


	public ToolType getToolType() {
		// TODO Auto-generated method stub
		return ToolType.TRANSFORM;
	}


	public void init(WorkCraftServer server) {
		// TODO Auto-generated method stub
		
	}


	public boolean isModelSupported(UUID modelUuid) {
		// TODO Auto-generated method stub
		return false;
	}


	public void run(Editor editor, WorkCraftServer server) {
		petrify(editor, server);
	}


}
