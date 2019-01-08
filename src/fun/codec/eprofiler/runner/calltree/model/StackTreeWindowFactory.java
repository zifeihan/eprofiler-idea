package fun.codec.eprofiler.runner.calltree.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import fun.codec.eprofiler.runner.ProfilerCollector;
import org.jetbrains.annotations.NotNull;

/**
 * window factory
 *
 * @author: echo
 * @create: 2019-01-08 18:41
 */
public class StackTreeWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ProfilerCallTreeWindow treeWindow = new ProfilerCallTreeWindow(toolWindow, project);

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(treeWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);

        ProfilerCollector component = project.getComponent(ProfilerCollector.class);
        component.setProfilerCallTreeWindow(treeWindow);
    }
}
