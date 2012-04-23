package workcraft.ADC;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.swing.JPanel;

import workcraft.DuplicateIdException;
import workcraft.InvalidConnectionException;
import workcraft.Model;
import workcraft.ModelBase;
import workcraft.ModelValidationException;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.common.DefaultConnection;
import workcraft.common.DefaultSimControls;
import workcraft.editor.BasicEditable;
import workcraft.editor.EditableConnection;
import workcraft.editor.EditorPane;

public class ADCModel extends ModelBase
{
	public static final UUID _modeluuid = UUID.fromString("3c60dee8-0545-11dc-8314-0800200c9a66");
	public static final String _displayname = "Asynchronous Data Communication";

	public class SimThread extends Thread
	{
		public void run()
		{
			while (true)
			{
				try
				{
					sleep(30);
					
					long time = (long)((System.nanoTime() / 1000000) * panelSimControls.getSpeedFactor()) ;
					
					for(EditableProcess p : processes) p.simTick(time);
					
					simStep();
					server.execPython("_redraw()");
				}
				catch (InterruptedException e)
				{
					break;
				}
			}
		}
	}

	int a_name_cnt = 0;
	int r_name_cnt = 0;
	int w_name_cnt = 0;
	int p_name_cnt = 0;
	
	private int state = 0;
	private boolean loading;

	SimThread sim_thread = null;
	public static DefaultSimControls panelSimControls = null;

	LinkedList<EditableACM> ACMs;
	LinkedList<EditableReader> readers;
	LinkedList<EditableWriter> writers;
	LinkedList<EditableProcess> processes;
	LinkedList<DefaultConnection> connections;

	public ADCModel()
	{
		ACMs = new LinkedList<EditableACM>();
		readers = new LinkedList<EditableReader>();
		writers = new LinkedList<EditableWriter>();
		processes = new LinkedList<EditableProcess>();
		connections = new LinkedList<DefaultConnection>();
	}

	public String getNextACMID()
	{
		return "a"+a_name_cnt++;
	}

	public String getNextReaderID()
	{
		return "r"+r_name_cnt++;
	}

	public String getNextWriterID()
	{
		return "w"+w_name_cnt++;
	}

	public String getNextProcessID()
	{
		return "p"+p_name_cnt++;
	}

	public void addComponent(BasicEditable c, boolean auto_name) throws UnsupportedComponentException
	{
		if (c instanceof EditableProcess)
		{
			EditableProcess p = (EditableProcess)c;
			processes.add(p);
			p.setOwnerDocument(this);
			if (auto_name)
				for (;;)
				{
					try
					{
						p.setId(getNextProcessID());
						break;
					}
					catch (DuplicateIdException e)
					{
					}
				}		
		}
		else
		if (c instanceof EditableACM)
		{
			EditableACM p = (EditableACM)c;
			ACMs.add(p);
			p.setOwnerDocument(this);
			if (auto_name)
				for (;;)
				{
					try
					{
						p.setId(getNextACMID());
						break;
					}
					catch (DuplicateIdException e)
					{
					}
				}		
		}
		else
		if (c instanceof EditableReader)
		{
			EditableReader p = (EditableReader)c;
			readers.add(p);
			p.setOwnerDocument(this);
			if (auto_name)
				for (;;)
				{
					try
					{
						p.setId(getNextReaderID());
						break;
					}
					catch (DuplicateIdException e)
					{
					}
				}		
		}
		else
		if (c instanceof EditableWriter)
		{
			EditableWriter p = (EditableWriter)c;
			writers.add(p);
			p.setOwnerDocument(this);
			if (auto_name)
				for (;;)
				{
					try
					{
						p.setId(getNextWriterID());
						break;
					}
					catch (DuplicateIdException e)
					{
					}
				}		
		}
		else
		throw new UnsupportedComponentException();
		
		super.addComponent(c, auto_name);
	}

