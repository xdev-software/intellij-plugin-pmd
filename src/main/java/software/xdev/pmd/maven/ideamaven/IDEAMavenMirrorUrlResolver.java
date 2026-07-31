package software.xdev.pmd.maven.ideamaven;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jetbrains.idea.maven.utils.MavenEelUtil;
import org.jspecify.annotations.Nullable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.text.StringUtil;

import software.xdev.pmd.maven.resolve.mirror.MavenMirrorUrlResolver;


public class IDEAMavenMirrorUrlResolver implements MavenMirrorUrlResolver
{
	@Override
	public Optional<String> resolve(final Project project)
	{
		return Optional.ofNullable(MavenEelUtil.resolveUserSettingsPathBlocking(null, project))
			// Somehow some static methods of MavenUtil were converted to instance methods
			// in https://github.com/JetBrains/intellij-community/commit/cb47b90276da8ac0e9d51b635ee44596f42711f2
			// therefore making them no longer accessible because the class is now private
			.map(path -> getMirroredUrl(path, DEFAULT_CENTRAL_REPOSITORY_URL, "central"))
			.filter(s -> !DEFAULT_CENTRAL_REPOSITORY_URL.equals(s));
	}
	
	// region FromIDEA
	
	private static final List<String> SETTINGS_LIST_NAMESPACES = List.of(
		"http://maven.apache.org/SETTINGS/1.0.0",
		"http://maven.apache.org/SETTINGS/1.1.0",
		"http://maven.apache.org/SETTINGS/1.2.0"
	);
	
	@SuppressWarnings("java:S135")
	public static String getMirroredUrl(final Path settingsFile, final String url, final String id)
	{
		try
		{
			final Element mirrorParent = getElementWithRegardToNamespace(
				getDomRootElement(settingsFile), "mirrors", SETTINGS_LIST_NAMESPACES);
			if(mirrorParent == null)
			{
				return url;
			}
			
			final List<Element> mirrors =
				getElementsWithRegardToNamespace(mirrorParent, "mirror", SETTINGS_LIST_NAMESPACES);
			for(final Element el : mirrors)
			{
				final Element mirrorOfElement = getElementWithRegardToNamespace(
					el, "mirrorOf", SETTINGS_LIST_NAMESPACES);
				final Element mirrorUrlElement = getElementWithRegardToNamespace(
					el, "url", SETTINGS_LIST_NAMESPACES);
				if(mirrorOfElement == null)
				{
					continue;
				}
				if(mirrorUrlElement == null)
				{
					continue;
				}
				
				final String mirrorOf = mirrorOfElement.getTextTrim();
				final String mirrorUrl = mirrorUrlElement.getTextTrim();
				
				if(StringUtil.isEmptyOrSpaces(mirrorOf) || StringUtil.isEmptyOrSpaces(mirrorUrl))
				{
					continue;
				}
				
				if(isMirrorApplicable(mirrorOf, url, id))
				{
					return mirrorUrl;
				}
			}
		}
		catch(final Exception ignore)
		{
			// ignored
		}
		
		return url;
	}
	
	private static Element getDomRootElement(final Path file) throws IOException, JDOMException
	{
		return JDOMUtil.load(new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8));
	}
	
	private static @Nullable Element getElementWithRegardToNamespace(
		final Element parent,
		final String childName,
		final List<String> namespaces)
	{
		Element element = parent.getChild(childName);
		if(element != null)
		{
			return element;
		}
		for(final String namespace : namespaces)
		{
			element = parent.getChild(childName, Namespace.getNamespace(namespace));
			if(element != null)
			{
				return element;
			}
		}
		return null;
	}
	
	private static List<Element> getElementsWithRegardToNamespace(
		final Element parent,
		final String childrenName,
		final List<String> namespaces)
	{
		List<Element> elements = parent.getChildren(childrenName);
		if(!elements.isEmpty())
		{
			return elements;
		}
		for(final String namespace : namespaces)
		{
			elements = parent.getChildren(childrenName, Namespace.getNamespace(namespace));
			if(!elements.isEmpty())
			{
				return elements;
			}
		}
		return List.of();
	}
	
	@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
	private static boolean isMirrorApplicable(final String mirrorOf, final String url, final String id)
	{
		final Set<String> patterns = new HashSet<>(StringUtil.split(mirrorOf, ","));
		
		if(patterns.contains("!" + id))
		{
			return false;
		}
		
		if(patterns.contains("*") || patterns.contains(id))
		{
			return true;
		}
		if(patterns.contains("external:*"))
		{
			try
			{
				final URI uri = URI.create(url);
				return !"file".equals(uri.getScheme())
					&& !"localhost".equals(uri.getHost())
					&& !"127.0.0.1".equals(uri.getHost());
			}
			catch(final IllegalArgumentException e)
			{
				return false;
			}
		}
		return false;
	}
	
	// endregion
}
