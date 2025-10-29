package software.xdev.pmd.ui.toolwindow.currentfile;

import java.awt.BorderLayout;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.text.DefaultCaret;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.SwingHelper;
import com.intellij.util.ui.UIUtil;

import net.sourceforge.pmd.lang.rule.Rule;
import software.xdev.pmd.markdown.RuleDescriptionDocMarkdownToHtmlService;
import software.xdev.pmd.util.pmd.PMDLanguageFileTypeMapper;


class RuleDetailPanel extends JBPanel<RuleDetailPanel>
{
	private final Project project;
	private final RuleDescriptionDocMarkdownToHtmlService mdToHtmlService;
	
	public RuleDetailPanel(
		final Project project,
		final Rule rule)
	{
		super(new VerticalFlowLayout());
		this.project = project;
		this.mdToHtmlService = project.getService(RuleDescriptionDocMarkdownToHtmlService.class);
		
		final JBPanel topPanel = new JBPanel(new BorderLayout());
		this.add(topPanel);
		
		final JBLabel lblName = this.createLabel(rule.getName());
		lblName.setFont(JBFont.h4().asBold());
		
		Optional.ofNullable(rule.getExternalInfoUrl())
			.ifPresent(url -> {
				final HyperlinkLabel externalInfo = new HyperlinkLabel("External Info");
				externalInfo.setHyperlinkTarget(url);
				topPanel.add(externalInfo, BorderLayout.LINE_END);
			});
		
		topPanel.add(lblName, BorderLayout.LINE_START);
		
		final JBLabel lblMessage = this.createLabel(rule.getMessage());
		this.add(lblMessage);
		
		rule.getRuleSetName();
		rule.getMinimumLanguageVersion();
		rule.getMaximumLanguageVersion();
		rule.getPriority();
		rule.getExternalInfoUrl();
		
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
		
		this.add(tabs);
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
		final EditorFactory editorFactory = EditorFactory.getInstance();
		final Document doc = editorFactory.createDocument(code);
		doc.setReadOnly(true);
		final EditorEx viewer = (EditorEx)editorFactory.createViewer(doc);
		viewer.setCaretEnabled(false);
		viewer.setContextMenuGroupId(null);
		
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
	
	private JBLabel createLabel(final String text)
	{
		return new JBLabel(text).setCopyable(true);
	}
}
