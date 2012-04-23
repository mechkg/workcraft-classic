package workcraft.sdfs;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import workcraft.ModelBase;
import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.abstractsdfs.ASDFS2MGMapper;
import workcraft.common.ExternalProcess;
import workcraft.common.PetriNetMapper;
import workcraft.counterflow.CFModel;
import workcraft.editor.Editor;
import workcraft.mg.MGModel;
import workcraft.mg.MGPlace;
import workcraft.mg.MGTransition;
import workcraft.mg.MGConnection;
import workcraft.spreadtoken.STModel;

public class SDFSCriticalCycleAnalyzer implements Tool
{
	public static final String _modeluuid = "*";
	public static final String _displayname = "Critical cycle search";

	public boolean isModelSupported(UUID modelUuid)
	{
		if (modelUuid.compareTo(STModel._modeluuid)==0)
			return true;
//		if (modelUuid.compareTo(CFModel._modeluuid)==0)
		//return true;
		return false;
	}

	public void init(WorkCraftServer server)
	{
		// TODO Auto-generated method stub
	}

	public void run(Editor editor, WorkCraftServer server)
	{
		ASDFSMapper asdfsmapper = (ASDFSMapper)server.getToolInstance(ASDFSMapper.class);
		if (asdfsmapper == null) {
			JOptionPane.showMessageDialog(null, "This tool requires ASDFS Mapper tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		ASDFS2MGMapper mgmapper = (ASDFS2MGMapper)server.getToolInstance(ASDFS2MGMapper.class);
		if (asdfsmapper == null) {
			JOptionPane.showMessageDialog(null, "This tool requires MG Mapper tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}	

		JFrame frame = (JFrame)server.python.get("_main_frame", JFrame.class);
		SDFSCriticalCycleAnalyzerFrame win = new SDFSCriticalCycleAnalyzerFrame(frame);

		String s = "";

		MGModel doc = mgmapper.map(server, asdfsmapper.map(server, editor.getDocument()));

		boolean live = doc.isLive(); 

		s+="Underlying marked graph properties (to be able to see the graph, please use MG mapper tool instead):\n";

		if (live)
		{
			s += "The marked graph is live.\n";
			if (doc.isSafe())
			{
				s += "The marked graph is safe.\n";
			}
			else
			{
				s += "The marked graph is unsafe.\n";
			}
		}
		else
		{
			s += "The marked graph is not live.\n";
		}	

		s += "\n";

		win.doc = doc;

		if (live)
		{
			editor.getDocument().clearHighlights();
			LinkedList<MGPlace> cp = new LinkedList<MGPlace>();
			LinkedList<MGTransition> ct = new LinkedList<MGTransition>();

			doc.calculateCriticalCycle(cp, ct);
			((ModelBase)editor.getDocument()).setShowHighlight(true);

			double delay = 0.0;
			int tokens = 0;

			for(int i = 0; i < ct.size(); i++)
			{
				delay += cp.get(i).getDelayMean();
				tokens += cp.get(i).getTokens();
				
				((SDFSNode)server.getObjectById(ct.get(i).getLabel())).highlight = true;
				
				for (String l_id : cp.get(i).getLabel().split("/")[1].split(","))
					((SDFSNode)server.getObjectById(l_id)).highlight = true;
				
			}
			s += "The mean critical cycle delay = " + delay + "\n";
			s += "The critical cycle contains " + tokens + " token(s)\n";

			s += "The critical cycle delay/token ratio = " + delay / tokens + "\n";
			// doc.updateConnectionsColor();
		}
		else
		{
			((ModelBase)editor.getDocument()).setShowHighlight(false);		
		}

		frame.repaint();
		win.getTxtReport().setText(s);


		win.go();
	}

	public void deinit(WorkCraftServer server) {
		// TODO Auto-generated method stub

	}

	public ToolType getToolType() {
		return ToolType.GENERAL;
	}
}
