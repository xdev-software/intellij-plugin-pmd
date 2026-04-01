package software.xdev.pmd.ui.config.project;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.intellij.util.ui.JBUI;


@SuppressWarnings("checkstyle:MagicNumber")
public class CompletePanel extends JPanel
{
	public CompletePanel()
	{
		super(new GridBagLayout());
		
		this.init();
	}
	
	private void init()
	{
		final JLabel infoLabel = new JLabel("The file has been validated and is ready to add");
		
		this.setBorder(JBUI.Borders.empty(4));
		
		this.add(
			infoLabel, new GridBagConstraints(
				0, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST,
				GridBagConstraints.NONE, JBUI.insets(8), 0, 0));
	}
}
