/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.rule.xpath.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.LocalVariableReference;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.AtomicSequence;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceUri;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.sxpath.IndependentContext;
import net.sf.saxon.sxpath.XPathDynamicContext;
import net.sf.saxon.sxpath.XPathEvaluator;
import net.sf.saxon.sxpath.XPathExpression;
import net.sf.saxon.sxpath.XPathVariable;
import net.sf.saxon.trans.UncheckedXPathException;
import net.sf.saxon.trans.XPathException;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.RootNode;
import net.sourceforge.pmd.lang.rule.xpath.PmdXPathException;
import net.sourceforge.pmd.lang.rule.xpath.PmdXPathException.Phase;
import net.sourceforge.pmd.lang.rule.xpath.XPathVersion;
import net.sourceforge.pmd.lang.rule.xpath.impl.XPathFunctionDefinition;
import net.sourceforge.pmd.lang.rule.xpath.impl.XPathHandler;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.util.DataMap;
import net.sourceforge.pmd.util.DataMap.SimpleDataKey;


/**
 * Fork/Override of upstream to fix some performance problems. See IMPROVED comments for details
 * <p>
 * Based on PMD 7.17.0
 */
@SuppressWarnings("all")
public class SaxonXPathRuleQuery
{
	// IMPROVED
	// Different XPathHandlers have different extensions (e.g. pmd-java) that are registered with configuration
	private static Map<XPathHandler, CachedInitData> cachedInitData = Collections.synchronizedMap(new WeakHashMap<>());
	
	
	record CachedInitData(
		Configuration configuration,
		Map<String, NamespaceUri> namespacesToDeclare)
	{
	}
	
	private static CachedInitData getOrCreateCachedInitData(final XPathHandler xPathHandler)
	{
		return cachedInitData.computeIfAbsent(
			xPathHandler, ignored -> {
				final Configuration config = Configuration.newConfiguration();
				config.setNamePool(getNamePool());
				
				final Set<XPathFunctionDefinition> registeredExtensionFunctions =
					xPathHandler.getRegisteredExtensionFunctions();
				final Map<String, NamespaceUri> namespacesToDeclare =
					new LinkedHashMap<>(registeredExtensionFunctions.size());
				for(final XPathFunctionDefinition xpathFun : registeredExtensionFunctions)
				{
					final ExtensionFunctionDefinition fun = new SaxonExtensionFunctionDefinitionAdapter(xpathFun);
					final StructuredQName qname = fun.getFunctionQName();
					namespacesToDeclare.put(qname.getPrefix(), qname.getNamespaceUri());
					config.registerExtensionFunction(fun);
				}
				
				return new CachedInitData(config, namespacesToDeclare);
			});
	}
	
	static final String AST_ROOT = "_AST_ROOT_";
	
	private static final Logger LOG = LoggerFactory.getLogger(SaxonXPathRuleQuery.class);
	
	private static final NamePool NAME_POOL = new NamePool();
	
	private static final SimpleDataKey<AstTreeInfo> SAXON_TREE_CACHE_KEY = DataMap.simpleDataKey("saxon.tree");
	
	private final String xpathExpr;
	private final XPathVersion version;
	private final Map<PropertyDescriptor<?>, Object> properties;
	private final XPathHandler xPathHandler;
	private final List<String> rulechainQueries = new ArrayList<>();
	private Configuration configuration;
	
	Map<String, List<Expression>> nodeNameToXPaths = new HashMap<>();
	
	XPathExpression xpathExpression;
	
	private final DeprecatedAttrLogger attrCtx;
	
	public SaxonXPathRuleQuery(
		final String xpathExpr,
		final XPathVersion version,
		final Map<PropertyDescriptor<?>, Object> properties,
		final XPathHandler xPathHandler,
		final DeprecatedAttrLogger logger) throws PmdXPathException
	{
		this.xpathExpr = xpathExpr;
		this.version = version;
		this.properties = properties;
		this.xPathHandler = xPathHandler;
		this.attrCtx = logger;
		try
		{
			this.initialize();
		}
		catch(final XPathException e)
		{
			throw this.wrapException(e, Phase.INITIALIZATION);
		}
	}
	
	public String getXpathExpression()
	{
		return this.xpathExpr;
	}
	
	public List<String> getRuleChainVisits()
	{
		return this.rulechainQueries;
	}
	
	public List<Node> evaluate(final Node node)
	{
		final AstTreeInfo documentNode = this.getDocumentNodeForRootNode(node);
		documentNode.setAttrCtx(this.attrCtx);
		try
		{
			
			// Map AST Node -> Saxon Node
			final XPathDynamicContext xpathDynamicContext =
				this.xpathExpression.createDynamicContext(documentNode.findWrapperFor(node));
			
			// XPath 2.0 sequences may contain duplicates
			final Set<Node> results = new LinkedHashSet<>();
			final List<Expression> expressions = this.getExpressionsForLocalNameOrDefault(node.getXPathNodeName());
			for(final Expression expression : expressions)
			{
				final SequenceIterator iterator = expression.iterate(xpathDynamicContext.getXPathContextObject());
				Item current = iterator.next();
				while(current != null)
				{
					if(current instanceof AstNodeOwner)
					{
						results.add(((AstNodeOwner)current).getUnderlyingNode());
					}
					else
					{
						throw new XPathException(
							"XPath rule expression returned a non-node (" + current.getClass() + "): " + current);
					}
					current = iterator.next();
				}
			}
			
			final List<Node> sortedRes = new ArrayList<>(results);
			sortedRes.sort(RuleChainAnalyzer.documentOrderComparator());
			return sortedRes;
		}
		catch(final XPathException e)
		{
			throw this.wrapException(e, Phase.EVALUATION);
		}
		catch(final UncheckedXPathException e)
		{
			throw this.wrapException(e.getXPathException(), Phase.EVALUATION);
		}
		finally
		{
			documentNode.setAttrCtx(DeprecatedAttrLogger.noop());
		}
	}
	
