package workcraft.mg;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.common.ExternalProcess;
import workcraft.editor.Editor;

public class MGAnalyser implements Tool
{
	public static final String _modeluuid = "4fbdb830-dba3-11db-8314-0800200c9a66";
	public static final String _displayname = "Marked Graph Properties";

	public boolean isModelSupported(UUID modelUuid)
	{
		// TODO Auto-generated method stub
		return true;
	}

	public void init(WorkCraftServer server)
	{
		// TODO Auto-generated method stub
	}

	public void run(Editor editor, WorkCraftServer server)
	{
		JFrame frame = (JFrame)server.python.get("_main_frame", JFrame.class);
		MGAnalyserFrame win = new MGAnalyserFrame(frame);
		
		String s = "";
		
		MGModel doc = (MGModel)editor.getDocument();
		
		s += "Number of places: " + doc.places.size() + "\n";
		s += "Number of transitions: " + doc.transitions.size() + "\n\n";
		
		boolean live = doc.isLive(); 
		
		if (live)
		{
			s += "Marked graph is live.\n";
			if (doc.isSafe())
			{
				s += "Marked graph is safe.\n";
			}
			else
			{
				s += "Marked graph is unsafe.\n";
			}
		}
		else
		{
			s += "Marked graph is not live.\n";
		}	
		
		s += "\n";
		
		win.getChkPlaces().setSelected(MGPlace.show_critical);
		win.getChkTransitions().setSelected(MGTransition.show_critical);
		win.getChkConnections().setSelected(MGConnection.show_critical);
		win.doc = doc;
		
		if (live)
		{
			win.getChkPlaces().setEnabled(true);
			win.getChkTransitions().setEnabled(true);
			win.getChkConnections().setEnabled(true);
			
			LinkedList<MGPlace> cp = new LinkedList<MGPlace>();
			LinkedList<MGTransition> ct = new LinkedList<MGTransition>();
			
			doc.calculateCriticalCycle(cp, ct);
			
			double delay = 0.0;
			int tokens = 0;
			
			for(int i = 0; i < ct.size(); i++)
			{
				delay += cp.get(i).getDelayMean();
				tokens += cp.get(i).getTokens();
				ct.get(i).critical = true;
				cp.get(i).critical = true;
				((MGConnection)cp.get(i).connections.getFirst()).critical = true;
				((MGConnection)cp.get(i).connections.getLast()).critical = true;
			}
			s += "The mean critical cycle delay = " + delay + "\n";
			s += "The critical cycle contains " + tokens + " token(s)\n";
			
			s += "The critical cycle delay/token ratio = " + delay / tokens + "\n";
			doc.updateConnectionsColor();
		}
		else
		{
			win.getChkPlaces().setEnabled(false);
			win.getChkTransitions().setEnabled(false);
			win.getChkConnections().setEnabled(false);
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
