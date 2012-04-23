package workcraft.editor;

import java.awt.event.KeyEvent;
import java.util.HashMap;

public class ComponentHotkey {
	HashMap <Integer, Class> vk_to_component  = new HashMap<Integer, Class>();
	
	public static String getComponentHotkeyString(Class cls) {
		try {
			String s = (String)cls.getField("_hotkey").get(null);
			return s;
		} catch (NoSuchFieldException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		}
	}
	
	public static int getComponentHotkeyVk(Class cls) {
		try {
			Integer s = (Integer)cls.getField("_hotkeyvk").get(null);
			return s;
		} catch (NoSuchFieldException e) {
			return -1;
		} catch (IllegalAccessException e) {
			return -1;
		}
	}
	
	public void addComponent(Class cls) {
		int vk = getComponentHotkeyVk (cls);
		if (vk > 0)
			hotKeySetVk(vk, cls);
	}
	
	public void hotKeySetNum (int idx, Class cls) {
		if (idx >=0 && idx <10)
			vk_to_component.put(KeyEvent.VK_0+idx, cls);		
	}

	public void hotKeySetVk (int vk, Class cls) {
		vk_to_component.put(vk, cls);
	}
	
	public Class hotKeyGet (int vk) {
		return vk_to_component.get(vk);		
	}
	
	public void hotKeyClear() {
		vk_to_component.clear();
	}
}