	private ContextedRuntimeException wrapException(final XPathException e, final Phase phase)
	{
		return new PmdXPathException(e, phase, this.xpathExpr, this.version);
	}
	
	// test only
	List<Expression> getExpressionsForLocalNameOrDefault(final String nodeName)
	{
		final List<Expression> expressions = this.nodeNameToXPaths.get(nodeName);
		if(expressions != null)
		{
			return expressions;
		}
		return this.nodeNameToXPaths.get(AST_ROOT);
	}
	
	// test only
	Expression getFallbackExpr()
	{
		return this.nodeNameToXPaths.get(SaxonXPathRuleQuery.AST_ROOT).get(0);
	}
	
	private AstTreeInfo getDocumentNodeForRootNode(final Node node)
	{
		final RootNode root = node.getRoot();
		return root.getUserMap().computeIfAbsent(
			SAXON_TREE_CACHE_KEY, () -> new AstTreeInfo(
				root,
				this.configuration)); // NOTE: ONLY WANTS NAME POOL OF CONFIGURATION AND NOTHING ELSE
	}
	
	private void addExpressionForNode(final String nodeName, final Expression expression)
	{
		this.nodeNameToXPaths.computeIfAbsent(nodeName, n -> new ArrayList<>(2)).add(expression);
	}
	
	private void initialize() throws XPathException
	{
		// IMPROVED: Use cache when possible
		final CachedInitData initData = getOrCreateCachedInitData(this.xPathHandler);
		this.configuration = initData.configuration();
		
		final StaticContextWithProperties staticCtx = new StaticContextWithProperties(configuration);
		staticCtx.setXPathLanguageLevel(this.version == XPathVersion.XPATH_3_1 ? 31 : 20);
		staticCtx.declareNamespace("fn", NamespaceUri.FN);
		
		for(final PropertyDescriptor<?> propertyDescriptor : this.properties.keySet())
		{
			final String name = propertyDescriptor.name();
			if(!"xpath".equals(name))
			{
				staticCtx.declareProperty(propertyDescriptor);
			}
		}
		
		// IMPROVED: Use cache
		initData.namespacesToDeclare().forEach(staticCtx::declareNamespace);
		
		final XPathEvaluator xpathEvaluator = new XPathEvaluator(configuration);
		xpathEvaluator.setStaticContext(staticCtx);
		
		this.xpathExpression = xpathEvaluator.createExpression(this.xpathExpr);
		this.analyzeXPathForRuleChain(xpathEvaluator);
	}
	
	private void analyzeXPathForRuleChain(final XPathEvaluator xpathEvaluator)
	{
		final Expression expr = this.xpathExpression.getInternalExpression();
		
		boolean useRuleChain = true;
		
		// First step: Split the union venn expressions into single expressions
		final Iterable<Expression> subexpressions = SaxonExprTransformations.splitUnions(expr);
		
		// Second step: Analyze each expression separately
		for(final Expression subexpression : subexpressions)
		{ // final because of checkstyle
			Expression modified = subexpression;
			modified = SaxonExprTransformations.hoistFilters(modified);
			modified = SaxonExprTransformations.reduceRoot(modified);
			modified = SaxonExprTransformations.copyTopLevelLets(modified, expr);
			final RuleChainAnalyzer rca = new RuleChainAnalyzer(xpathEvaluator.getConfiguration());
			final Expression finalExpr = rca.visit(modified); // final because of lambda
			
			if(!rca.getRootElements().isEmpty())
			{
				rca.getRootElements().forEach(it -> this.addExpressionForNode(it, finalExpr));
			}
			else
			{
				// couldn't find a root element for the expression, that means, we can't use rule chain at all
				// even though, it would be possible for part of the expression.
				useRuleChain = false;
				break;
			}
		}
		
		if(useRuleChain)
		{
			this.rulechainQueries.addAll(this.nodeNameToXPaths.keySet());
		}
		else
		{
			this.nodeNameToXPaths.clear();
			LOG.debug("Unable to use RuleChain for XPath: {}", this.xpathExpr);
		}
		
		// always add fallback expression
		this.addExpressionForNode(AST_ROOT, this.xpathExpression.getInternalExpression());
	}
	
	public static NamePool getNamePool()
	{
		return NAME_POOL;
	}
	
	final class StaticContextWithProperties extends IndependentContext
	{
		private final Map<StructuredQName, PropertyDescriptor<?>> propertiesByName = new HashMap<>();
		
		StaticContextWithProperties(final Configuration config)
		{
			super(config);
			// This statement is necessary for Saxon to support sequence-valued attributes
			this.getPackageData().setSchemaAware(true);
		}
		
		public void declareProperty(final PropertyDescriptor<?> prop)
		{
			final XPathVariable var = this.declareVariable(null, prop.name());
			this.propertiesByName.put(var.getVariableQName(), prop);
		}
		
		@Override
		public Expression bindVariable(final StructuredQName qName) throws XPathException
		{
			final LocalVariableReference local = (LocalVariableReference)super.bindVariable(qName);
			final PropertyDescriptor<?> prop = this.propertiesByName.get(qName);
			if(prop == null || prop.defaultValue() == null)
			{
				return local;
			}
			
			final Object actualValue = SaxonXPathRuleQuery.this.properties.getOrDefault(prop, prop.defaultValue());
			final AtomicSequence converted = DomainConversion.convert(actualValue);
			local.setStaticType(null, converted, 0);
			return local;
		}
	}
}
