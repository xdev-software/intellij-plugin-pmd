package software.xdev.pmd.ui.toolwindow;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultCaret;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.SwingHelper;
import com.intellij.util.ui.UIUtil;

import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.lang.rule.RulePriority;
import net.sourceforge.pmd.lang.rule.RuleTargetSelector;
import software.xdev.pmd.markdown.RuleDescriptionDocMarkdownToHtmlService;
import software.xdev.pmd.ui.toolwindow.node.other.RulePriorityIcons;
import software.xdev.pmd.util.pmd.PMDLanguageFileTypeMapper;


public class RuleDetailPanel extends JBPanel<RuleDetailPanel> implements Disposable
{
	private final Project project;
	private final RuleDescriptionDocMarkdownToHtmlService mdToHtmlService;
	private final EditorFactory editorFactory;
	private final List<Editor> editorsToRelease = new ArrayList<>();
	
	public RuleDetailPanel(
		final Project project,
		final Rule rule)
	{
		super(new VerticalFlowLayout());
		this.project = project;
		this.mdToHtmlService = project.getService(RuleDescriptionDocMarkdownToHtmlService.class);
		this.editorFactory = EditorFactory.getInstance();
		
		this.add(this.createTopPanel(rule));
		
		final JBLabel lblMessage = this.createLabel(rule.getMessage());
		this.add(lblMessage);
		
		final JBTabbedPane tabs = new JBTabbedPane();
		Optional.ofNullable(rule.getDescription())
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.map(this::createMarkdownPanel)
			.ifPresent(c -> tabs.addTab("Description", c));
		
		if(!rule.getExamples().isEmpty())
		{
			final FileType fileType = project.getService(PMDLanguageFileTypeMapper.class).get(rule.getLanguage());
			
			final AtomicInteger exampleCounter = new AtomicInteger(0);
			rule.getExamples()
				.stream()
				.filter(Objects::nonNull)
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.forEach(example -> tabs.addTab(
					"Example" + (exampleCounter.incrementAndGet() > 1 ? " " + exampleCounter.get() : ""),
					this.createCodePanel(fileType, example)));
		}
		
		tabs.addTab("Definition", this.createDefinitionTabContent(rule));
		
		this.add(tabs);
	}
	
	private JBPanel<?> createTopPanel(final Rule rule)
	{
		final JBPanel<?> topPanel = new JBPanel<>(new HorizontalLayout(JBUI.scale(5)));
		
		final RulePriority priority = rule.getPriority();
		
		final JBLabel lblPriority = new JBLabel();
		lblPriority.setToolTipText(priority.getName() + " (" + priority.getPriority() + ")");
		lblPriority.setIcon(RulePriorityIcons.get(priority));
		topPanel.add(lblPriority, HorizontalLayout.LEFT);
		
		final JBLabel lblName = this.createLabel(rule.getName());
		lblName.setFont(JBFont.h4().asBold());
		topPanel.add(lblName, HorizontalLayout.LEFT);
		
		if(rule.isDeprecated())
		{
			final JBLabel lblDeprecated = new JBLabel();
			lblDeprecated.setToolTipText("Deprecated");
			lblDeprecated.setIcon(AllIcons.Nodes.ErrorIntroduction);
			topPanel.add(lblDeprecated, HorizontalLayout.LEFT);
		}
		
		Optional.ofNullable(rule.getExternalInfoUrl())
			.ifPresent(url -> {
				final HyperlinkLabel externalInfo = new HyperlinkLabel("External info");
				externalInfo.setHyperlinkTarget(url);
				topPanel.add(externalInfo, HorizontalLayout.RIGHT);
			});
		
		return topPanel;
	}
	
