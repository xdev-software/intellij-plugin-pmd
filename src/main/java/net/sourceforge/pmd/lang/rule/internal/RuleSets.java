/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.rule.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sourceforge.pmd.benchmark.TimeTracker;
import net.sourceforge.pmd.benchmark.TimedOperation;
import net.sourceforge.pmd.benchmark.TimedOperationCategory;
import net.sourceforge.pmd.lang.LanguageProcessor;
import net.sourceforge.pmd.lang.LanguageProcessorRegistry;
import net.sourceforge.pmd.lang.ast.RootNode;
import net.sourceforge.pmd.lang.document.TextFile;
import net.sourceforge.pmd.lang.rule.InternalApiBridge;
import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.lang.rule.RuleReference;
import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.lang.rule.RuleSet.RuleSetBuilder;
import net.sourceforge.pmd.lang.rule.impl.UnnecessaryPmdSuppressionRule;
import net.sourceforge.pmd.reporting.FileAnalysisListener;
import net.sourceforge.pmd.util.log.PmdReporter;


/**
 * Fork/Override of upstream to fix some performance problems. See IMPROVED comments for details
 * <p>
 * Based on PMD 7.17.0
 */
@SuppressWarnings("all")
public class RuleSets
{
	// IMPROVED
	private final RuleSetsPerformanceImprover performanceImprover = new RuleSetsPerformanceImprover();
	
	private final List<RuleSet> ruleSets;
	private RuleApplicator ruleApplicator;
	
	public RuleSets(final RuleSets ruleSets)
	{
		final List<RuleSet> rsets = new ArrayList<>();
		for(final RuleSet rs : ruleSets.ruleSets)
		{
			// IMPROVED
			// Copy RuleSet but keep already initalized rules so that they don't need to be initialized again!
			rsets.add(rs.toBuilder().build());
		}
		this.ruleSets = Collections.unmodifiableList(rsets);
	}
	
	public RuleSets(final Collection<? extends RuleSet> ruleSets)
	{
		final List<RuleSet> rulesets = new ArrayList<>();
		final List<RuleSet> suppressionRules = new ArrayList<>();

        /*
         Suppression rules are separated because they must be run last.
         They are packed into their own rulesets. and added at the end of the ruleset list.
         */
		
		for(final RuleSet ruleSet : ruleSets)
		{
			final RuleSetBuilder noSuppressions = ruleSet.toBuilder();
			final RuleSetBuilder onlySuppressions = ruleSet.toBuilder();
			
			noSuppressions.removeIf(rule1 -> followReference(rule1) instanceof UnnecessaryPmdSuppressionRule);
			onlySuppressions.removeIf(rule1 -> !(followReference(rule1) instanceof UnnecessaryPmdSuppressionRule));
			rulesets.add(noSuppressions.build());
			suppressionRules.add(onlySuppressions.build());
		}
		rulesets.addAll(suppressionRules);
		this.ruleSets = Collections.unmodifiableList(rulesets);
	}
	
	private static Rule followReference(final Rule rule)
	{
		if(rule instanceof RuleReference)
		{
			return followReference(((RuleReference)rule).getRule());
		}
		return rule;
	}
	
	public RuleSets(final RuleSet ruleSet)
	{
		this.ruleSets = Collections.singletonList(ruleSet);
	}
	
	public void initializeRules(final LanguageProcessorRegistry lpReg, final PmdReporter reporter)
	{
		// this is abusing the mutability of RuleSet, will go away eventually.
		for(final RuleSet rset : this.ruleSets)
		{
			for(final Iterator<Rule> iterator = rset.getRules().iterator(); iterator.hasNext(); )
			{
				final Rule rule = iterator.next();
				
				try
				{
					// IMPROVED
					final LanguageProcessor lp = lpReg.getProcessor(rule.getLanguage());
					if(performanceImprover.shouldRuleInitializationBeSkipped(rule, lp))
					{
						continue;
					}
					rule.initialize(lp);
				}
				catch(final Exception e)
				{
					reporter.errorEx(
						"Exception while initializing rule " + rule.getName() + ", the rule will not be run", e);
					iterator.remove();
				}
			}
		}
	}
	
	private RuleApplicator prepareApplicator()
	{
		return RuleApplicator.build(this.ruleSets.stream().flatMap(it -> it.getRules().stream())::iterator);
	}
	
	public RuleSet[] getAllRuleSets()
	{
		return this.ruleSets.toArray(new RuleSet[0]);
	}
	
	// internal
	List<RuleSet> getRuleSetsInternal()
	{
		return this.ruleSets;
	}
	
	public Iterator<RuleSet> getRuleSetsIterator()
	{
		return this.ruleSets.iterator();
	}
	
	public Set<Rule> getAllRules()
	{
		final Set<Rule> result = new HashSet<>();
		for(final RuleSet r : this.ruleSets)
		{
			result.addAll(r.getRules());
		}
		return result;
	}
	
	public boolean applies(final TextFile file)
	{
		for(final RuleSet ruleSet : this.ruleSets)
		{
			if(InternalApiBridge.ruleSetApplies(ruleSet, file.getFileId()))
			{
				return true;
			}
		}
		return false;
	}
	
	public void apply(final RootNode root, final FileAnalysisListener listener)
	{
		if(this.ruleApplicator == null)
		{
			// initialize here instead of ctor, because some rules properties
			// are set after creating the ruleset, and jaxen xpath queries
			// initialize their XPath expressions when calling getRuleChainVisits()... fixme
			this.ruleApplicator = this.prepareApplicator();
		}
		
		try(final TimedOperation ignored = TimeTracker.startOperation(TimedOperationCategory.RULE_AST_INDEXATION))
		{
			this.ruleApplicator.index(root);
		}
		
		for(final RuleSet ruleSet : this.ruleSets)
		{
			if(InternalApiBridge.ruleSetApplies(ruleSet, root.getTextDocument().getFileId()))
			{
				this.ruleApplicator.apply(ruleSet.getRules(), listener);
			}
		}
	}
	
	public Rule getRuleByName(final String ruleName)
	{
		Rule rule = null;
		for(final Iterator<RuleSet> i = this.ruleSets.iterator(); i.hasNext() && rule == null; )
		{
			final RuleSet ruleSet = i.next();
			rule = ruleSet.getRuleByName(ruleName);
		}
		return rule;
	}
	
	public int ruleCount()
	{
		int count = 0;
		for(final RuleSet r : this.ruleSets)
		{
			count += r.getRules().size();
		}
		return count;
	}
	
	public void removeDysfunctionalRules(final Collection<Rule> collector)
	{
		for(final RuleSet ruleSet : this.ruleSets)
		{
			ruleSet.removeDysfunctionalRules(collector);
		}
	}
	
	public long getChecksum()
	{
		long checksum = 1;
		for(final RuleSet ruleSet : this.ruleSets)
		{
			checksum = checksum * 31 + ruleSet.getChecksum();
		}
		return checksum;
	}
}
