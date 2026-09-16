package software.xdev.pmd.util.ep;

import java.util.Comparator;
import java.util.List;

import com.intellij.openapi.extensions.ExtensionPointName;


public class CachedOrderedExtensionPointContainer<T extends HasOrder>
{
	private final ExtensionPointName<T> epn;
	
	private List<T> lastSeen;
	private List<T> cachedOrdered;
	
	public CachedOrderedExtensionPointContainer(final String suffix)
	{
		this.epn = ExtensionPointName.create("software.xdev.pmd." + suffix);
	}
	
	public List<T> orderedEps()
	{
		final List<T> extensions = this.epn.getExtensionList();
		if(this.cachedOrdered == null || extensions != this.lastSeen)
		{
			this.cachedOrdered = extensions
				.stream()
				.sorted(Comparator.comparingInt(HasOrder::order))
				.toList();
			this.lastSeen = extensions;
		}
		return this.cachedOrdered;
	}
}
