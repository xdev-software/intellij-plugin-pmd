package software.xdev.pmd.ui.toolwindow.node.other;

import java.util.function.Supplier;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.common.base.Suppliers;


public interface NodeErrorAdapter
{
	String summary();
	
	String allDetails();
	
	static ThrowableNodeErrorAdapter fromThrowable(final Throwable throwable)
	{
		return new ThrowableNodeErrorAdapter(throwable);
	}
	
	static SimpleNodeErrorAdapter fromString(final String s)
	{
		return new SimpleNodeErrorAdapter(s);
	}
	
	class ThrowableNodeErrorAdapter implements NodeErrorAdapter
	{
		private final String message;
		private final Supplier<String> allDetails;
		
		public ThrowableNodeErrorAdapter(final Throwable throwable)
		{
			this.message = throwable.getClass().getName() + ": " + throwable.getMessage();
			this.allDetails = Suppliers.memoize(() -> ExceptionUtils.getStackTrace(throwable));
		}
		
		@Override
		public String summary()
		{
			return this.message;
		}
		
		@Override
		public String allDetails()
		{
			return this.allDetails.get();
		}
	}
	
	
	record SimpleNodeErrorAdapter(String str) implements NodeErrorAdapter
	{
		@Override
		public String summary()
		{
			return this.str;
		}
		
		@Override
		public String allDetails()
		{
			return this.str;
		}
	}
}
