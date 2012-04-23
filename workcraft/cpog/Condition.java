package workcraft.cpog;

import java.util.LinkedList;

import workcraft.logic.DNF;
import workcraft.logic.InvalidExpressionException;

public class Condition
{
	String string;
	
	private DNF dnf;	
	private boolean cached;
	private boolean cachedValue;
	private CPOGModel cpog;
	
	public Condition(String string, CPOGModel cpog)
	{
		this.cpog = cpog;
		setCondition(string);
	}
	
	public boolean setCondition(String string)
	{
		DNF dnf = new DNF();
		
		try
		{
			dnf.parseExpression(string);
		}
		catch (InvalidExpressionException e)
		{
			return false;
		}
		
		this.string = string;
		this.dnf = dnf;
		cached = false;
		
		return true;
	}
	
	public void refresh()
	{
		cached = false;		
	}
	
	boolean evaluate()
	{
		if (cached) return cachedValue;
		cached = true;
		cachedValue = dnf.evaluate(cpog.controlValues); 
		return cachedValue;
	}
}
