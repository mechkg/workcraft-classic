package workcraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.zip.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;
import org.w3c.dom.Node;
import org.xml.sax.*;







public class PluginManager {
	private LinkedList<String> class_names;
	
	PluginManager() {
		class_names = new LinkedList<String>();
	}

	public void loadManifest(String path) {
		try
		{
			System.out.println("Loading plugin class manifest from "+path);

			File f = new File(path);

			if (!f.exists() || !f.isDirectory())
				throw new Exception ("Directory '" + f.getAbsolutePath()+ "' does not exist!");;

				File manifest = new File(path+File.separator+"manifest.xml");

				if (!manifest.exists() || !manifest.canRead())
					throw new Exception("File'" + manifest.getAbsolutePath()+ "' does not exist or is not accessible!");

				DocumentBuilderFactory factory =  DocumentBuilderFactory.newInstance();
				
				FileInputStream stream = new FileInputStream(manifest); 

				try {
					DocumentBuilder builder = factory.newDocumentBuilder();
					Document document = builder.parse(stream);
					Element el = document.getDocumentElement();
					
					if (el.getNodeName()!="manifest")
						throw new Exception("Incorrect manifest.xml format, expected <manifest> as document element, got <"+el.getNodeName()+">");

					NodeList nl = el.getChildNodes();

					for (int i=0;;i++) {
						Node n = nl.item(i);
						if (n==null)
							break;

						if (n.getNodeType()!=Node.ELEMENT_NODE)
							continue;
						if (n.getNodeName()!="class")
							continue;
						System.out.print('\t');
						NamedNodeMap attr = n.getAttributes();
						String cl = attr.getNamedItem("name").getNodeValue();

						if (cl==null)
							System.out.println("wrong class entry format, skipping");
						/* else
							System.out.println("class '"+cl+"'"); */
						
						class_names.add(cl);
					}
					System.out.println("Manifest load succeeded.");
				}
				catch (SAXParseException spe) {
					System.err.println("\tParse exception: "+spe.getMessage());				
				}
				catch (SAXException e)	{
					System.err.println("\tSAX exception: "+ e.getMessage());					
				}
				catch (ParserConfigurationException e) {
					System.err.println("\tParser configuration exception: " + e.getMessage());						
				}
				stream.close();
		}
		catch (Exception e) {
			System.err.println("Error loading manifest: "+e.getMessage());
			System.out.println("Manifest load failed. See errors tab for details.");
		}
	}
	
	public LinkedList<Class> getClassesByInterface(Class inter) {
		LinkedList<Class> list = new LinkedList<Class>();
		ClassLoader ldr = ClassLoader.getSystemClassLoader();
		
		for (String nm : class_names) {
			Class cls = null;
			try
			{
				cls = ldr.loadClass(nm);
			} catch (ClassNotFoundException e) {
		//		System.err.println("Class "+nm+", declared in manifest, cannot be loaded: "+e.getMessage());				
			}
			
			if (cls!=null)
				for (Class i : cls.getInterfaces())
					if (i == inter)
					{
						list.add(cls);
						break;
					}
		}
		
		return list;
	}
	
	public LinkedList<Class> getClassesBySuperclass(Class superclass) {
		LinkedList<Class> list = new LinkedList<Class>();
		ClassLoader ldr = ClassLoader.getSystemClassLoader();
		
		for (String nm : class_names) {
			Class cls = null;
			try
			{
				cls = ldr.loadClass(nm);
			} catch (ClassNotFoundException e) {
				System.err.println("Class "+nm+", declared in manifest, cannot be loaded: "+e.getMessage());				
			}
			
			if (cls!=null)
				if (superclass.isAssignableFrom(cls))
					list.add(cls);
		}
		return list;
	}	
}




