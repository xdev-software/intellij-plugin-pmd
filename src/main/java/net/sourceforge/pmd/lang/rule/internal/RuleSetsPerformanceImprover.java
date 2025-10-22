package net.sourceforge.pmd.lang.rule.internal;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.pmd.lang.LanguageProcessor;
import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.lang.rule.RuleReference;
import net.sourceforge.pmd.lang.rule.xpath.XPathRule;
import net.sourceforge.pmd.lang.rule.xpath.impl.XPathHandler;
import net.sourceforge.pmd.lang.rule.xpath.internal.SaxonXPathRuleQuery;


public class RuleSetsPerformanceImprover
{
	private static final Logger LOG = LoggerFactory.getLogger(RuleSetsPerformanceImprover.class);
	
	private final Map<FieldCacheKey, Field> cachedFields = new ConcurrentHashMap<>();
	
	public boolean shouldRuleInitializationBeSkipped(final Rule rule, final LanguageProcessor lp)
	{
		final Rule unwrappedRule = this.unwrapRule(rule);
		if(!(unwrappedRule instanceof final XPathRule xPathRule))
		{
			return false;
		}
		
		final SaxonXPathRuleQuery xPathRuleQuery =
			this.getFromField(XPathRule.class, "xpathRuleQuery", xPathRule, SaxonXPathRuleQuery.class);
		if(xPathRuleQuery == null)
		{
			return false;
		}
		
		final XPathHandler previousXPathHandler =
			this.getFromField(SaxonXPathRuleQuery.class, "xPathHandler", xPathRuleQuery, XPathHandler.class);
		if(previousXPathHandler == null)
		{
			return false;
		}
		
		// If the xPathHandler is identical to the already initialized one, initialization can be skipped
		return previousXPathHandler.equals(lp.services().getXPathHandler());
	}
	
	private Rule unwrapRule(final Rule rule)
	{
		final Set<Rule> alreadyVisited = new HashSet<>(Set.of(rule));
		Rule currentRule = rule;
		for(int i = 0; currentRule instanceof final RuleReference ref && i < 1000; i++)
		{
			currentRule = ref.getRule();
			// Loop detection
			if(!alreadyVisited.add(currentRule))
			{
				return currentRule;
			}
		}
		return currentRule;
	}
	
	private <T> T getFromField(
		final Class<?> clazz,
		final String fieldName,
		final Object toExtractFrom,
		final Class<T> expectedType)
	{
		final Field field = this.cachedFields.computeIfAbsent(
			new FieldCacheKey(clazz, fieldName), k -> {
				try
				{
					final Field field1 = clazz.getDeclaredField(fieldName);
					field1.setAccessible(true);
					return field1;
				}
				catch(final NoSuchFieldException e)
				{
					LOG.warn("Failed to find field[clazz={},fieldName={}]", clazz, fieldName, e);
					return null;
				}
			});
		if(field == null)
		{
			return null;
		}
		
		try
		{
			final Object value = field.get(toExtractFrom);
			return value != null ? expectedType.cast(value) : null;
		}
		catch(final Exception e)
		{
			LOG.warn("Failed to access field[{}]", field, e);
			return null;
		}
	}
	
	record FieldCacheKey(Class<?> clazz, String fieldName)
	{
	}
}
