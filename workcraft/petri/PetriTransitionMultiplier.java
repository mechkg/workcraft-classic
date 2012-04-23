package workcraft.petri;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.python.core.Py;
import org.python.core.PyObject;

import workcraft.DuplicateIdException;
import workcraft.InvalidConnectionException;
import workcraft.Tool;
import workcraft.ToolType;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.editor.BasicEditable;
import workcraft.editor.Editor;
import workcraft.editor.EditorPane;
import workcraft.petri.PetriModel;

public class PetriTransitionMultiplier implements Tool {
	public static final String _modeluuid = "65f89260-641d-11db-bd13-0800200c9a66";
	public static final String _displayname = "Multiply selected transitions";

	public void run(Editor editor, WorkCraftServer server) {
		for (BasicEditable n : editor.getSelection()) {
			if (n instanceof EditablePetriTransition) {
				EditablePetriTransition t = (EditablePetriTransition)n;
				int number = 8;
				int offsetDir = 0;

				while (true) {
					try {
						number = Integer.parseInt(JOptionPane.showInputDialog(null, "Number of copies (1-20):", 8));
						if (number < 1) {
							JOptionPane.showMessageDialog(null, "Number truncated to 1", "Info", JOptionPane.INFORMATION_MESSAGE);
							number = 1;
						}
						if (number > 20) {
							JOptionPane.showMessageDialog(null, "Number truncated to 20", "Info",  JOptionPane.INFORMATION_MESSAGE);
							number = 20;
						}
						
						offsetDir = JOptionPane.showOptionDialog(null, "Which way to offset copies?", "Multiply transitions", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, 
																	new String[] {"Up", "Down", "Left", "Right"}, 0);
						if (offsetDir == JOptionPane.CLOSED_OPTION)
							offsetDir = 0;
												
						break;
					} catch (NumberFormatException e) 
					{	
						JOptionPane.showMessageDialog(null, "Invalid number", "Try again", JOptionPane.WARNING_MESSAGE);
					}
				}
				
				float dx = 0.0f, dy = 0.0f;
				float offsetx = 0.0f, offsety = 0.0f;
				
				
				switch (offsetDir) {
					case 0:
						dx = 0.0f; dy = -0.15f; break;
					case 1:
						dx = 0.0f; dy = 0.15f; break;
					case 2:
						dx = 0.15f; dy = 0.0f; break;
					case 3:
						dx = -0.15f; dy = 0.0f; break;
				}
				
				
				
				try {
				for (int i=0; i<number; i++) {
					EditablePetriTransition newt = new EditablePetriTransition(editor.getDocument().getRoot());
					newt.transform.copy(t.transform);
					
					offsetx += dx;
					offsety += dy;
					
					newt.transform.translateRel(offsetx, offsety, 0.0f);
					
					String oldName = t.getId();
					
					
					int num = 0;
					
					
					if (Character.isDigit((oldName.charAt(oldName.length()-1)))) {
						int j = oldName.length()-1;
						while (Character.isDigit((oldName.charAt(j))) && j > 0) j--;
						num = Integer.parseInt(oldName.substring(j+1));
						oldName = oldName.substring(0, j+1);
					}
					
					while (true) {
						try {
							newt.setId(oldName+Integer.toString(++num));
							break;
						} catch (DuplicateIdException e) {}
					}
					
					for (EditablePetriPlace p : t.getIn())
						editor.getDocument().createConnection(p, newt);
					for (EditablePetriPlace p : t.getOut())
						editor.getDocument().createConnection(newt, p);
				}
				} catch (UnsupportedComponentException e) {
					e.printStackTrace();					
				} catch (InvalidConnectionException e) {
					e.printStackTrace();
				}
			}
		}
		
		editor.refresh();
	}

	public void init(WorkCraftServer server) {
	}

	public boolean isModelSupported(UUID modelUuid) {
		return false;
	}

	public void deinit(WorkCraftServer server) {
		
	}

	public ToolType getToolType() {
		return ToolType.TRANSFORM;
	}
}
