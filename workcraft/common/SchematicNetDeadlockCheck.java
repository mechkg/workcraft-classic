package workcraft.common;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.counterflow.CFModel;
import workcraft.editor.Editor;
import workcraft.gate.GateModel;
import workcraft.petri.PetriDotGSaver;
import workcraft.petri.PetriModel;
import workcraft.petri.ReadArcsComplexityReduction;
import workcraft.spreadtoken.STModel;

public class SchematicNetDeadlockCheck implements Tool {
	public static final String _modeluuid = "*";
	public static final String _displayname = "Check for deadlocks (PUNF/MPSAT)";

	public boolean isModelSupported(UUID modelUuid) {
		if (modelUuid.compareTo(STModel._modeluuid)==0)
			return true;
		if (modelUuid.compareTo(CFModel._modeluuid)==0)
			return true;
		if (modelUuid.compareTo(GateModel._modeluuid)==0)
			return true;
		return false;
	}

	public void init(WorkCraftServer server) {
		// TODO Auto-generated method stub
	}

	public void run(Editor editor, WorkCraftServer server) {
		PetriNetMapper mapper = (PetriNetMapper)server.getToolInstance(PetriNetMapper.class);
		if (mapper == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net Mapper tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

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
			saver.writeFile("tmp/_net_.g", rd.reduce(mapper.map(server, editor.getDocument())));
			p.run(new String[] {"util/punf", "-s", "-t", "-p", "tmp/_net_.g"}, ".", "Unfolding report", true);
			p.run(new String[] {"util/mpsat", "-D", "-f", "tmp/_net_.mci"}, ".", "Model-checking report", true);
			File f = new File("tmp/_net_.g");
			f.delete();
			f = new File("tmp/_net_.mci");
			f.delete();

			String badTrace = MPSATOutputParser.parseSchematicNetTrace(p.getOutput());

			if (badTrace != null)
				if (JOptionPane.showConfirmDialog(null, "The system has a deadlock. Do you wish to load the event trace that leads to the deadlock?", "Confirm", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
					((DefaultSimControls)editor.getSimControls()).setTrace(badTrace);


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
