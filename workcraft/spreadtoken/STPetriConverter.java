package workcraft.spreadtoken;

import java.util.HashMap;
import java.util.UUID;

import javax.swing.JOptionPane;

import workcraft.DuplicateIdException;
import workcraft.InvalidConnectionException;
import workcraft.Tool;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.common.DefaultConnection;
import workcraft.editor.Editor;
import workcraft.logic.DNF;
import workcraft.logic.DNFClause;
import workcraft.logic.DNFLiteral;
import workcraft.logic.InvalidExpressionException;
import workcraft.petri.EditablePetriPlace;
import workcraft.petri.EditablePetriTransition;
import workcraft.petri.PetriModel;
import workcraft.sdfs.LogicState;
import workcraft.sdfs.RegisterState;
import workcraft.util.Vec2;

public class STPetriConverter implements Tool {
	
	public static final String plus = "plus";
	public static final String minus = "minus";
	
	class LogicExpansion
	{
		EditablePetriPlace [] p;
		EditablePetriTransition [] te, tr;
		
		int ne, nr;
		
		DNF evaluate, reset;
		
		LogicExpansion(String id, PetriModel doc, float x, float y, DNF evaluate, DNF reset) throws DuplicateIdException, UnsupportedComponentException
		{
			this.evaluate = evaluate;
			this.reset = reset;
			
			ne = evaluate.clauses.size();
			nr = reset.clauses.size();			
			
			// components creation
			
			p = new EditablePetriPlace[2];
			te = new EditablePetriTransition[ne];
			tr = new EditablePetriTransition[nr];
			
			p[0] = new EditablePetriPlace(doc);
			p[0].setId(id+"_0");
			
			p[1] = new EditablePetriPlace(doc);
			p[1].setId(id+"_1");

			for(int i = 0; i<ne; i++)
			{
				te[i] = new EditablePetriTransition(doc);
				te[i].setId(id+"_"+plus+"_"+i);
			}
			for(int i = 0; i<nr; i++)
			{
				tr[i] = new EditablePetriTransition(doc);
				tr[i].setId(id+"_"+minus+"_"+i);
			}
			
			// placement
			
			x*=5;
			y*=11;
			
			p[0].transform.translateAbs(-(x - 0.2f), -y, 0);
			p[1].transform.translateAbs(-(x + 0.2f), -y, 0);

			for(int i=0; i<ne; i++)
			{
				te[i].transform.translateAbs(-x, -(y + (i+1)*0.2f), 0);
			}
			
			for(int i=0; i<nr; i++)
			{
				tr[i].transform.translateAbs(-x, -(y - (i+1)*0.2f), 0);			
			}
	
			// connections
			
			try
			{
				DefaultConnection c;
				for(int i=0; i<ne; i++)
				{
					c = (DefaultConnection) doc.createConnection(p[0], te[i]);

					c.connectionType = DefaultConnection.Type.bezierConnection;
					c.setInternalPoints(makeTurnL(p[0].transform.getTranslation2d(), te[i].transform.getTranslation2d()));
										
					c = (DefaultConnection) doc.createConnection(te[i], p[1]);

					c.connectionType = DefaultConnection.Type.bezierConnection;
					c.setInternalPoints(makeTurnR(te[i].transform.getTranslation2d(), p[1].transform.getTranslation2d()));
				}
				for(int i=0; i<nr; i++)
				{
					c = (DefaultConnection) doc.createConnection(p[1], tr[i]);

					c.connectionType = DefaultConnection.Type.bezierConnection;
					c.setInternalPoints(makeTurnL(p[1].transform.getTranslation2d(), tr[i].transform.getTranslation2d()));

					c = (DefaultConnection) doc.createConnection(tr[i], p[0]);

					c.connectionType = DefaultConnection.Type.bezierConnection;
					c.setInternalPoints(makeTurnR(tr[i].transform.getTranslation2d(), p[0].transform.getTranslation2d()));
				}
			}
			catch (InvalidConnectionException e) {}
						
		}

		private Vec2[] makeTurnL(Vec2 v1, Vec2 v2)
		{
			Vec2 [] v = new Vec2[2];
			v[0] = new Vec2(-v1.getX(), -(v1.getY()+0.66f*(v2.getY()-v1.getY())));					
			v[1] = new Vec2(-(v2.getX() + 0.66f*(v1.getX()-v2.getX())), -v2.getY());
			return v;
		}
		
		private Vec2[] makeTurnR(Vec2 v1, Vec2 v2)
		{
			Vec2 [] v = new Vec2[2];
			v[0] = new Vec2(-(v1.getX() + 0.66f*(v2.getX()-v1.getX())), -v1.getY());
			v[1] = new Vec2(-v2.getX(), -(v2.getY()+0.66f*(v1.getY()-v2.getY())));					
			return v;
		}
	}
	
