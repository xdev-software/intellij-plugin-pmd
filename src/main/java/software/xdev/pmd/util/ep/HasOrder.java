package software.xdev.pmd.util.ep;

public interface HasOrder
{
	default int order()
	{
		return 1000;
	}
}
