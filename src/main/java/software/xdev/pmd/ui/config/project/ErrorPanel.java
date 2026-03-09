package software.xdev.pmd.ui.config.project;

import static javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS;
import static javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS;

import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;


@SuppressWarnings("checkstyle:MagicNumber")
public class ErrorPanel extends JPanel
{
	private final JTextArea errorField = new JTextArea();
	
	public ErrorPanel()
	{
		super(new BorderLayout());
		
		this.init();
		
		this.setError(new RuntimeException());
	}
	
	private void init()
	{
		this.setBorder(JBUI.Borders.empty(8));
		
		final JLabel infoLabel = new JLabel("Loading the rule file caused an error:");
		infoLabel.setBorder(JBUI.Borders.emptyBottom(8));
		this.add(infoLabel, BorderLayout.NORTH);
		
		this.errorField.setEditable(false);
		this.errorField.setTabSize(2);
		this.errorField.setWrapStyleWord(true);
		this.errorField.setLineWrap(true);
		
		final JScrollPane errorScrollPane =
			new JBScrollPane(this.errorField, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_ALWAYS);
		this.add(errorScrollPane, BorderLayout.CENTER);
	}
	
	public void setError(final Throwable t)
	{
		final StringWriter errorWriter = new StringWriter(256);
		this.causeOf(t).printStackTrace(new PrintWriter(errorWriter));
		
		this.errorField.setText(errorWriter.getBuffer().toString());
		this.errorField.setCaretPosition(0);
		this.invalidate();
	}
	
	private Throwable causeOf(final Throwable t)
	{
		if(t.getCause() != null
			&& t.getCause() != t
			&& !t.getClass().getPackage().getName().startsWith("net.sourceforge.pmd"))
		{
			return this.causeOf(t.getCause());
		}
		return t;
	}
}
