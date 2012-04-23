package workcraft.petri;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.common.DefaultSimControls;
import workcraft.common.ExternalProcess;
import workcraft.common.MPSATOutputParser;
import workcraft.editor.Editor;

public class PetriMPSAT implements Tool {
	public static final String _modeluuid = "65f89260-641d-11db-bd13-0800200c9a66";
	public static final String _displayname = "Check for deadlocks (PUNF/MPSAT)";

	public boolean isModelSupported(UUID modelUuid) {
		// TODO Auto-generated method stub
		return true;
	}

	public void init(WorkCraftServer server) {
		// TODO Auto-generated method stub
	}
	
	public void run(Editor editor, WorkCraftServer server) {
		PetriDotGSaver saver = (PetriDotGSaver)server.getToolInstance(PetriDotGSaver.class);
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net .g export tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		ReadArcsComplexityReduction rd =(ReadArcsComplexityReduction)server.getToolInstance(ReadArcsComplexityReduction.class);  
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net read arcs complexity reduction tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		JFrame frame = (JFrame)server.python.get("_main_frame", JFrame.class);
		ExternalProcess p = new ExternalProcess(frame);
		
		try {
			saver.writeFile("tmp/_net_.g", rd.reduce((PetriModel)editor.getDocument()));
			p.run(new String[] {"util/punf", "-s", "-t", "-p", "tmp/_net_.g"}, ".", "Unfolding report", true);
			p.run(new String[] {"util/mpsat", "-D", "tmp/_net_.mci"}, ".", "Model-checking report", true);
			
			String badTrace =MPSATOutputParser.parsePetriNetTrace(p.getOutput());
			
			if (badTrace != null)
				if (JOptionPane.showConfirmDialog(null, "The system has a deadlock. Do you wish to load the event trace that leads to the deadlock?", "Confirm", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
						((DefaultSimControls)editor.getSimControls()).setTrace(badTrace);
			
			File f = new File("tmp/_net_.g");
			f.delete();
			f = new File("tmp/_net_.mci");
			f.delete();
			
		} catch (IOException e) {
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
