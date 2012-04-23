package workcraft.mg;

public class GaussianDelay
{
	public double mean;
	public double dev;
	public GaussianDelay()
	{
		mean = 1.0;
		dev = 0.0;
	}
	public GaussianDelay(double mean, double dev)
	{
		this.mean = mean;
		this.dev = dev;
	}
	public GaussianDelay(GaussianDelay delay)
	{
		this.mean = delay.mean;
		this.dev = delay.dev;
	}
	
};
