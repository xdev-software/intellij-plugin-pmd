package software.xdev.pmd.ui.config.project;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Collection;
import java.util.Enumeration;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;

import software.xdev.pmd.config.PatternContainer;


class FileMaskPanelContainer
{
	private SortedSet<String> patterns = new TreeSet<>();
	
	private final SortedListModel patternModels = new SortedListModel();
	
	@SuppressWarnings("checkstyle:IllegalIdentifierName")
	private final JBList<String> patternList;
	
	private final JPanel patternPanel;
	private final JPanel panel;
	
	FileMaskPanelContainer(
		final String textTitle,
		final String textEmpty,
		final String textAddTitle,
		final String textEditTitle,
		final String textAddOrEditMessage)
	{
		this(textTitle, textEmpty, textAddTitle, textAddOrEditMessage, textEditTitle, textAddOrEditMessage);
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	FileMaskPanelContainer(
		final String textTitle,
		final String textEmpty,
		final String textAddTitle,
		final String textAddMessage,
		final String textEditTitle,
		final String textEditMessage)
	{
		this.patternList = new JBList<>(this.patternModels);
		this.patternList.setEmptyText(textEmpty);
		this.patternPanel = ToolbarDecorator.createDecorator(this.patternList)
			.setAddAction(this.getAddActionButtonRunnable(textAddTitle, textAddMessage))
			.setRemoveAction(this.getRemoveActionButtonRunnable())
			.setEditAction(this.getEditActionButtonRunnable(textEditTitle, textEditMessage))
			.disableUpDownActions()
			.setPreferredSize(new Dimension(Integer.MAX_VALUE, 80))
			.createPanel();
		
		this.panel = new JPanel(new BorderLayout());
		this.panel.add(new TitledSeparator(textTitle), BorderLayout.NORTH);
		this.panel.add(this.patternPanel, BorderLayout.CENTER);
	}
	
	private AnActionButtonRunnable getEditActionButtonRunnable(
		final String textEditTitle,
		final String textEditMessage)
	{
		return actionButton -> {
			final String oldValue = this.patternList.getSelectedValue();
			final String pattern = Messages.showInputDialog(
				textEditMessage, textEditTitle, null, oldValue, this.getInputValidator());
			if(pattern != null && !pattern.equals(oldValue))
			{
				this.patterns.remove(oldValue);
				this.patternModels.removeElement(oldValue);
				if(this.patterns.add(pattern))
				{
					this.patternModels.addElementSorted(pattern);
				}
			}
		};
	}
	
	JPanel getPanel()
	{
		return this.panel;
	}
	
	void update(final SortedSet<String> patterns)
	{
		this.patterns = new TreeSet<>(patterns);
		this.patternModels.clear();
		this.patternModels.addAllSorted(patterns);
	}
	
	SortedSet<String> getPatterns()
	{
		return this.patterns;
	}
	
	@NotNull
	private AnActionButtonRunnable getRemoveActionButtonRunnable()
	{
		return actionButton -> {
			for(final String selectedValue : this.patternList.getSelectedValuesList())
			{
				this.patterns.remove(selectedValue);
				this.patternModels.removeElement(selectedValue);
			}
		};
	}
	
	@NotNull
	private AnActionButtonRunnable getAddActionButtonRunnable(final String textAddTitle, final String textAddMessage)
	{
		return actionButton -> {
			final String pattern = Messages.showInputDialog(
				textAddMessage, textAddTitle, null, null, this.getInputValidator());
			if(pattern != null && this.patterns.add(pattern))
			{
				this.patternModels.addElementSorted(pattern);
			}
		};
	}
	
	@NotNull
	private InputValidator getInputValidator()
	{
		return new InputValidator()
		{
			@Override
			public boolean checkInput(final String string)
			{
				return PatternContainer.tryCreate(string) != null;
			}
			
			@Override
			public boolean canClose(final String s)
			{
				return true;
			}
		};
	}
	
	private static final class SortedListModel extends DefaultListModel<String>
	{
		private void addElementSorted(final String element)
		{
			final Enumeration<?> modelElements = this.elements();
			int index = 0;
			while(modelElements.hasMoreElements())
			{
				final String modelElement = (String)modelElements.nextElement();
				if(0 < modelElement.compareTo(element))
				{
					this.add(index, element);
					return;
				}
				index++;
			}
			this.addElement(element);
		}
		
		private void addAllSorted(final Collection<String> elements)
		{
			for(final String element : elements)
			{
				this.addElementSorted(element);
			}
		}
	}
}
