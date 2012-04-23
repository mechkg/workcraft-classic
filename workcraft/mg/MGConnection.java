package workcraft.mg;

import workcraft.common.DefaultConnection;
import workcraft.editor.BasicEditable;

public class MGConnection extends DefaultConnection
{
	protected boolean critical = false;
	protected static boolean show_critical = true;
	public MGConnection()
	{
		super();
		// TODO Auto-generated constructor stub
	}
	public MGConnection(BasicEditable first, BasicEditable second)
	{
		super(first, second);
		// TODO Auto-generated constructor stub
	}
}
