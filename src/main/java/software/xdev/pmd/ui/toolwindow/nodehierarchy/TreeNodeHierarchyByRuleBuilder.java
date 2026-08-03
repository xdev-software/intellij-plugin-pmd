package software.xdev.pmd.ui.toolwindow.nodehierarchy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.ui.toolwindow.node.BaseNode;
import software.xdev.pmd.ui.toolwindow.node.ErrorSummaryNode;
import software.xdev.pmd.ui.toolwindow.node.FileOverviewNode;
import software.xdev.pmd.ui.toolwindow.node.FileOverviewWithRuleNode;
import software.xdev.pmd.ui.toolwindow.node.SuppressedSummaryNode;
import software.xdev.pmd.ui.toolwindow.node.SuppressedViolationNode;
import software.xdev.pmd.ui.toolwindow.node.SuppressedViolationRuleNode;
import software.xdev.pmd.ui.toolwindow.node.ViolationNode;
import software.xdev.pmd.ui.toolwindow.node.ViolationRuleNode;


public class TreeNodeHierarchyByRuleBuilder extends AbstractTreeNodeHierarchyByBuilder
{
	private final Map<PsiFile, FileOverviewNode> fileErrorNodes = new HashMap<>();
	
	public TreeNodeHierarchyByRuleBuilder(final CombinedPMDAnalysisResult result)
	{
		super(result);
	}
	
	private void computeFileErrors()
	{
		this.computeErrors(file -> this.fileErrorNodes.computeIfAbsent(file, FileOverviewNode::new));
	}
	
	private Map<ViolationRuleNode, Map<FileOverviewNode, List<ViolationNode>>> computeViolations()
	{
		return this.abstractComputeViolations(
			this.result.violations(),
			Function.identity(),
			ViolationNode::new,
			ViolationRuleNode::new);
	}
	
	private Map<SuppressedViolationRuleNode, Map<FileOverviewNode, List<SuppressedViolationNode>>>
	computeSuppressedViolations()
	{
		return this.abstractComputeViolations(
			this.result.suppressedRuleViolations(),
			Report.SuppressedViolation::getRuleViolation,
			SuppressedViolationNode::new,
			SuppressedViolationRuleNode::new);
	}
	
	private <V, RVN extends BaseNode, VN extends BaseNode> Map<RVN, Map<FileOverviewNode, List<VN>>>
	abstractComputeViolations(
		final List<V> resultViolations,
		final Function<V, RuleViolation> extractRuleViolation,
		final BiFunction<RuleViolation, PsiFile, VN> createViolationNode,
		final Function<Rule, RVN> createRuleViolationNode)
	{
		final Map<Rule, Map<FileId, List<V>>> condensedStructure =
			resultViolations
				.stream()
				.collect(Collectors.groupingBy(v -> extractRuleViolation.apply(v).getRule()))
				.entrySet()
				.stream()
				.collect(Collectors.toMap(
					Map.Entry::getKey,
					e -> e.getValue().stream()
						.collect(Collectors.groupingBy(v -> extractRuleViolation.apply(v).getFileId()))
				));
		
		final Map<RVN, Map<FileOverviewNode, List<VN>>> structure =
			condensedStructure.entrySet()
				.stream()
				.sorted(Comparator.<Map.Entry<Rule, Map<FileId, List<V>>>>comparingInt(e ->
						e.getKey().getPriority().getPriority())
					.thenComparing(e -> e.getKey().getName()))
				.collect(Collectors.toMap(
					e -> createRuleViolationNode.apply(e.getKey()),
					e -> e.getValue().entrySet().stream()
						.sorted(Comparator.comparing(e2 -> e2.getKey().getFileName()))
						.map(e2 -> {
							final PsiFile psiFile = this.result.fileIdPsiFiles().get(e2.getKey());
							final FileOverviewNode fileOverviewNode =
								new FileOverviewWithRuleNode(psiFile, e.getKey());
							
							final List<VN> violationNodes = e2.getValue()
								.stream()
								.map(v -> createViolationNode.apply(extractRuleViolation.apply(v), psiFile))
								.toList();
							violationNodes.forEach(fileOverviewNode::add);
							
							return Map.entry(fileOverviewNode, violationNodes);
						})
						.collect(Collectors.toMap(
							Map.Entry::getKey,
							Map.Entry::getValue,
							(l, r) -> r,
							LinkedHashMap::new
						)),
					(l, r) -> r,
					LinkedHashMap::new
				));
		structure.forEach((ruleNode, values) ->
			values.keySet().forEach(ruleNode::add));
		
		return structure;
	}
	
	@Override
	public List<BaseNode> build()
	{
		this.computeGeneralErrors();
		this.computeFileErrors();
		
		final List<BaseNode> nodes = new ArrayList<>(this.computeViolations().keySet());
		
		final var computedSuppressedViolations = this.computeSuppressedViolations();
		if(!computedSuppressedViolations.isEmpty())
		{
			final SuppressedSummaryNode suppressedSummaryNode = new SuppressedSummaryNode();
			computedSuppressedViolations.keySet().forEach(suppressedSummaryNode::add);
			nodes.add(suppressedSummaryNode);
		}
		
		if(!this.generalErrors.isEmpty()
			|| !this.fileErrorNodes.isEmpty())
		{
			this.errorsInFiles.forEach((fileNode, errorInFileNodes) ->
				errorInFileNodes.forEach(fileNode::add));
			
			final ErrorSummaryNode errorNode = new ErrorSummaryNode(this.fileErrorNodes.values());
			errorNode.addAll(this.generalErrors);
			nodes.add(errorNode);
		}
		
		return nodes;
	}
}
