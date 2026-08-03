package software.xdev.pmd.model.config.rulesetlocation.file.pmd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.intellij.openapi.diagnostic.Logger;

import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.lang.rule.RuleSetLoader;
import net.sourceforge.pmd.lang.rule.internal.RuleSetReferenceId;
import net.sourceforge.pmd.util.internal.ResourceLoader;


// Workaround for: https://github.com/pmd/pmd/issues/6913
public final class LoadFromStringRuleSetLoaderWorkaround
{
	private static final Logger LOG = Logger.getInstance(LoadFromStringRuleSetLoaderWorkaround.class);
	
	private static boolean reflectionInitialized;
	static boolean reflectionUsable;
	
	private static Field fResourceLoader;
	private static Method mLoadFromResource;
	private static Field resourceLoaderFClassLoader;
	
	static void initReflection()
	{
		if(reflectionInitialized)
		{
			return;
		}
		reflectionInitialized = true;
		
		try
		{
			fResourceLoader = RuleSetLoader.class.getDeclaredField("resourceLoader");
			fResourceLoader.setAccessible(true);
		}
		catch(final Exception ex)
		{
			LOG.warn("Failed to get field 'resourceLoader'", ex);
		}
		
		try
		{
			mLoadFromResource = RuleSetLoader.class.getDeclaredMethod(
				"loadFromResource", RuleSetReferenceId.class);
			mLoadFromResource.setAccessible(true);
		}
		catch(final Exception ex)
		{
			LOG.warn("Failed to get method 'loadFromResource'", ex);
		}
		
		try
		{
			resourceLoaderFClassLoader = ResourceLoader.class.getDeclaredField("classLoader");
			resourceLoaderFClassLoader.setAccessible(true);
		}
		catch(final Exception ex)
		{
			LOG.warn("Failed to get method 'classLoader'", ex);
		}
		
		reflectionUsable = fResourceLoader != null
			&& mLoadFromResource != null
			&& resourceLoaderFClassLoader != null;
	}
	
	public static RuleSet loadFromString(
		final RuleSetLoader ruleSetLoader,
		final String filename,
		final String rulesetXmlContent)
	{
		initReflection();
		
		if(reflectionUsable)
		{
			try
			{
				if(filename == null || filename.isEmpty())
				{
					throw new IllegalArgumentException("Invalid empty filename");
				}
				
				final ResourceLoader oldLoader = (ResourceLoader)fResourceLoader.get(ruleSetLoader);
				final ClassLoader oldClassLoader = (ClassLoader)resourceLoaderFClassLoader.get(oldLoader);
				
				try
				{
					fResourceLoader.set(
						ruleSetLoader,
						new ResourceLoader(oldClassLoader)
						{
							@Override
							public @NonNull InputStream loadResourceAsStream(final String name) throws IOException
							{
								if(Objects.equals(name, filename))
								{
									return new ByteArrayInputStream(rulesetXmlContent.getBytes(StandardCharsets.UTF_8));
								}
								return oldLoader.loadResourceAsStream(name);
							}
						});
					return (RuleSet)mLoadFromResource.invoke(
						ruleSetLoader,
						new RuleSetReferenceId(filename, null));
				}
				finally
				{
					fResourceLoader.set(ruleSetLoader, oldLoader);
				}
			}
			catch(final IllegalAccessException | InvocationTargetException ex)
			{
				LOG.warn("Failed to invoke loadFromString workaround", ex);
			}
		}
		
		// Fallback
		return ruleSetLoader.loadFromString(filename, rulesetXmlContent);
	}
	
	private LoadFromStringRuleSetLoaderWorkaround()
	{
	}
}