	private JEditorPane createMarkdownPanel(final String markdown)
	{
		final JEditorPane editorPane = new JEditorPane();
		editorPane.setContentType(UIUtil.HTML_MIME);
		if(editorPane.getCaret() == null)
		{
			editorPane.setCaret(new DefaultCaret());
		}
		if(editorPane.getCaret() instanceof final DefaultCaret defaultCaret)
		{
			defaultCaret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		}
		editorPane.setEditorKit(new HTMLEditorKitBuilder()
			.withGapsBetweenParagraphs()
			.build());
		editorPane.setBorder(JBUI.Borders.empty(5));
		editorPane.setEditable(false);
		editorPane.setOpaque(false);
		editorPane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
		
		SwingHelper.setHtml(
			editorPane,
			this.mdToHtmlService.mdToHtml(markdown),
			UIUtil.getLabelForeground());
		
		return editorPane;
	}
	
	private JComponent createCodePanel(final FileType fileType, final String code)
	{
		final Document doc = this.editorFactory.createDocument(code);
		doc.setReadOnly(true);
		final EditorEx viewer = (EditorEx)this.editorFactory.createViewer(doc);
		viewer.setCaretEnabled(false);
		viewer.setContextMenuGroupId(null);
		
		this.editorsToRelease.add(viewer);
		
		final EditorSettings settings = viewer.getSettings();
		settings.setLineMarkerAreaShown(false);
		settings.setFoldingOutlineShown(false);
		settings.setAdditionalColumnsCount(0);
		settings.setAdditionalLinesCount(0);
		settings.setRightMarginShown(false);
		settings.setLineNumbersShown(false);
		settings.setVirtualSpace(false);
		settings.setAdditionalPageAtBottom(false);
		
		viewer.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(
			this.project,
			fileType
		));
		
		return viewer.getComponent();
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	private Component createDefinitionTabContent(final Rule rule)
	{
		final DefaultTableModel defaultTableModel = new DefaultTableModel();
		final JBTable table = new JBTable(defaultTableModel);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		table.setStriped(true);
		
		defaultTableModel.addColumn("Attribute");
		defaultTableModel.addColumn("Value");
		
		record MetaInfoTableEntry<T>(
			String name,
			Function<Rule, T> extractFromRule,
			Function<T, String> format)
		{
			public static MetaInfoTableEntry<String> createForString(
				final String name,
				final Function<Rule, String> extractFromRule)
			{
				return new MetaInfoTableEntry<>(name, extractFromRule, Function.identity());
			}
			
			public String formattedValue(final Rule rule)
			{
				final T value = this.extractFromRule().apply(rule);
				if(value == null)
				{
					return "";
				}
				return this.format.apply(value);
			}
		}
		
		final Function<LanguageVersion, String> formatLangVersion =
			languageVersion -> languageVersion.getLanguage().getName() + " " + languageVersion.getVersion();
		Stream.of(
			new MetaInfoTableEntry<>("Min. language version", Rule::getMinimumLanguageVersion, formatLangVersion),
			new MetaInfoTableEntry<>("Max. language version", Rule::getMaximumLanguageVersion, formatLangVersion),
			MetaInfoTableEntry.createForString("RuleSet", Rule::getRuleSetName),
			MetaInfoTableEntry.createForString("Rule Class", Rule::getRuleClass),
			new MetaInfoTableEntry<>("Target Selector", Rule::getTargetSelector, RuleTargetSelector::toString),
			MetaInfoTableEntry.createForString("since", Rule::getSince)
		).forEach(entry ->
			defaultTableModel.addRow(new Object[]{entry.name(), entry.formattedValue(rule)}));
		
		table.getColumnModel().getColumn(0).setPreferredWidth(75);
		table.getColumnModel().getColumn(1).setPreferredWidth(300);
		
		// ToolbarDecorator is required for correct table initialization?
		return ToolbarDecorator.createDecorator(table).createPanel();
	}
	
	private JBLabel createLabel(final String text)
	{
		return new JBLabel(text).setCopyable(true);
	}
	
	@Override
	public void dispose()
	{
		this.editorsToRelease.forEach(this.editorFactory::releaseEditor);
		this.editorsToRelease.clear();
	}
}
