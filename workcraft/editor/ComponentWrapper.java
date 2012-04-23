package workcraft.editor;

public class ComponentWrapper {
	private Class component_class;
	
	public ComponentWrapper(Class cls) {
		component_class = cls;
	}
	
	public Class getComponentClass () {
		return component_class;		
	}
	
	public String toString() {
		String s = "INVALID COMPONENT";
		try {
			s = (String)component_class.getField("_displayname").get(null);
		}
		catch (NoSuchFieldException e) {
			System.err.println(e);
			
		}
		catch (IllegalAccessException t) {
			System.err.println(t);
		}
		
		String k = ComponentHotkey.getComponentHotkeyString(component_class);
		if (k!=null)
			return "["+k+"] " + s;
		else
			return s;
	}
}