package workcraft.ADC;

public class ADCToken
{
	public boolean		valid;
	public int			time;
	
	public ADCToken()
	{
		valid = false;
		time = 0;
	}

	public ADCToken(boolean valid, int time)
	{
		this.valid = valid;
		this.time = time;
	}
	
	public ADCToken(ADCToken token)
	{
		valid = token.valid;
		time = token.time;
	}
}
