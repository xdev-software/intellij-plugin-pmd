package software.xdev.pmd.maven.ideamaven;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import fleet.util.GlobKt;


public final class GlobToRegex
{
	private static BiConsumer<String, StringBuilder> convertGlobToRegexFuncCache;
	
	@SuppressWarnings("PMD.AvoidStringBuilderOrBuffer") // API forces use of StringBuilder
	public static String toRegex(final String glob)
	{
		final StringBuilder sb = new StringBuilder(glob.length() * 2);
		convertGlobToRegexFunc().accept(glob, sb);
		return sb.toString();
	}
	
	private static BiConsumer<String, StringBuilder> convertGlobToRegexFunc()
	{
		if(convertGlobToRegexFuncCache == null)
		{
			convertGlobToRegexFuncCache = initInvokeFunc();
		}
		return convertGlobToRegexFuncCache;
	}
	
	private static BiConsumer<String, StringBuilder> initInvokeFunc()
	{
		try
		{
			// > 263
			return tryBuildInvokeFunc(
				(glob, sb) -> new Object[]{glob, new ArrayList<int[]>(), sb, '/'},
				String.class, List.class, StringBuilder.class, char.class);
		}
		catch(final NoSuchMethodException _)
		{
			try
			{
				// <= 262
				return tryBuildInvokeFunc(
					(glob, sb) -> new Object[]{glob, new ArrayList<int[]>(), sb},
					String.class, List.class, StringBuilder.class);
			}
			catch(final NoSuchMethodException ex)
			{
				throw new IllegalStateException("Failed to determine invoke function", ex);
			}
		}
	}
	
	private static BiConsumer<String, StringBuilder> tryBuildInvokeFunc(
		final BiFunction<String, StringBuilder, Object[]> argsProvider,
		final Class<?>... parameterTypes)
		throws NoSuchMethodException
	{
		final Method mConvertGlobToRegEx = GlobKt.class.getDeclaredMethod(
			"convertGlobToRegEx",
			parameterTypes);
		mConvertGlobToRegEx.setAccessible(true);
		return (glob, sb) -> {
			try
			{
				mConvertGlobToRegEx.invoke(null, argsProvider.apply(glob, sb));
			}
			catch(final IllegalAccessException | InvocationTargetException e)
			{
				throw new IllegalStateException("Failed to invoke", e);
			}
		};
	}
	
	private GlobToRegex()
	{
	}
}
