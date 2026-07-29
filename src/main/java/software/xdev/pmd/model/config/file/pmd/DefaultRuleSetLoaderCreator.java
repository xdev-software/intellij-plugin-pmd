package software.xdev.pmd.model.config.file.pmd;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.event.Level;

import com.intellij.openapi.diagnostic.Logger;

import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.lang.rule.RuleSetLoader;
import net.sourceforge.pmd.util.log.PmdReporter;
import net.sourceforge.pmd.util.log.internal.MessageReporterBase;


// Workaround for: https://github.com/pmd/pmd/issues/6912
public final class DefaultRuleSetLoaderCreator
{
	private static final Logger LOG = Logger.getInstance(DefaultRuleSetLoaderCreator.class);
	
	private static boolean reflectionInitialized;
	private static Method mWithReporter;
	
	static void initReflection()
	{
		if(reflectionInitialized)
		{
			return;
		}
		reflectionInitialized = true;
		
		try
		{
			mWithReporter = RuleSetLoader.class.getDeclaredMethod("withReporter", PmdReporter.class);
			mWithReporter.setAccessible(true);
		}
		catch(final Exception ex)
		{
			LOG.warn("Failed to get method 'withReporter'", ex);
		}
	}
	
	public static RuleSet createAndLoad(final Function<RuleSetLoader, RuleSet> loadFunc)
	{
		initReflection();
		
		final RuleSetLoader ruleSetLoader = new RuleSetLoader();
		
		final Optional<ErrorStoringPmdReporter> optErrorStoringPmdReporter = Optional.ofNullable(mWithReporter)
			.map(m -> {
				try
				{
					final ErrorStoringPmdReporter errorStoringPmdReporter = new ErrorStoringPmdReporter();
					m.invoke(ruleSetLoader, errorStoringPmdReporter);
					return errorStoringPmdReporter;
				}
				catch(final Exception ex)
				{
					LOG.warn("Failed to invoke 'withReporter'", ex);
					return null;
				}
			});
		
		try
		{
			return loadFunc.apply(ruleSetLoader);
		}
		catch(final RuntimeException rex)
		{
			optErrorStoringPmdReporter.ifPresent(r -> r.addErrorsAsSuppressedAndClear(rex));
			throw rex;
		}
	}
	
	private DefaultRuleSetLoaderCreator()
	{
	}
	
	static class ErrorStoringPmdReporter extends MessageReporterBase implements PmdReporter
	{
		private final List<Throwable> errors = new ArrayList<>();
		
		@Override
		protected boolean isLoggableImpl(final Level level)
		{
			return false;
		}
		
		@Override
		public void logEx(
			final Level level,
			@Nullable final String message,
			final Object[] formatArgs,
			@Nullable final Throwable error)
		{
			this.errors.add(error);
			super.logEx(level, message, formatArgs, error);
		}
		
		@Override
		protected void logImpl(final Level level, final String message)
		{
			// noop
		}
		
		public void addErrorsAsSuppressedAndClear(final Exception ex)
		{
			this.errors.forEach(ex::addSuppressed);
			this.errors.clear();
		}
	}
}
