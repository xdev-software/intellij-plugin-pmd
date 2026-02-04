package software.xdev.pmd.ui.toolwindow.nodehierarchy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.ui.toolwindow.node.BaseNode;
import software.xdev.pmd.ui.toolwindow.node.ErrorSummaryNode;
import software.xdev.pmd.ui.toolwindow.node.FileOverviewNode;
import software.xdev.pmd.ui.toolwindow.node.SuppressedSummaryNode;
import software.xdev.pmd.ui.toolwindow.node.SuppressedViolationNode;
import software.xdev.pmd.ui.toolwindow.node.SuppressedViolationRuleNode;
import software.xdev.pmd.ui.toolwindow.node.ViolationNode;
import software.xdev.pmd.ui.toolwindow.node.ViolationRuleNode;


public class TreeNodeHierarchyByFileBuilder extends AbstractTreeNodeHierarchyByBuilder
{
	private final Map<PsiFile, FileOverviewNode> fileOverviewNodes = new HashMap<>();
	
	private final Map<FileOverviewNode, Map<ViolationRuleNode, Set<ViolationNode>>> fileViolations =
		new HashMap<>();
	
	private final Map<FileOverviewNode, Map<SuppressedViolationRuleNode, Set<SuppressedViolationNode>>>
		fileSuppressedViolations = new HashMap<>();
	
	public TreeNodeHierarchyByFileBuilder(final CombinedPMDAnalysisResult result)
	{
		super(result);
	}
	
	private FileOverviewNode fileOverViewNode(final PsiFile psiFile)
	{
		return this.fileOverviewNodes.computeIfAbsent(psiFile, FileOverviewNode::new);
	}
	
	private void computeErrors()
	{
		this.computeErrors(this::fileOverViewNode);
	}
	
	private void computeViolations()
	{
		this.abstractComputeViolations(
			this.result.violations(),
			Function.identity(),
			ViolationNode::new,
			ViolationRuleNode::new,
			this.fileViolations);
	}
	
	private void computeSuppressedViolations()
	{
		this.abstractComputeViolations(
			this.result.suppressedRuleViolations(),
			Report.SuppressedViolation::getRuleViolation,
			SuppressedViolationNode::new,
			SuppressedViolationRuleNode::new,
			this.fileSuppressedViolations);
	}
	
	@SuppressWarnings("java:S119")
	private <V, RVN, VN> void abstractComputeViolations(
		final List<V> resultViolations,
		final Function<V, RuleViolation> extractRuleViolation,
		final BiFunction<RuleViolation, PsiFile, VN> createViolationNode,
		final Function<Rule, RVN> createRuleViolationNode,
		final Map<FileOverviewNode, Map<RVN, Set<VN>>> outputs)
	{
		final Map<PsiFile, Map<Rule, Set<VN>>> violations = new HashMap<>();
		resultViolations.forEach(violation -> {
			final RuleViolation ruleViolation = extractRuleViolation.apply(violation);
			final PsiFile psiFile = this.fileIdPsiFiles.get(ruleViolation.getFileId());
			if(psiFile == null)
			{
				return;
			}
			
			final VN violationNode = createViolationNode.apply(ruleViolation, psiFile);
			violations.computeIfAbsent(psiFile, ignored -> new LinkedHashMap<>())
				.computeIfAbsent(ruleViolation.getRule(), ignored -> new LinkedHashSet<>())
				.add(violationNode);
		});
		
		final Comparator<Map.Entry<Rule, Set<VN>>> comparator =
			Comparator.<Map.Entry<Rule, Set<VN>>>comparingInt(e -> e.getKey().getPriority().getPriority())
				.thenComparing(e -> e.getKey().getName());
		violations.forEach((file, ruleViolations) ->
			outputs.put(
				this.fileOverViewNode(file), ruleViolations.entrySet()
					.stream()
					.sorted(comparator)
					.collect(Collectors.toMap(
						e -> createRuleViolationNode.apply(e.getKey()),
						Map.Entry::getValue,
						(l, r) -> r,
						LinkedHashMap::new))));
	}
	
	@Override
	public List<BaseNode> build()
	{
		this.computeGeneralErrors();
		this.computeErrors();
		this.computeViolations();
		this.computeSuppressedViolations();
		
		final List<BaseNode> nodes = new ArrayList<>();
		
		this.fileOverviewNodes.entrySet()
			.stream()
			.sorted(Comparator.comparing(e -> e.getKey().getName()))
			.map(Map.Entry::getValue)
			.peek(fileNode ->
			{
				Optional.ofNullable(this.fileViolations.get(fileNode))
					.ifPresent(rules ->
						rules.forEach((rule, violations) -> {
							violations.forEach(rule::add);
							fileNode.add(rule);
						}));
				
				Optional.ofNullable(this.fileSuppressedViolations.get(fileNode))
					.ifPresent(suppressedRules -> {
						final SuppressedSummaryNode suppressedSummaryNode = new SuppressedSummaryNode();
						suppressedRules.forEach((rule, violations) -> {
							violations.forEach(rule::add);
							suppressedSummaryNode.add(rule);
						});
						fileNode.add(suppressedSummaryNode);
					});
				
				Optional.ofNullable(this.errorsInFiles.get(fileNode))
					.ifPresent(errorInFileNodes ->
						fileNode.add(new ErrorSummaryNode(errorInFileNodes)));
			})
			.forEach(nodes::add);
		
		if(!this.generalErrors.isEmpty())
		{
			nodes.add(new ErrorSummaryNode(this.generalErrors));
		}
		
		return nodes;
	}
}
