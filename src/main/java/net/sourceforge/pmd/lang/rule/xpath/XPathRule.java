/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.rule.xpath;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.pmd.lang.LanguageProcessor;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.rule.AbstractRule;
import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.lang.rule.RuleTargetSelector;
import net.sourceforge.pmd.lang.rule.xpath.impl.XPathHandler;
import net.sourceforge.pmd.lang.rule.xpath.internal.DeprecatedAttrLogger;
import net.sourceforge.pmd.lang.rule.xpath.internal.SaxonXPathRuleQuery;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.properties.PropertyFactory;
import net.sourceforge.pmd.reporting.RuleContext;
import net.sourceforge.pmd.util.IteratorUtil;


/**
 * Fork/Override of upstream to improve performance. See IMPROVED comments for details.
 * <p>
 * Based on PMD 7.27.0
 */
@SuppressWarnings("all")
public final class XPathRule extends AbstractRule
{
	private static final Logger LOG = LoggerFactory.getLogger(XPathRule.class);
	
	private static final PropertyDescriptor<String> XPATH_DESCRIPTOR =
		PropertyFactory.stringProperty("xpath")
			.desc("XPath expression")
			.defaultValue("")
			.build();
	
	private SaxonXPathRuleQuery xpathRuleQuery;
	
	private DeprecatedAttrLogger attrLogger = DeprecatedAttrLogger.create(this);
	
	XPathRule()
	{
		this.definePropertyDescriptor(XPATH_DESCRIPTOR);
	}
	
	// IMPROVED - Removed deprecated constructor
	
	@Override
	public Rule deepCopy()
	{
		final XPathRule rule = (XPathRule)super.deepCopy();
		rule.attrLogger = this.attrLogger;
		// IMPROVED - Also copy xpathRuleQuery
		rule.xpathRuleQuery = this.xpathRuleQuery;
		return rule;
	}
	
	public String getXPathExpression()
	{
		return this.getProperty(XPATH_DESCRIPTOR);
	}
	
	@Override
	public void apply(final Node target, final RuleContext ctx)
	{
		final SaxonXPathRuleQuery query = this.getQueryMaybeInitialize();
		
		final List<Node> nodesWithViolation;
		try
		{
			nodesWithViolation = query.evaluate(target);
		}
		catch(final PmdXPathException e)
		{
			throw this.addExceptionContext(e);
		}
		
		for(final Node nodeWithViolation : nodesWithViolation)
		{
			// see Deprecate getImage/@Image #4787 https://github.com/pmd/pmd/issues/4787
			String messageArg = nodeWithViolation.getImage();
			// Nodes might already have been refactored to not use getImage anymore.
			// Therefore, try several other common names
			if(messageArg == null)
			{
				messageArg =
					this.getFirstMessageArgFromNode(nodeWithViolation, "Name", "SimpleName", "MethodName", "Value");
			}
			ctx.addViolation(nodeWithViolation, messageArg);
		}
	}
	
	private String getFirstMessageArgFromNode(final Node node, final String... attributeNames)
	{
		final List<String> nameList = Arrays.asList(attributeNames);
		return IteratorUtil.toStream(node.getXPathAttributesIterator())
			.filter(a -> nameList.contains(a.getName()))
			.findFirst()
			.map(Attribute::getStringValue)
			.orElse(null);
	}
	
	private ContextedRuntimeException addExceptionContext(final PmdXPathException e)
	{
		return e.addRuleName(this.getName());
	}
	
	@Override
	public void initialize(final LanguageProcessor languageProcessor)
	{
		// IMPROVED - Check if xPathRuleQuery is identical
		final XPathHandler currentXPathHandler = languageProcessor.services().getXPathHandler();
		if(xpathRuleQuery != null && xpathRuleQuery.getxPathHandler().equals(currentXPathHandler))
		{
			return;
		}
		
		final String xpath = this.getXPathExpression();
		
		try
		{
			this.xpathRuleQuery = new SaxonXPathRuleQuery(
				xpath,
				XPathVersion.DEFAULT,
				this.getPropertiesByPropertyDescriptor(),
				currentXPathHandler,
				this.attrLogger);
		}
		catch(final PmdXPathException e)
		{
			throw this.addExceptionContext(e);
		}
	}
	
	private SaxonXPathRuleQuery getQueryMaybeInitialize() throws PmdXPathException
	{
		if(this.xpathRuleQuery == null)
		{
			throw new IllegalStateException("Not initialized");
		}
		return this.xpathRuleQuery;
	}
	
	@Override
	protected @NonNull RuleTargetSelector buildTargetSelector()
	{
		final List<String> visits = this.getQueryMaybeInitialize().getRuleChainVisits();
		
		this.logXPathRuleChainUsage(!visits.isEmpty());
		
		return visits.isEmpty() ? RuleTargetSelector.forRootOnly()
			: RuleTargetSelector.forXPathNames(visits);
	}
	
	private void logXPathRuleChainUsage(final boolean usesRuleChain)
	{
		LOG.debug(
			"{} rule chain for XPath rule: {} ({})",
			usesRuleChain ? "Using" : "no",
			this.getName(),
			this.getRuleSetName());
	}
	
	@Override
	public String dysfunctionReason()
	{
		if(StringUtils.isBlank(this.getXPathExpression()))
		{
			return "Missing XPath expression";
		}
		return null;
	}
}
