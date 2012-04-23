package workcraft;
import java.util.ArrayList;
import java.util.LinkedList;

import org.python.util.*;
import org.python.core.*;

import workcraft.editor.BasicEditable;


public class WorkCraftServer {
	public PythonInterpreter python = null;
	private boolean init_ok = false;
	private PluginManager pmgr;
	public ModelManager mmgr;

	private ArrayList<String> registeredIds = new ArrayList<String>();

	public void initialize()
	{
		if (init_ok)
		{
			System.out.println("Server already initialized.");
			return;
		}
		
		System.out.println("Initializing server...");
		
		System.out.println("\tInitializing Python...");
		 
		python = new PythonInterpreter();

		try
		{
			python.exec("from java.lang import System");
			python.exec("def quit():\n\tSystem.exit(0)");
			python.exec("def _redraw():\n\treturn");
			python.exec("def help():\n\tprint(\"Sorry?\")");
			python.set("ZERO", 0);
            python.set("ONE", 1);
            python.set("_srv", this);
            python.set("_debuginfo", 1);
			python.exec("_tools={}");
			System.out.println ("\tPython initialized.");
		}
		catch (PyException e)
		{
			System.err.println("\tPython initialization error:\n"+e);		
		}
		
		System.out.println ("Server initialized.\n\n");
		System.out.println ("Initializing components...");
		
		pmgr = new PluginManager();
		pmgr.loadManifest("Plugins");
		mmgr = new ModelManager();

		System.out.println("Models:");

		LinkedList<Class> models = pmgr.getClassesBySuperclass(ModelBase.class);
		for (Class cls : models) {
			mmgr.addModel(cls);		
		}

		System.out.println("Components:");

		LinkedList<Class> components = pmgr.getClassesBySuperclass(BasicEditable.class);
		for (Class cls : components) {
			mmgr.addComponent(cls);		
		}

		LinkedList<Class> tools = pmgr.getClassesByInterface(Tool.class);
		System.out.println("Tools:");
		for (Class cls : tools) {
			mmgr.addTool(cls, this);		
		}

		init_ok = true;
		
		System.out.println ("Components initialized.\n\n");
	}
	
	public void execPython(String s)
	{
		if (init_ok==false)
		{
			System.err.println("Server is not initialised.");
			return;
		}
		try
		{
			python.exec(s);
		}
		catch (PyException e)
		{
			System.err.println(e);		
		}
	}

	public void registerObject(Object o, String id) throws DuplicateIdException {
		if (id==null)
			throw new DuplicateIdException("null id");

		PyObject pyo = python.get(id);
		if (pyo != null) {
			if (o != pyo.__tojava__(Object.class))
				throw new DuplicateIdException(id);
		}

		registeredIds.add(id);
		python.set(id, o);
	}
	
	public void registerToolInstance (Tool tool) {
		//System.err.println("registered:" + tool.getClass().getName());
		
		PyObject t = python.get("_tools");
		t.__setitem__(tool.getClass().getName().intern(), Py.java2py(tool));
	}
	
	public Tool getToolInstance (String className) {
		//System.err.println("requested:" + className);
		
		PyObject t = python.get("_tools");
		PyObject o = t.__finditem__(className.intern());
		if (o != null)
			return (Tool)(o.__tojava__(Tool.class));
		else
			return null;
	}
	
	public Tool getToolInstance (Class cls) {
		return getToolInstance (cls.getName());
	}

	public void unregisterObject(String id) {
		if (id == null)
			return;
		if (python.get(id)!=null) {
			python.getLocals().__delitem__(id.intern());
			registeredIds.remove(id);
		}
	}

	public void unbind() {
		PyStringMap locals = (PyStringMap)python.getLocals();
		for (String id : registeredIds)
			locals.__delitem__(id.intern());
		registeredIds.clear();
	}

	public Object getObjectById(String id) {
		PyObject o =  python.getLocals().__finditem__(id.intern());
		if (o!=null)
			return o.__tojava__(Object.class);
		else
			return null;
	}
	
	public boolean testObject(String id) {
		PyObject o =  python.getLocals().__finditem__(id.intern());
		if (o!=null)
			return o.__nonzero__();
		else
			return false;
	}

	public LinkedList<Class> getClassesByInterface(Class interf)
	{
		return pmgr.getClassesByInterface(interf);
	}

	public LinkedList<Class> getClassesBySuperclass(Class superclass)
	{
		return pmgr.getClassesBySuperclass(superclass);
	}

	public void Cleanup()
	{
		python.cleanup();					
	}
	
	public void printDebugString(String s) {
		if (testObject("_debuginfo")) {
			System.err.print (s);
		}
	}
	
	public void printDebugLine(String s) {
		if (testObject("_debuginfo")) {
			System.err.println (s);
		}
	}
}