	class RegisterExpansion
	{
		EditablePetriPlace [] pe;
		EditablePetriTransition [] te;
	
		EditablePetriPlace [] pm;
		EditablePetriTransition [] tm;
		
		RegisterExpansion(String id, PetriModel doc, float x, float y) throws DuplicateIdException, UnsupportedComponentException
		{
			// components creation
			
			pe = new EditablePetriPlace[2];
			te = new EditablePetriTransition[2];
			
			pm = new EditablePetriPlace[2];
			tm = new EditablePetriTransition[2];

			pe[0] = new EditablePetriPlace(doc);
			pe[0].setId("e"+id+"_0");
			
			pe[1] = new EditablePetriPlace(doc);
			pe[1].setId("e"+id+"_1");

			te[0] = new EditablePetriTransition(doc);
			te[0].setId("e"+id+"_"+plus);
			
			te[1] = new EditablePetriTransition(doc);
			te[1].setId("e"+id+"_"+minus);
			
			pm[0] = new EditablePetriPlace(doc);
			pm[0].setId("m"+id+"_0");
			
			pm[1] = new EditablePetriPlace(doc);
			pm[1].setId("m"+id+"_1");

			tm[0] = new EditablePetriTransition(doc);
			tm[0].setId("m"+id+"_"+plus);
			
			tm[1] = new EditablePetriTransition(doc);
			tm[1].setId("m"+id+"_"+minus);
			
		
			
			// placement
			
			x*=5;
			y*=11;
			
			pe[0].transform.translateAbs(-(x - 0.2f), -(y + 0.3f), 0);
			pe[1].transform.translateAbs(-(x + 0.2f), -(y + 0.3f), 0);

			te[0].transform.translateAbs(-x, -(y + 0.5f), 0);
			te[1].transform.translateAbs(-x, -(y + 0.1f), 0);
			
			pm[0].transform.translateAbs(-(x - 0.2f), -(y - 0.3f), 0);
			pm[1].transform.translateAbs(-(x + 0.2f), -(y - 0.3f), 0);

			tm[0].transform.translateAbs(-x, -(y - 0.1f), 0);
			tm[1].transform.translateAbs(-x, -(y - 0.5f), 0);
			
			// connections
			
			try
			{
				DefaultConnection c;
					
				c = (DefaultConnection) doc.createConnection(pe[0], te[0]);
				c.connectionType = DefaultConnection.Type.bezierConnection;
				c.setInternalPoints(makeTurnL(pe[0].transform.getTranslation2d(), te[0].transform.getTranslation2d()));
				
				c = (DefaultConnection) doc.createConnection(pm[0], te[0]); c.drawArrow = false;
				c = (DefaultConnection) doc.createConnection(te[0], pm[0]); c.drawArrow = false;
				
				c = (DefaultConnection) doc.createConnection(te[0], pe[1]);
				c.connectionType = DefaultConnection.Type.bezierConnection;
				c.setInternalPoints(makeTurnR(te[0].transform.getTranslation2d(), pe[1].transform.getTranslation2d()));
				
				c = (DefaultConnection) doc.createConnection(pe[1], te[1]);
				c.connectionType = DefaultConnection.Type.bezierConnection;
				c.setInternalPoints(makeTurnL(pe[1].transform.getTranslation2d(), te[1].transform.getTranslation2d()));
				
				c = (DefaultConnection) doc.createConnection(pm[1], te[1]); c.drawArrow = false;
				c = (DefaultConnection) doc.createConnection(te[1], pm[1]); c.drawArrow = false;
				
				c = (DefaultConnection) doc.createConnection(te[1], pe[0]);
				c.connectionType = DefaultConnection.Type.bezierConnection;
				c.setInternalPoints(makeTurnR(te[1].transform.getTranslation2d(), pe[0].transform.getTranslation2d()));

				c = (DefaultConnection) doc.createConnection(pm[0], tm[0]);
				c.connectionType = DefaultConnection.Type.bezierConnection;
				c.setInternalPoints(makeTurnL(pm[0].transform.getTranslation2d(), tm[0].transform.getTranslation2d()));
				
				c = (DefaultConnection) doc.createConnection(pe[1], tm[0]); c.drawArrow = false;
				c = (DefaultConnection) doc.createConnection(tm[0], pe[1]); c.drawArrow = false;
				
				c = (DefaultConnection) doc.createConnection(tm[0], pm[1]);
				c.connectionType = DefaultConnection.Type.bezierConnection;
				c.setInternalPoints(makeTurnR(tm[0].transform.getTranslation2d(), pm[1].transform.getTranslation2d()));
				
				c = (DefaultConnection) doc.createConnection(pm[1], tm[1]);
				c.connectionType = DefaultConnection.Type.bezierConnection;
				c.setInternalPoints(makeTurnL(pm[1].transform.getTranslation2d(), tm[1].transform.getTranslation2d()));

				c = (DefaultConnection) doc.createConnection(pe[0], tm[1]); c.drawArrow = false;
				c = (DefaultConnection) doc.createConnection(tm[1], pe[0]); c.drawArrow = false;
				
				c = (DefaultConnection) doc.createConnection(tm[1], pm[0]);
				c.connectionType = DefaultConnection.Type.bezierConnection;
				c.setInternalPoints(makeTurnR(tm[1].transform.getTranslation2d(), pm[0].transform.getTranslation2d()));
			}
			catch (InvalidConnectionException e) {}
						
		}
		
