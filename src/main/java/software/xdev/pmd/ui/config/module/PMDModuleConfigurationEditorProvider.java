package software.xdev.pmd.ui.config.module;

import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationEditorProvider;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;


public class PMDModuleConfigurationEditorProvider implements ModuleConfigurationEditorProvider
{
	@Override
	public ModuleConfigurationEditor[] createEditors(final ModuleConfigurationState state)
	{
		return new ModuleConfigurationEditor[]{
			new PMDModuleConfigurationEditor(state.getCurrentRootModel()
				.getModule())};
	}
}