	public void removeComponent(BasicEditable c) throws UnsupportedComponentException
	{
		super.removeComponent(c);
		if (c instanceof EditableProcess)
		{
			EditableProcess v = (EditableProcess)c;
			for (EditableWriter t : v.getOut())	t.removeIn(v);				
			for (EditableReader t : v.getIn()) t.removeOut(v);

			processes.remove(c);		
		}
		else
		if (c instanceof EditableACM)
		{
			EditableACM v = (EditableACM)c;
			for (EditableWriter t : v.getIn())	t.removeOut(v);				
			for (EditableReader t : v.getOut()) t.removeIn(v);

			ACMs.remove(c);	
		}
		else
		if (c instanceof EditableReader)
		{
			EditableReader v = (EditableReader)c;
			for (EditableACM t : v.getIn())	t.removeOut(v);				

			readers.remove(c);
		}
		else
		if (c instanceof EditableWriter)
		{
			EditableWriter v = (EditableWriter)c;
			for (EditableACM t : v.getOut()) t.removeIn(v);				

			writers.remove(c);
		}
		else
		throw new UnsupportedComponentException();
		
		super.removeComponent(c);
	}


	public EditableConnection createConnection(BasicEditable first, BasicEditable second) throws InvalidConnectionException
	{
		if (first instanceof EditableProcess && second instanceof EditableWriter)
		{
			EditableProcess p = (EditableProcess)first;
			EditableWriter q = (EditableWriter)second;
			
			DefaultConnection con = new DefaultConnection(p, q);
			
			if (p.addOut(con) && q.addIn(con))
			{
				connections.add(con);
				return con;
			}
			
			return null;
		}

		if (first instanceof EditableReader && second instanceof EditableProcess)
		{
			EditableReader p = (EditableReader)first;
			EditableProcess q = (EditableProcess)second;
			
			DefaultConnection con = new DefaultConnection(p, q);
			
			if (p.addOut(con) && q.addIn(con))
			{
				connections.add(con);
				return con;
			}
			
			return null;
		}

		if (first instanceof EditableACM && second instanceof EditableReader)
		{
			EditableACM p = (EditableACM)first;
			EditableReader q = (EditableReader)second;
			
			DefaultConnection con = new DefaultConnection(p, q);
			
			if (p.addOut(con) && q.addIn(con))
			{
				connections.add(con);
				return con;
			}
			
			return null;
		}

		if (first instanceof EditableWriter && second instanceof EditableACM)
		{
			EditableWriter p = (EditableWriter)first;
			EditableACM q = (EditableACM)second;
			
			DefaultConnection con = new DefaultConnection(p, q);
			
			if (p.addOut(con) && q.addIn(con))
			{
				connections.add(con);
				return con;
			}
			
			return null;
		}		
		throw new InvalidConnectionException ("Invalid connection.");
	}

	public void removeConnection(EditableConnection con) throws UnsupportedComponentException
	{
		if (con.getFirst() instanceof EditableProcess && con.getSecond() instanceof EditableWriter)
		{
			EditableProcess p = (EditableProcess)con.getFirst();
			EditableWriter q = (EditableWriter)con.getSecond();
			
			p.removeOut(q);
			q.removeIn(p);
		}

		if (con.getFirst() instanceof EditableReader && con.getSecond() instanceof EditableProcess)
		{
			EditableReader p = (EditableReader)con.getFirst();
			EditableProcess q = (EditableProcess)con.getSecond();
			
			p.removeOut(q);
			q.removeIn(p);
		}
		
		if (con.getFirst() instanceof EditableACM && con.getSecond() instanceof EditableReader)
		{
			EditableACM p = (EditableACM)con.getFirst();
			EditableReader q = (EditableReader)con.getSecond();
			
			p.removeOut(q);
			q.removeIn(p);
		}

		if (con.getFirst() instanceof EditableWriter && con.getSecond() instanceof EditableACM)
		{
			EditableWriter p = (EditableWriter)con.getFirst();
			EditableACM q = (EditableACM)con.getSecond();
			
			p.removeOut(q);
			q.removeIn(p);
		}
		
		connections.remove(con);
	}

