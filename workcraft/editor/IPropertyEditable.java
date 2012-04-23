package workcraft.editor;
import java.util.Properties;

public interface IPropertyEditable {
	public Properties getProperties();
	public void setProperties(Properties p);
	public String getProperty(String name);
	public void setProperty(String name, String value);
}