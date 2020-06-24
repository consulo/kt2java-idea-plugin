package consulo.internal.mjga.idea.toolWindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 2019-12-31
 */
public class JavaToolWindowFactory implements ToolWindowFactory
{
	@Override
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow)
	{
		ContentFactory factory = toolWindow.getContentManager().getFactory();


	}
}
