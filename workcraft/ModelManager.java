package workcraft;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.UUID;


public class ModelManager {
	private Hashtable<UUID, Class> uuid_model_map;
	private Hashtable<UUID, LinkedList<Class>> uuid_component_list_map;
	private Hashtable<UUID, LinkedList<Class>> uuid_tool_list_map;
	private LinkedList<Class> model_list;
	private LinkedList<Tool> multi_tool_list;

	public ModelManager() {
		uuid_model_map = new Hashtable<UUID, Class>();
		uuid_component_list_map = new Hashtable<UUID, LinkedList<Class>>();
		uuid_tool_list_map = new Hashtable<UUID, LinkedList<Class>>();
		model_list = new LinkedList<Class>();
		multi_tool_list = new LinkedList<Tool>(); 
	}

	public LinkedList<Class> getComponentsByModelUUID(UUID uuid) {
		LinkedList<Class> lst = uuid_component_list_map.get(uuid);
		if (lst!=null)
			return (LinkedList<Class>)lst.clone();
		else
			return null;
	}

	public LinkedList<Class> getToolsByModelUUID(UUID uuid) {
		LinkedList<Class> lst = uuid_tool_list_map.get(uuid);
		if (lst!=null)
			return (LinkedList<Class>)lst.clone();
		else
			return null;
	}

	public LinkedList<Tool> getMultiModelTools() {
		return (LinkedList<Tool>)multi_tool_list.clone();
	}

	public LinkedList<Class> getModelList() {
		return (LinkedList<Class>)model_list.clone();
	}

	public Class getModelByUUID(UUID uuid) {
		return uuid_model_map.get(uuid);
	}

	public static boolean isValidModelClass(Class cls) {
		boolean if_ok = ModelBase.class.isAssignableFrom(cls);

		try
		{
			UUID uuid = (UUID)cls.getField("_modeluuid").get(null);
			String model_name = (String)cls.getField("_displayname").get(null);
		}
		catch (NoSuchFieldException e) {
			return false;
		}
		catch (IllegalAccessException e) {
			return false;
		}

		return if_ok;
	}

	public static boolean isValidToolClass(Class cls) {
		boolean if_ok = false;
		for (Class i : cls.getInterfaces()) {
			if (i == Tool.class)
			{
				if_ok = true;
				break;
			}
		}

		try
		{
			String uuid = (String)cls.getField("_modeluuid").get(null);
			String model_name = (String)cls.getField("_displayname").get(null);
		}
		catch (NoSuchFieldException e) {
			return false;
		}
		catch (IllegalAccessException e) {
			return false;
		}

		return if_ok;
	}

	public static UUID getModelUUID(Class model_class) {
		UUID uuid = null;
		if (!isValidModelClass(model_class))
			return null;
		try
		{
			uuid = (UUID)model_class.getField("_modeluuid").get(null);
		}
		catch (NoSuchFieldException e) {
			System.err.println("Model implementation class is improperly declared: static final String "+e.getMessage()+" is required");
		}
		catch (IllegalAccessException e) {
			System.err.println("Model implementation class is improperly declared: static final String "+e.getMessage()+" is required");
		}
		return uuid;
	}

	public static String getModelDisplayName(Class model_class) {
		if (!isValidModelClass(model_class))
			return null;
		try
		{
			return (String)model_class.getField("_displayname").get(null);
		}
		catch (NoSuchFieldException e) {
			System.err.println("Model implementation class is improperly declared: static final String "+e.getMessage()+" is required");
		}
		catch (IllegalAccessException e) {
			System.err.println("Model implementation class is improperly declared: static final String "+e.getMessage()+" is required");
		}
		return null;
	}

	public static String getToolDisplayName(Class tool_class) {
		if (!isValidToolClass(tool_class))
			return null;
		try
		{
			return (String)tool_class.getField("_displayname").get(null);
		}
		catch (NoSuchFieldException e) {
			System.err.println("Tool implementation class is improperly declared: static final String "+e.getMessage()+" is required");
		}
		catch (IllegalAccessException e) {
			System.err.println("Tool implementation class is improperly declared: static final String "+e.getMessage()+" is required");
		}
		return null;
	}	

	public void addModel(Class cls) {
		try
		{
			UUID uuid = (UUID)cls.getField("_modeluuid").get(null);
			String model_name = (String)cls.getField("_displayname").get(null);
			if (uuid_model_map.get(uuid)!=null) {
				System.err.println ("Duplicate model id ("+uuid.toString()+"), skipping");
				return;
			}
			model_list.add(cls);
			uuid_model_map.put(uuid, cls);
			System.out.println("\t"+model_name+" added");
		}
		catch (NoSuchFieldException e) {
			System.err.println("Model implementation class is improperly declared: static final String "+e.getMessage()+" is required");
		}
		catch (IllegalAccessException e) {
			System.err.println("Model implementation class is improperly declared: static final String "+e.getMessage()+" is required");
		}
	}

	public void addComponent (Class cls) {
		try
		{
			UUID uuid = (UUID)cls.getField("_modeluuid").get(null);
			String component_name = (String)cls.getField("_displayname").get(null);

			if (uuid_model_map.get(uuid)==null) {
				System.err.println ("Component "+component_name+"(class "+cls.getName()+") refers to unknown model (id "+uuid.toString()+"), skipping");
				return;
			}

			LinkedList<Class> list = uuid_component_list_map.get(uuid);

			if (list == null)
			{
				list = new LinkedList<Class>();
				uuid_component_list_map.put(uuid, list);
			}

			list.add(cls);

			System.out.println("\t"+component_name+" added");
		}
		catch (NoSuchFieldException e) {
			System.err.println("Component implementation class is improperly declared: static final String "+e.getMessage()+" is required");
		}
		catch (IllegalAccessException e) {
			System.err.println("Component implementation class is improperly declared: static final String "+e.getMessage()+" is required");
		}		
	}

	public void addTool (Class cls, WorkCraftServer server) {
		try
		{
			String model_uuid = (String)cls.getField("_modeluuid").get(null);
			String component_name = (String)cls.getField("_displayname").get(null);
			
			Tool tool =(Tool) cls.newInstance();
			tool.init(null);
			server.registerToolInstance(tool);			

			if (model_uuid.equals("*")) {
				multi_tool_list.add(tool);
			} else {
				UUID uuid = UUID.fromString(model_uuid);

				if (uuid_model_map.get(uuid)==null) {
					System.err.println ("Tool "+component_name+"(class "+cls.getName()+") refers to unknown model (id "+uuid.toString()+"), skipping");
					return;
				}

				LinkedList<Class> list = uuid_tool_list_map.get(uuid);

				if (list == null)
				{
					list = new LinkedList<Class>();
					uuid_tool_list_map.put(uuid, list);
				}

				list.add(cls);
			}

			System.out.println("\t"+component_name+" added");
		}
		catch (NoSuchFieldException e) {
			System.err.println("Tool implementation class is improperly declared: static final String "+e.getMessage()+" is required");
		}
		catch (IllegalAccessException e) {
			System.err.println("Tool implementation class is improperly declared: static final String "+e.getMessage()+" is required");
		} catch (InstantiationException e) {
			e.printStackTrace();
		}			
	}
}