/*	
 * This most probably doesn't work properly
 *  * * 
class MyClassLoader extends ClassLoader{

	JarFile jf = null;
	private byte[] loadClassData(String name) throws IllegalStateException, IOException, Exception, ZipException, SecurityException {
		if (jf==null)
			throw new Exception("Jar file not set for class loader");
		ZipEntry entry = jf.getEntry(name+".class");
		if (entry==null)
			throw new Exception("No such entry in Jar file: "+name+".class");

		InputStream stream = jf.getInputStream(entry);

		long size = entry.getSize();

		if (size>(long)Integer.MAX_VALUE)
			throw new Exception ("Entry size exceeds int o_Oa");

		int isize = (int)size;

		if (isize <= 0)
			throw new Exception("Unable to get Jar entry size");

		byte[] b = new byte[isize];

		try {
			stream.read(b, 0, isize);
		} 
		catch (NullPointerException e) {
			throw new Exception ("Null pointer exception");
		}

		return b;

		super.findSystemClass(name)
	}

	public void setJar(JarFile jf ) {
		this.jf = jf;		
	}

	public Class<?> findClass(String name) throws ClassNotFoundException{
		byte[] b;
		try{
			b = loadClassData(name);
		}
		catch (Exception e)
		{
			throw new ClassNotFoundException(e.getMessage());
		}
		return defineClass(name, b, 0, b.length);
	}
}


public class PluginManager {
	private Hashtable<String, LinkedList<Class>> plugins = null;
	private ClassLoader ldr = ClassLoader.getSystemClassLoader();

	public LinkedList<Class> getClassesByInterface(String interface_name)
	{
		if (plugins == null)
			return null;
		LinkedList<Class> list = plugins.get(interface_name);
		if (list != null)
			return (LinkedList<Class>)list.clone();
		else
			return null;
	}

	public void loadPlugins (String path) {

		if (plugins == null) 
			plugins = new Hashtable<String, LinkedList<Class>>();

		System.out.println("Lodaing plugins from "+path+"...");
		try
		{
			File f = new File(path);
			if (!f.exists())
				System.out.println("Directory '" + f.getAbsolutePath()+ "' does not exist!");

			String[] files = f.list();
			DocumentBuilderFactory factory =  DocumentBuilderFactory.newInstance();
			MyClassLoader ldr = new MyClassLoader();

			if (files!=null)	{
				for (String filename : files) {
					String fullpath = f.getAbsolutePath() + File.separator + filename;
					System.out.print (filename+"...... ");

					JarFile jf = null; ZipEntry entry=null;

					try
					{
						jf = new JarFile(fullpath);
						entry = jf.getEntry("plugin.xml");
						jf.entries();
					} catch (Exception e)
					{
						System.out.println("can't open JAR");
						System.err.println(e);
						continue;
					}

					if (entry==null)
					{
						System.out.println("no 'plugin.xml' file found inside the JAR");
						jf.close();
						continue;
					}

					InputStream stream = jf.getInputStream(entry);

					System.out.println();

					try {
						DocumentBuilder builder = factory.newDocumentBuilder();
						Document document = builder.parse(stream);
						Element el = document.getDocumentElement();

						if (el.getNodeName()!="wplugin")
							throw new java.lang.Exception("\tincorrect plugin.xml format, expected <wplugin> as document element, got <"+el.getNodeName()+">");
						NodeList nl = el.getChildNodes();

						ldr.setJar(jf);

						for (int i=0;;i++) {
							Node n = nl.item(i);
							if (n==null)
								break;

							if (n.getNodeType()!=Node.ELEMENT_NODE)
								continue;
							if (n.getNodeName()!="class")
								continue;
							if (i==0)
								System.out.println();
							System.out.print('\t');
							NamedNodeMap attr = n.getAttributes();
							String cl = attr.getNamedItem("name").getNodeValue();

							if (cl==null)
								System.out.println("Wrong class entry format, skipping");
							else
								System.out.print("Loading class "+cl +" ...... ");

							Class cls = null;

							try
							{
								cls = ldr.loadClass(cl);
							}
							catch (ClassNotFoundException e)
							{
								System.out.println("FAILED");
								System.out.println("\t\t"+e.getMessage());
								continue;
							}

							if (cls==null)
								continue;

							System.out.print("OK (interfaces: ");

							Class[] interfaces = cls.getInterfaces();

							if (interfaces == null || interfaces.length == 0) {
								System.out.println("none");
								continue;
							}

							boolean putcomma = false;
							for (Class interf : interfaces) {
								LinkedList<Class> list = plugins.get(interf.getName());

								if (list==null) {
									LinkedList<Class> newlist = new LinkedList<Class>();

									newlist.add(cls);
									plugins.put(interf.getName(), newlist);
									if (putcomma) {
										System.out.print(", ");										   
									} else {
										putcomma = true;
									}
									System.out.print(interf.getName());
								} else  {
									list.add(cls);

									if (putcomma) {
										System.out.print(", ");										   
									} else {
										putcomma = true;
									}
									System.out.print(interf.getName());
								}
							}
							System.out.println(")");
						}
					} 
					catch (SAXParseException spe) {
						System.out.println("\tParse exception: "+spe.getMessage());						
					}
					catch (SAXException e)	{
						System.out.println("\tSAX exception: "+ e.getMessage());					
					}
					catch (ParserConfigurationException e) {
						System.out.println("\tParser configuration exception: " + e.getMessage());						
					}
					catch (java.lang.Exception e) {
						System.out.println(e);												
					}
					stream.close();
					jf.close();
				}
			}
			else {
				System.out.println ("Directory" + f.getAbsolutePath() +" is empty.");		    
			}
		}
		catch (IOException e) {
			System.err.println(e);
		}
	}
}
 */