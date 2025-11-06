package software.xdev.pmd.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.intellij.openapi.command.impl.DummyProject;

import net.sourceforge.pmd.PMDVersion;


class RuleDescriptionDocMarkdownToHtmlServiceTest
{
	@Test
	@DisplayName("Basic MD to HTML Check")
	void basicMDToHTMLCheck()
	{
		final RuleDescriptionDocMarkdownToHtmlService ruleDescriptionDocMarkdownToHtmlService =
			new RuleDescriptionDocMarkdownToHtmlService(DummyProject.getInstance());
		
		assertEquals(
			"<a href=\"https://example.org\">https://example.org</a><p>"
				+ "<a href=\"https://docs.pmd-code.org/apidocs/pmd-core/" + PMDVersion.VERSION
				+ "/net/sourceforge/pmd/lang/rule/Rule"
				+ ".html\">Rule</a>",
			ruleDescriptionDocMarkdownToHtmlService.mdToHtml("""
				<https://example.org>
				
				{% jdoc core::lang.rule.Rule %}
				""")
		);
	}
}
