package workcraft.stg;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.swing.JOptionPane;

import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.editor.Editor;
import workcraft.petri.EditablePetriTransition;
import workcraft.util.Vec2;

public class STGAddNumbers implements Tool{
	public static final String _modeluuid = "10418180-D733-11DC-A679-A32656D89593";
	public static final String _displayname = "Add numbers";
	
	private static STGRemoveNumbers numrem;  	
	
	class TransPosComparator implements Comparator<EditablePetriTransition> {

		public int compare(EditablePetriTransition arg0, EditablePetriTransition arg1) {
				Vec2 v0 = arg0.transform.getTranslation2d();
				Vec2 v1 = arg1.transform.getTranslation2d();
				
				if ((int)(v0.getY()*4-v1.getY()*4)==0)
					return (int)(v1.getX()*80-v0.getX()*80);
				
				return (int)(v0.getY()*4-v1.getY()*4);
		}
				

	}
	
	public void addNumbers(STGModel model) {
		// first, remove all numbered transition labels from the diagram
		numrem.removeNumbers(model);

		
		LinkedList<EditablePetriTransition> tlst = new LinkedList<EditablePetriTransition>();
		model.getTransitions(tlst);

		
		TransPosComparator comp = new TransPosComparator();
		
		Collections.sort(tlst, comp);
		
		// need to sort list vertically, then horizontally 
		Map <String, Integer> trnum = new HashMap<String, Integer>();
		Set <String> numbered = new HashSet<String>();
		
		
		for (EditablePetriTransition t : tlst) {
			if (trnum.containsKey(t.getLabel())&&!t.getLabel().equals("")) {
				numbered.add(t.getLabel());
			}
			trnum.put(t.getLabel(), 0);
		}
		
		for (EditablePetriTransition t : tlst) {
			if (numbered.contains(t.getLabel())) {
				trnum.put(t.getLabel(), trnum.get(t.getLabel())+1);
				t.setLabel(t.getLabel()+"/"+trnum.get(t.getLabel()));
			}
		}
		
		
	}



	public void deinit(WorkCraftServer server) {
		// TODO Auto-generated method stub
		
	}


	public ToolType getToolType() {
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
		STGModel model = (STGModel) (editor.getDocument());
		
		numrem = (STGRemoveNumbers)server.getToolInstance(STGRemoveNumbers.class);
		
		if (numrem == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net .g export tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		addNumbers(model);
		
	}
}