	public void simReset()
	{
		state = 0;
		
		for (EditableReader t : readers) t.canFire = false;
		for (EditableWriter t : writers) t.canFire = false;
	}

	public void simBegin()
	{
		if (sim_thread==null)
		{
			simReset();
			sim_thread = new SimThread();
			sim_thread.start();
		}
	}

	public void simStep() {
		switch (state) {
		case 0:
			for (EditableReader r : readers)
			{
				boolean canfire = false;
				for (EditableACM a : r.getIn()) {
					if (a.canRead())
					{
						canfire = true;
						break;
					}
				}
				
				r.canFire = canfire;
			}			
			for (EditableWriter w : writers)
			{
				boolean canfire = false;
				for (EditableACM a : w.getOut()) {
					if (a.canWrite())
					{
						canfire = true;
						break;
					}
				}
				
				w.canFire = canfire;
			}
			state = 1;
			break;
		case 1:
			if (panelSimControls.isUserInteractionEnabled())
			{
				for (EditableReader r : readers)
					if (r.canFire && r.canWork)
					{
						for(EditableACM a : r.getIn())
							if (a.canRead()) r.token = a.read();
						
						r.canFire = r.canWork = false;
						break;
					}

				for (EditableWriter w : writers)
					if (w.canFire && w.canWork)
					{
						ADCToken token = new ADCToken();
						for(EditableACM a : w.getOut())
							if (a.canWrite()) a.write(token);
						
						w.canFire = w.canWork = false;
						break;
					}
			}
			else
			{
				int cnt = 0;
				
				for (EditableReader r : readers) if (r.canFire) cnt++;
				for (EditableWriter w : writers) if (w.canFire) cnt++;
				
				int k = (int)Math.floor(Math.random() * cnt);
				
				for (EditableReader r : readers)
					if (r.canFire) 
					{
						if (k == 0)
						{
							for(EditableACM a : r.getIn())
								if (a.canRead()) r.token = a.read();
							
							r.canFire = false;
							k--;
							break;
						}
						k--;
					}
				
				if (k >= 0)
				for (EditableWriter w : writers)
					if (w.canFire) 
					{
						if (k == 0)
						{
							ADCToken token = new ADCToken();
							for(EditableACM a : w.getOut())
								if (a.canWrite()) a.write(token);
							
							w.canFire = false;
							break;
						}
						k--;
					}				
			}

			state = 0;
			
			break;
		}

	}

	public boolean simIsRunning()
	{
		return (sim_thread != null);
	}

	public void simFinish()
	{
		if (sim_thread!=null) {
			sim_thread.interrupt();
			sim_thread = null;
		}
	}

	public List<EditableConnection> getConnections()
	{
		return (List<EditableConnection>)((List)connections);		
	}

	public JPanel getSimulationControls()
	{
		if (panelSimControls == null)
		{
			panelSimControls = new DefaultSimControls(_modeluuid.toString());
	    }
		return panelSimControls;
	}

	public void bind(WorkCraftServer server)
	{
		this.server = server;
		server.python.set("_document", this);
	}

	public EditorPane getEditor()
	{
		return editor;
	}

	public void setEditor(EditorPane editor)
	{
		this.editor = editor;
	}

	public WorkCraftServer getServer()
	{
		return server;
	}

	public void setLoading(boolean loading)
	{
		this.loading = loading;
	}

	public boolean isLoading()
	{
		return loading;
	}

	public void getComponents(List<BasicEditable> out)
	{
		for (BasicEditable t: processes) out.add(t);
		for (BasicEditable t: ACMs)	out.add(t);
		for (BasicEditable t: readers) out.add(t);
		for (BasicEditable t: writers) out.add(t);
	}

	public void validate() throws ModelValidationException
	{
		// TODO Auto-generated method stub
	}
}