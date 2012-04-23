package workcraft.stg;

import java.util.LinkedList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.editor.Editor;
import workcraft.petri.EditablePetriTransition;

public class STGRemoveNumbers implements Tool{
	public static final String _modeluuid = "10418180-D733-11DC-A679-A32656D89593";
	public static final String _displayname = "Remove numbers";



	
	public void removeNumbers(STGModel model) {
		// remove all numbered transition labels from the diagram
		LinkedList<EditablePetriTransition> tlst = new LinkedList<EditablePetriTransition>();
		model.getTransitions(tlst);
		
		Pattern p = Pattern.compile("(.*)\\/[0-9]*");
		Matcher m;
		
		for (EditablePetriTransition t : tlst) {
			m = p.matcher(t.getLabel());
			if (m.find()) {
				t.setLabel(m.group(1));
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
		removeNumbers(model);
		
	}

}
