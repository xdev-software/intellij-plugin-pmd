package software.xdev.pmd.ui.toolwindow.node;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import software.xdev.pmd.ui.toolwindow.node.has.HasErrorCount;
import software.xdev.pmd.ui.toolwindow.node.has.HasSuppressedViolationCount;
import software.xdev.pmd.ui.toolwindow.node.has.HasViolationCount;


public abstract class BaseHasViolationSuppressedErrorNode extends BaseNode
{
	protected int violationCount;
	protected int suppressedCount;
	protected int errorCount;
	
	@Override
	public void update()
	{
		this.violationCount =
			this.childrenSum(HasViolationCount.class, HasViolationCount::violationCount);
		this.suppressedCount =
			this.childrenSum(HasSuppressedViolationCount.class, HasSuppressedViolationCount::suppressedCount);
		this.errorCount =
			this.childrenSum(HasErrorCount.class, HasErrorCount::errorCount);
	}
	
	protected String violationsSuppressedErrorToString()
	{
		return Stream.of(
				Map.entry("violations", this.violationCount),
				Map.entry("suppressed", this.suppressedCount),
				Map.entry("errors", this.errorCount))
			.filter(e -> e.getValue() > 0)
			.map(e -> e.getValue() + "x " + e.getKey())
			.collect(Collectors.joining(", "));
	}
}
