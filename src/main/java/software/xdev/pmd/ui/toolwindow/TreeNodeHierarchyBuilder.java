package software.xdev.pmd.ui.toolwindow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.intellij.psi.PsiFile;

import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.reporting.RuleViolation;
import software.xdev.pmd.currentfile.CombinedPMDAnalysisResult;
import software.xdev.pmd.ui.toolwindow.node.BaseNode;
import software.xdev.pmd.ui.toolwindow.node.ErrorInFileNode;
import software.xdev.pmd.ui.toolwindow.node.ErrorNode;
import software.xdev.pmd.ui.toolwindow.node.ErrorSummaryNode;
import software.xdev.pmd.ui.toolwindow.node.FileOverviewNode;
import software.xdev.pmd.ui.toolwindow.node.SuppressedSummaryNode;
import software.xdev.pmd.ui.toolwindow.node.SuppressedViolationNode;
import software.xdev.pmd.ui.toolwindow.node.SuppressedViolationRuleNode;
import software.xdev.pmd.ui.toolwindow.node.ViolationNode;
import software.xdev.pmd.ui.toolwindow.node.ViolationRuleNode;
import software.xdev.pmd.ui.toolwindow.node.error.NodeErrorAdapter;


public class TreeNodeHierarchyBuilder
{
	private final CombinedPMDAnalysisResult result;
	private final Map<FileId, PsiFile> fileIdPsiFiles;
	
	private final Map<PsiFile, FileOverviewNode> fileFileOverviewNodes = new HashMap<>();
	
	private final List<ErrorNode> generalErrors = new ArrayList<>();
	private final Map<FileOverviewNode, Set<ErrorInFileNode>> errorsInFiles = new HashMap<>();
	
	private final Map<FileOverviewNode, Map<ViolationRuleNode, Set<ViolationNode>>> fileViolations =
		new HashMap<>();
	
	private final Map<FileOverviewNode, Map<SuppressedViolationRuleNode, Set<SuppressedViolationNode>>>
		fileSuppressedViolations = new HashMap<>();
	
	public TreeNodeHierarchyBuilder(final CombinedPMDAnalysisResult result)
	{
		this.result = result;
		this.fileIdPsiFiles = result.fileIdPsiFiles();
	}
	
	private FileOverviewNode fileOverViewNode(final PsiFile psiFile)
	{
		return this.fileFileOverviewNodes.computeIfAbsent(psiFile, FileOverviewNode::new);
	}
	
	private void computeGeneralErrors()
	{
		this.result.configErrors().stream()
			.map(ce -> new ErrorNode(NodeErrorAdapter.fromString(ce.rule().getName() + ": " + ce.issue())))
			.forEach(this.generalErrors::add);
	}
	
	private void computeErrors()
	{
		this.result.errors().forEach(pe -> {
			final NodeErrorAdapter nodeErrorAdapter = NodeErrorAdapter.fromThrowable(pe.getError());
			Optional.ofNullable(pe.getFileId())
				.map(this.fileIdPsiFiles::get)
				.ifPresentOrElse(
					file -> this.errorsInFiles.computeIfAbsent(
							this.fileOverViewNode(file),
							ignored -> new LinkedHashSet<>())
						.add(new ErrorInFileNode(nodeErrorAdapter, file)),
					() -> this.generalErrors.add(new ErrorNode(nodeErrorAdapter))
				);
		});
	}
	
	private void computeViolations()
	{
		final Map<PsiFile, Map<Rule, Set<ViolationNode>>> violations = new HashMap<>();
		this.result.violations().forEach(ruleViolation -> {
			final PsiFile psiFile = this.fileIdPsiFiles.get(ruleViolation.getFileId());
			if(psiFile == null)
			{
				return;
			}
			
			final ViolationNode violationNode = new ViolationNode(ruleViolation, psiFile);
			violations.computeIfAbsent(psiFile, ignored -> new LinkedHashMap<>())
				.computeIfAbsent(ruleViolation.getRule(), ignored -> new LinkedHashSet<>())
				.add(violationNode);
		});
		
		violations.forEach((file, ruleViolations) -> {
			this.fileViolations.put(
				this.fileOverViewNode(file), ruleViolations.entrySet()
					.stream()
					.sorted(Comparator.comparing(e -> e.getKey().getName()))
					.collect(Collectors.toMap(
						e -> new ViolationRuleNode(e.getKey()),
						Map.Entry::getValue,
						(l, r) -> r,
						LinkedHashMap::new)));
		});
	}
	
	private void computeSuppressedViolations()
	{
		final Map<PsiFile, Map<Rule, Set<SuppressedViolationNode>>> violations = new HashMap<>();
		this.result.suppressedRuleViolations().forEach(suppressedViolation -> {
			final RuleViolation ruleViolation = suppressedViolation.getRuleViolation();
			final PsiFile psiFile = this.fileIdPsiFiles.get(ruleViolation.getFileId());
			if(psiFile == null)
			{
				return;
			}
			
			final SuppressedViolationNode violationNode = new SuppressedViolationNode(ruleViolation, psiFile);
			violations.computeIfAbsent(psiFile, ignored -> new LinkedHashMap<>())
				.computeIfAbsent(ruleViolation.getRule(), ignored -> new LinkedHashSet<>())
				.add(violationNode);
		});
		
		violations.forEach((file, ruleViolations) -> {
			this.fileSuppressedViolations.put(
				this.fileOverViewNode(file), ruleViolations.entrySet()
					.stream()
					.sorted(Comparator.comparing(e -> e.getKey().getName()))
					.collect(Collectors.toMap(
						e -> new SuppressedViolationRuleNode(e.getKey()),
						Map.Entry::getValue,
						(l, r) -> r,
						LinkedHashMap::new)));
		});
	}
	
	public List<BaseNode> build()
	{
		this.computeGeneralErrors();
		this.computeErrors();
		this.computeViolations();
		this.computeSuppressedViolations();
		
		final List<BaseNode> nodes = new ArrayList<>();
		
		this.fileFileOverviewNodes.entrySet()
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
					.ifPresent(errorInFileNodes -> {
						fileNode.add(new ErrorSummaryNode(errorInFileNodes));
					});
			})
			.forEach(nodes::add);
		
		if(!this.generalErrors.isEmpty())
		{
			nodes.add(new ErrorSummaryNode(this.generalErrors));
		}
		
		return nodes;
	}
}
