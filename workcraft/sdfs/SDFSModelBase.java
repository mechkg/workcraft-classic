package workcraft.sdfs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import workcraft.InvalidConnectionException;
import workcraft.ModelBase;
import workcraft.ModelValidationException;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.common.DefaultSimControls;
import workcraft.common.Trace;
import workcraft.editor.BasicEditable;
import workcraft.editor.EditableConnection;
import workcraft.spreadtoken.STLogic;
import workcraft.spreadtoken.STRegister;

public abstract class SDFSModelBase extends ModelBase {
	protected static DefaultSimControls panelSimControls = null;

	protected LinkedList<SDFSRegisterBase> registers;
	protected LinkedList<SDFSLogicBase> logic;

	protected List<String> traceReplay = null;
	private SimThread sim_thread = null;
	protected boolean allowNextStep = false;

	public class SimThread extends Thread {
		public void run() {

			Iterator<String> i = null;
			boolean nextActionFinished = false;


			if (traceReplay != null)
				i = traceReplay.iterator();
			
			String next = null;
			
			if (i.hasNext())
				next = i.next().split("/", 2)[0];
				
			while (true) {
				try {
					sleep(30);
					int time = (int)(System.nanoTime() / 1000000);


					if (traceReplay == null) {
						for (SDFSLogicBase l : logic)
							l.simTick(time);
						for (SDFSRegisterBase r : registers)
							r.simTick(time);						
					} else
					{ 
						if (panelSimControls.isStepByStepEnabled())
							if (!allowNextStep)
								continue;
						if (nextActionFinished) {
								if (i.hasNext())
									next = i.next().split("/", 2)[0];
								else
									next = null;
						}

						if (next != null) {
							//System.out.println("Next action: " +next);

							boolean nextFound = false;

							for (SDFSLogicBase l : logic)
								if (l.getId().equals(next)) {
									nextActionFinished = l.simTick(time);

									System.err.println (l.getId()+" TICK!");

									nextFound = true;
									break;
								}

							if (!nextFound)
								for (SDFSRegisterBase r : registers)
									if (r.getId().equals(next)) {
										nextActionFinished =  r.simTick(time);

										System.err.println (r.getId()+" TICK!");

										nextFound = true;
										break;
									}
							if (nextActionFinished)
								allowNextStep = false;

							if (!nextFound) {
								JOptionPane.showMessageDialog(null, "Component which is listed in trace, "+next+", is not present in the model. Replay terminated.", "End of replay", JOptionPane.WARNING_MESSAGE);
								break;
							}
						} else {
							JOptionPane.showMessageDialog(null, "Replay has finished.", "End of replay", JOptionPane.INFORMATION_MESSAGE);
							break;
						}
					}
					server.execPython("_redraw()");
				}
				catch (InterruptedException e) { break; }
			}
		}
	}

	public void addTraceEvent(String event) {
		if (panelSimControls != null) {
			if (panelSimControls.isTraceWriteEnabled())
				panelSimControls.addTraceEvent(event);
		}
	}

	public boolean isUserInteractionEnabled() {
		if (panelSimControls != null)
			return panelSimControls.isUserInteractionEnabled();
		else
			return false;
	}

	public void simBegin() {
		if (panelSimControls.isTraceReplayEnabled())
			traceReplay = panelSimControls.getTrace().getEvents();
		else
		{
			traceReplay = null;
			if (panelSimControls.isTraceWriteEnabled())
				panelSimControls.clearTrace();
		}

		if (sim_thread == null) {
			sim_thread = new SimThread();
			sim_thread.start();
		}

	}

	public void simReset() {
	}


	public void simStep() {
		allowNextStep = true;
	}

	public boolean simIsRunning() {
		return (sim_thread!=null);
	}

	public void simFinish() {
		if (sim_thread != null) {
			sim_thread.interrupt();
			sim_thread = null;
		}
	}	

	@Override
	public void restoreState(Object state) {
		if (state instanceof HashMap && server != null) {
			HashMap m = (HashMap)state;
			for (Object key : m.keySet()) {
				SDFSNode n = (SDFSNode)server.getObjectById(key.toString());
				if (n!=null)
					n.restoreState(m.get(key));
				else 
					JOptionPane.showMessageDialog(null, "Model structure inconsistent with state data: node "+key.toString() +" does not exist.", "Warning", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	@Override
	public Object saveState() {
		HashMap<String, Object> componentStates = new HashMap<String, Object>();
		LinkedList <BasicEditable> lst = new LinkedList<BasicEditable>();
		getComponents(lst);
		for (BasicEditable bn : lst)
			componentStates.put(bn.getId(),((SDFSNode)bn).saveState());			
		return componentStates;
	}
}