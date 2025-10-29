package software.xdev.pmd.markdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter;
import com.intellij.openapi.project.Project;

import net.sourceforge.pmd.PMDVersion;
import software.xdev.pmd.external.org.apache.shiro.lang.util.SoftHashMap;


public class RuleDescriptionDocMarkdownToHtmlService
{
	private static final Pattern PMD_JDOC = Pattern.compile("\\{% jdoc ([a-z!]*)::([A-Za-z0-9\\._#]*) %\\}");
	private static final Pattern PMD_RULE = Pattern.compile("\\{% rule \"([A-Za-z0-9\\.\\/_#]*)\" %\\}");
	private static final Pattern INCORRECTLY_DEFINED_LINK = Pattern.compile("<(https:\\/\\/.*)>");
	
	private final Project project;
	
	private final Map<String, String> cache = Collections.synchronizedMap(new SoftHashMap<>());
	
	public RuleDescriptionDocMarkdownToHtmlService(@NotNull final Project project)
	{
		this.project = project;
	}
	
	public String mdToHtml(final String md)
	{
		return this.cache.computeIfAbsent(
			md,
			this::mdToHtmlInternal);
	}
	
	@SuppressWarnings("checkstyle:FinalParameters")
	private String mdToHtmlInternal(String md)
	{
		// Try to detect invalid markdown indentations
		// in this case most lines have multiple whitespaces (space or tabs) in front
		// To fix this we need to detect these whitespaces and remove them
		md = this.fixInvalidMdIndentations(md);
		
		// Incorrectly defined link
		if(md.contains("<https://"))
		{
			md = INCORRECTLY_DEFINED_LINK.matcher(md).replaceAll(res -> {
				final String match = res.group(1);
				return "[" + match + "](" + res.group(1) + ")";
			});
		}
		
		// Check for references to doc
		// https://pmd.github.io/pmd/pmd_devdocs_writing_documentation.html#custom-liquid-tags
		if(md.contains("{%"))
		{
			md = this.patchPMDLiquidTags(md);
		}
		
		String html = DocMarkdownToHtmlConverter.convert(this.project, md.trim());
		
		if(html.startsWith("<p>") && html.length() > 3)
		{
			html = html.substring(3);
		}
		
		return html.trim();
	}
	
	@SuppressWarnings("checkstyle:FinalParameters")
	private String fixInvalidMdIndentations(String md)
	{
		final int firstNewLine = md.indexOf('\n');
		if(firstNewLine != -1)
		{
			// Detect whitespaces and build global replacement sequence
			final List<Character> replaceSeq = new ArrayList<>();
			for(int currentIndex = firstNewLine; currentIndex < md.length(); currentIndex++)
			{
				final char currentChar = md.charAt(currentIndex);
				if(currentChar == ' ' || currentChar == '\t')
				{
					replaceSeq.add(currentChar);
				}
				// Continue when: We are still at the beginning and encounter additional \n
				// Otherwise break
				else if(!(currentChar == '\n' && replaceSeq.isEmpty()))
				{
					break;
				}
			}
			if(!replaceSeq.isEmpty())
			{
				md = md.replaceAll(
					"\n" + replaceSeq.stream().map(String::valueOf).collect(Collectors.joining()),
					"\n");
			}
		}
		return md;
	}
	
	@SuppressWarnings({"checkstyle:FinalParameters", "java:S125"})
	private String patchPMDLiquidTags(String md)
	{
		// {% jdoc java::lang.java.metrics.JavaMetrics#CYCLO %}
		// {% jdoc !q!core::lang.rule.Rule %}
		if(md.contains("{% jdoc"))
		{
			md = PMD_JDOC.matcher(md).replaceAll(r -> this.replacePMDNativeJavaDoc(r.group(1), r.group(2)));
		}
		// {% rule "java/codestyle/LinguisticNaming" %}
		if(md.contains("{% rule"))
		{
			md = PMD_RULE.matcher(md).replaceAll(r -> "`" + r.group(1) + "`");
		}
		return md;
	}
	
	private String replacePMDNativeJavaDoc(
		final String classifierAndPmdLanguage,
		final String shortenedClassNameAndAnchor)
	{
		final int clPmdLangLastExclamationMark = classifierAndPmdLanguage.lastIndexOf('!');
		final String pmdLang =
			clPmdLangLastExclamationMark != -1 && clPmdLangLastExclamationMark + 1 < classifierAndPmdLanguage.length()
				? classifierAndPmdLanguage.substring(clPmdLangLastExclamationMark + 1)
				: classifierAndPmdLanguage;
		
		final String[] elements = shortenedClassNameAndAnchor.split("#", 2);
		
		final String anchor = elements.length == 2 ? "#" + elements[1] : "";
		
		final String shortenedClassName = elements[0];
		final int classNameLastDot = shortenedClassName.lastIndexOf('.');
		final String className = classNameLastDot != -1 && classNameLastDot + 1 < shortenedClassName.length()
			? shortenedClassName.substring(classNameLastDot + 1)
			: shortenedClassName;
		
		return "["
			+ className
			+ anchor
			+ "]"
			+ "(https://docs.pmd-code.org/apidocs/pmd-"
			+ pmdLang
			+ "/"
			+ PMDVersion.VERSION
			+ "/net/sourceforge/pmd/"
			+ shortenedClassName.replace('.', '/') + ".html"
			+ anchor
			+ ")";
	}
}