		private Vec2[] makeTurnL(Vec2 v1, Vec2 v2)
		{
			Vec2 [] v = new Vec2[2];
			v[0] = new Vec2(-v1.getX(), -(v1.getY()+0.66f*(v2.getY()-v1.getY())));					
			v[1] = new Vec2(-(v2.getX() + 0.66f*(v1.getX()-v2.getX())), -v2.getY());
			return v;
		}
		
		private Vec2[] makeTurnR(Vec2 v1, Vec2 v2)
		{
			Vec2 [] v = new Vec2[2];
			v[0] = new Vec2(-(v1.getX() + 0.66f*(v2.getX()-v1.getX())), -v1.getY());
			v[1] = new Vec2(-v2.getX(), -(v2.getY()+0.66f*(v1.getY()-v2.getY())));					
			return v;
		}
	}
	
	public static final String _modeluuid = "a57b3350-73d3-11db-9fe1-0800200c9a66";
	public static final String _displayname = "Spread token to Petri Net Converter";

	public void run(Editor editor, WorkCraftServer server) {
		SpreadTokenModel doc = (SpreadTokenModel) (editor.getDocument());
		
		PetriModel petri = new PetriModel();
		petri.init(server);
		
		HashMap<String, LogicExpansion> les = new HashMap<String, LogicExpansion>();
		HashMap<String, RegisterExpansion> res = new HashMap<String, RegisterExpansion>();
					
		try
		{
			// creating components expansions
			
			for(EditableSTLogic logic : doc.getLogic())
			{
				String id = logic.getId();

				DNF evaluate = new DNF();
				evaluate.parseExpression(logic.getEvalFunc());
				
				DNF reset = new DNF();
				reset.parseExpression(logic.getResetFunc());				
				
				Vec2 v = new Vec2(0f,0f);
				logic.transform.getLocalToViewMatrix().transform(v);
				float x = v.getX();
				float y = v.getY();
				les.put(id, new LogicExpansion(id, petri, x, y, evaluate, reset));
				if (logic.getState()==LogicState.EVALUATED)	les.get(id).p[1].setTokens(1); else les.get(id).p[0].setTokens(1); 
			}
			for(EditableSTRegister register : doc.getRegisters())
			{
				String id = register.getId();
				Vec2 v = new Vec2(0f,0f);
				register.transform.getLocalToViewMatrix().transform(v);
				float x = v.getX();
				float y = v.getY();
				res.put(id, new RegisterExpansion(id, petri, x, y));
				
				if (register.getState()==RegisterState.ENABLED)	res.get(id).pe[1].setTokens(1); else res.get(id).pe[0].setTokens(1); 
				
				if (register.getTokens() > 0) res.get(id).pm[1].setTokens(1); else res.get(id).pm[0].setTokens(1);
			}
			
			// connecting components expansions
			
			DefaultConnection c;
			
			for(EditableSTLogic logic : doc.getLogic())
			{
				LogicExpansion le = les.get(logic.getId());
				
				// Evaluation function implementation
				
				int k = 0;
				
				for(DNFClause clause: le.evaluate.clauses)
				{
					EditablePetriTransition t = le.te[k];					
					for(DNFLiteral literal: clause.pos)
					{
						EditablePetriPlace p = null;
						String id = literal.id.substring(1);
						if (literal.id.charAt(0) == 'M')
						{
							p = res.get(id).pm[1];
						}
						else
						if (literal.id.charAt(0) == 'E')
						{
							p = les.get(id).p[1];
						}
						c = (DefaultConnection) petri.createConnection(p, t); c.drawArrow = false;
						c = (DefaultConnection) petri.createConnection(t, p); c.drawArrow = false;
					}
					for(DNFLiteral literal: clause.neg)
					{
						EditablePetriPlace p = null;
						String id = literal.id.substring(1);
						if (literal.id.charAt(0) == 'M')
						{
							p = res.get(id).pm[0];
						}
						else
						if (literal.id.charAt(0) == 'E')
						{
							p = les.get(id).p[0];
						}
						c = (DefaultConnection) petri.createConnection(p, t); c.drawArrow = false;
						c = (DefaultConnection) petri.createConnection(t, p); c.drawArrow = false;
					}
					k++;
				}
				
				// Reset function implementation
				
				k = 0;
				
				for(DNFClause clause: le.reset.clauses)
				{
					EditablePetriTransition t = le.tr[k];					
					for(DNFLiteral literal: clause.pos)
					{
						EditablePetriPlace p = null;
						String id = literal.id.substring(1);
						if (literal.id.charAt(0) == 'M')
						{
							p = res.get(id).pm[1];
						}
						else
						if (literal.id.charAt(0) == 'E')
						{
							p = les.get(id).p[1];
						}
						c = (DefaultConnection) petri.createConnection(p, t); c.drawArrow = false;
						c = (DefaultConnection) petri.createConnection(t, p); c.drawArrow = false;
					}
					for(DNFLiteral literal: clause.neg)
					{
						EditablePetriPlace p = null;
						String id = literal.id.substring(1);
						if (literal.id.charAt(0) == 'M')
						{
							p = res.get(id).pm[0];
						}
						else
						if (literal.id.charAt(0) == 'E')
						{
							p = les.get(id).p[0];
						}
						c = (DefaultConnection) petri.createConnection(p, t); c.drawArrow = false;
						c = (DefaultConnection) petri.createConnection(t, p); c.drawArrow = false;
					}
					k++;
				}								
			}
			
			for(EditableSTRegister register : doc.getRegisters())
			{
				RegisterExpansion re = res.get(register.getId());
				for(STNode node : register.getIn())
					if (node instanceof EditableSTLogic)
					{
						LogicExpansion le_prev = les.get(((EditableSTLogic)node).getId());
						c = (DefaultConnection) petri.createConnection(le_prev.p[1], re.te[0]); c.drawArrow = false;
						c = (DefaultConnection) petri.createConnection(re.te[0], le_prev.p[1]); c.drawArrow = false;
						
						c = (DefaultConnection) petri.createConnection(le_prev.p[0], re.te[1]); c.drawArrow = false;
						c = (DefaultConnection) petri.createConnection(re.te[1], le_prev.p[0]); c.drawArrow = false;
					}
					else
					if (node instanceof EditableSTRegister)
					{
						RegisterExpansion re_prev = res.get(((EditableSTRegister)node).getId());
						c = (DefaultConnection) petri.createConnection(re_prev.pm[1], re.te[0]); c.drawArrow = false;
						c = (DefaultConnection) petri.createConnection(re.te[0], re_prev.pm[1]); c.drawArrow = false;
						
						c = (DefaultConnection) petri.createConnection(re_prev.pm[0], re.te[1]); c.drawArrow = false;
						c = (DefaultConnection) petri.createConnection(re.te[1], re_prev.pm[0]); c.drawArrow = false;
					}
						

				for(EditableSTRegister node: doc.getRegisterPreset(register))
				{
					RegisterExpansion re_prev = res.get(node.getId());
					
					c = (DefaultConnection) petri.createConnection(re_prev.pm[1], re.tm[0]); c.drawArrow = false;					
					c = (DefaultConnection) petri.createConnection(re.tm[0], re_prev.pm[1]); c.drawArrow = false;
					
					c = (DefaultConnection) petri.createConnection(re_prev.pm[0], re.tm[1]); c.drawArrow = false;
					c = (DefaultConnection) petri.createConnection(re.tm[1], re_prev.pm[0]); c.drawArrow = false;
				}

				for(EditableSTRegister node: doc.getRegisterPostset(register))
				{
					RegisterExpansion re_next = res.get(node.getId());
				
					c = (DefaultConnection) petri.createConnection(re_next.pm[0], re.tm[0]); c.drawArrow = false;					
					c = (DefaultConnection) petri.createConnection(re.tm[0], re_next.pm[0]); c.drawArrow = false;
					
					c = (DefaultConnection) petri.createConnection(re_next.pm[1], re.tm[1]); c.drawArrow = false;
					c = (DefaultConnection) petri.createConnection(re.tm[1], re_next.pm[1]); c.drawArrow = false;
				}
			}			
		}
		catch (DuplicateIdException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidConnectionException e) {
			// TODO Auto-generated catch block
			System.err.println("-------------------------------");
			e.printStackTrace();
		} catch (UnsupportedComponentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (InvalidExpressionException e)
		{
			JOptionPane.showMessageDialog(null, e.getMessage(), "Error during conversion to Petri Model", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		editor.setDocument(petri); 
	}

	public boolean isModelSupported(UUID modelUuid) {
		// TODO Auto-generated method stub
		return false;
	}

	public void load() {
		// TODO Auto-generated method stub
		
	}
}
