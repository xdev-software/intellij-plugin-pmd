package software.xdev.pmd.ui.config.project.components.thirdpartyclasspath;

import java.awt.BorderLayout;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;

import software.xdev.pmd.ui.config.project.PMDConfigPanel;
import software.xdev.pmd.ui.config.project.components.SubPMDConfigPanelManager;


public class ThirdPartyClasspathPanelManager extends SubPMDConfigPanelManager
{
	private final DefaultListModel<String> pathListModel = new DefaultListModel<>();
	private final JList<String> pathList = new JBList<>(this.pathListModel);
	
	public ThirdPartyClasspathPanelManager(final Project project, final PMDConfigPanel pmdConfigPanel)
	{
		super(project, pmdConfigPanel);
	}
	
	public JPanel buildPanel()
	{
		final JPanel container = new JPanel(new BorderLayout());
		container.add(
			new TitledSeparator("Third-Party Rules"),
			BorderLayout.NORTH);
		container.add(ToolbarDecorator.createDecorator(this.pathList)
			.setAddAction(new AddPathAction())
			.setEditAction(new EditPathAction())
			.setRemoveAction(new RemovePathAction())
			.setPreferredSize(DECORATOR_DIMENSIONS)
			.createPanel(),
			BorderLayout.CENTER);
		return container;
	}
	
	public List<String> getThirdPartyClassPath()
	{
		return IntStream.range(0, this.pathListModel.size())
			.mapToObj(this.pathListModel::get)
			.toList();
	}
	
	public void setThirdPartyClassPath(final List<String> classPath)
	{
		this.pathListModel.clear();
		this.pathListModel.addAll(classPath);
	}
	
	private static FileChooserDescriptor chooserDescriptor()
	{
		return new FileChooserDescriptor(true, false, true, true, false, false)
			.withFileFilter(file -> {
				final String currentExtension = file.getExtension();
				return currentExtension != null && "jar".equalsIgnoreCase(currentExtension.trim());
			});
	}
	
	class AddPathAction implements AnActionButtonRunnable
	{
		@Override
		public void run(final AnActionButton anActionButton)
		{
			final VirtualFile chosen = FileChooser.chooseFile(
				ThirdPartyClasspathPanelManager.chooserDescriptor(),
				ThirdPartyClasspathPanelManager.this.pmdConfigPanel,
				ThirdPartyClasspathPanelManager.this.project,
				ProjectUtil.guessProjectDir(ThirdPartyClasspathPanelManager.this.project));
			if(chosen != null)
			{
				ThirdPartyClasspathPanelManager.this.pathListModel.addElement(
					VfsUtilCore.virtualToIoFile(chosen).getAbsolutePath());
			}
		}
	}
	
	
	class EditPathAction implements AnActionButtonRunnable
	{
		@Override
		public void run(final AnActionButton anActionButton)
		{
			final int selected = ThirdPartyClasspathPanelManager.this.pathList.getSelectedIndex();
			if(selected < 0)
			{
				return;
			}
			
			final DefaultListModel<String> listModel = ThirdPartyClasspathPanelManager.this.pathListModel;
			final String selectedFile = listModel.get(selected);
			
			final VirtualFile toSelect = LocalFileSystem.getInstance().findFileByPath(selectedFile);
			final VirtualFile chosen = FileChooser.chooseFile(
				ThirdPartyClasspathPanelManager.chooserDescriptor(),
				ThirdPartyClasspathPanelManager.this.project,
				toSelect);
			if(chosen != null)
			{
				listModel.remove(selected);
				listModel.add(selected, VfsUtilCore.virtualToIoFile(chosen).getAbsolutePath());
				ThirdPartyClasspathPanelManager.this.pathList.setSelectedIndex(selected);
			}
		}
	}
	
	
	class RemovePathAction implements AnActionButtonRunnable
	{
		@Override
		public void run(final AnActionButton anActionButton)
		{
			final int[] selected = ThirdPartyClasspathPanelManager.this.pathList.getSelectedIndices();
			if(selected == null || selected.length == 0)
			{
				return;
			}
			
			for(final int index : selected)
			{
				ThirdPartyClasspathPanelManager.this.pathListModel.remove(index);
			}
		}
	}
}
