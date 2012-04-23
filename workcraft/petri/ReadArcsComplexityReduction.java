package workcraft.petri;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import javax.swing.JOptionPane;

import workcraft.InvalidConnectionException;
import workcraft.Model;
import workcraft.ModelBase;
import workcraft.Tool;
import workcraft.ToolType;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.common.DefaultConnection;
import workcraft.DuplicateIdException;
import workcraft.editor.EditableConnection;
import workcraft.editor.Editor;
import workcraft.editor.EditorPane;
import workcraft.logic.InvalidExpressionException;
import workcraft.petri.EditablePetriPlace;
import workcraft.petri.EditablePetriTransition;
import workcraft.petri.PetriModel;
import workcraft.util.Vec2;

public class ReadArcsComplexityReduction implements Tool {

	public static final String _modeluuid = "65f89260-641d-11db-bd13-0800200c9a66";
	public static final String _displayname = "Reduce read arcs complexity";

	public PetriModel reduce (PetriModel source) {
		PetriModel newdoc = new PetriModel();
		try
		{

			Vector<EditablePetriPlace> ps = new Vector<EditablePetriPlace>();
			Vector<EditablePetriTransition> ts = new Vector<EditablePetriTransition>();
			Vector<EditableConnection> cs = new Vector<EditableConnection>();

			Hashtable<String, EditablePetriPlace> id2p = new Hashtable<String, EditablePetriPlace>();
			Hashtable<String, EditablePetriTransition> id2t = new Hashtable<String, EditablePetriTransition>();

			for(EditablePetriPlace p : source.places)
			{
				EditablePetriPlace newp = new EditablePetriPlace(newdoc.getRoot());

				newp.setId(p.getId());
				newp.copyCustomProperties(p);

				Vec2 v = new Vec2(0f, 0f);
				p.transform.getLocalToViewMatrix().transform(v);
				float x = v.getX();
				float y = v.getY();

				newp.setLabel(p.getLabel());
				newp.setTokens(p.getTokens());

				newp.transform.translateAbs(-x, -y, 0);

				id2p.put(p.getId(), newp);
			}

			for(EditablePetriTransition t : source.transitions)
			{
				EditablePetriTransition newt = new EditablePetriTransition(newdoc.getRoot());

				newt.setId(t.getId());
				newt.copyCustomProperties(t);
				newt.setLabel(t.getLabel());

				Vec2 v = new Vec2(0f, 0f);
				t.transform.getLocalToViewMatrix().transform(v);
				float x = v.getX();
				float y = v.getY();

				newt.transform.translateAbs(-x, -y, 0);

				id2t.put(t.getId(), newt);
			}

			for(EditableConnection c : source.connections)
				if (c.getFirst() instanceof EditablePetriPlace)
				{
					EditablePetriPlace from = id2p.get(c.getFirst().getId());
					EditablePetriTransition to = id2t.get(c.getSecond().getId());

					DefaultConnection newc = (DefaultConnection) newdoc.createConnection(from, to);

					newc.drawArrow = ((DefaultConnection)c).drawArrow; 
				}
				else
				{
					EditablePetriTransition from = id2t.get(c.getFirst().getId());
					EditablePetriPlace to = id2p.get(c.getSecond().getId());

					DefaultConnection newc = (DefaultConnection) newdoc.createConnection(from, to);

					newc.drawArrow = ((DefaultConnection)c).drawArrow; 
				}

			for(EditablePetriPlace p : (List<EditablePetriPlace>)newdoc.places.clone())
			{
				ts.clear();

				for(EditablePetriTransition t : p.getOut()) if (t.getOut().contains(p)) ts.add(t);

				if (ts.size() > 1)
				{
					int n = ts.size(); 
					ps.clear();
					ps.add(p);
					for(int i = 1; i < n; i++)
					{
						EditablePetriPlace pnew = new EditablePetriPlace(newdoc.getRoot());

						pnew.setId(p.getId() + "_" + i);

						Vec2 v = new Vec2(0f, 0f);
						p.transform.getLocalToViewMatrix().transform(v);
						float x = v.getX();
						float y = v.getY();

						pnew.setLabel(p.getLabel());
						pnew.setTokens(p.getTokens());

						pnew.transform.translateAbs(-(x + 0.025f), -y, 0);

						ps.add(pnew);
					}

					cs.clear();

					for(EditableConnection c : p.connections)
						if (ts.contains(c.getFirst()) || ts.contains(c.getSecond())) cs.add(c);

					for(int i = 0; i < cs.size(); i++) newdoc.removeConnection(cs.get(i));

					for(EditablePetriTransition t : p.getIn())
						for(int i = 0; i < n; i++) newdoc.createConnection(t, ps.get(i));

					for(EditablePetriTransition t : p.getOut())
						for(int i = 0; i < n; i++) newdoc.createConnection(ps.get(i), t);

					for(int i = 0; i < n; i++)
					{
						DefaultConnection c = (DefaultConnection) newdoc.createConnection(ps.get(i), ts.get(i));
						c.drawArrow = false;
						c = (DefaultConnection) newdoc.createConnection(ts.get(i), ps.get(i));
						c.drawArrow = false;
					}
				}



			}

			//for(EditablePetriPlace p : newdoc.places) p.setId(p.getId().substring(2));
			//for(EditablePetriTransition t : newdoc.transitions) t.setId(t.getId().substring(2));

		} catch (UnsupportedComponentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DuplicateIdException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newdoc;		
	}

	public void run(Editor editor, WorkCraftServer server)
	{
		PetriModel doc = (PetriModel) (editor.getDocument());
		ModelBase newdoc = reduce(doc);
		editor.setDocument(newdoc);
	}

	public boolean isModelSupported(UUID modelUuid)
	{
		// TODO Auto-generated method stub
		return false;
	}

	public void init(WorkCraftServer server)
	{
		// TODO Auto-generated method stub

	}

	public void deinit(WorkCraftServer server) {
		// TODO Auto-generated method stub

	}

	public ToolType getToolType() {
		return ToolType.TRANSFORM;
	}
}
