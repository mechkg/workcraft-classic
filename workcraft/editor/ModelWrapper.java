package workcraft.editor;

public class ModelWrapper {
	private Class model_class;
	
	public ModelWrapper(Class cls) {
		model_class = cls;
	}
	
	public Class getModelClass () {
		return model_class;		
	}
	
	public String toString() {
		String s = "INVALID MODEL";
		try {
			s = (String)model_class.getField("_displayname").get(null);
		}
		catch (NoSuchFieldException e) {
			
		}
		catch (IllegalAccessException t) {
			
		}
		return s;
	}
}