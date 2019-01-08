package fun.codec.eprofiler.runner.calltree.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import fun.codec.eprofiler.runner.ProfilerCollector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * refresh stack tree
 *
 * @author: echo
 * @create: 2019-01-08 16:27
 */
public class RefreshAction extends AnAction {

    public RefreshAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        project.getComponent(ProfilerCollector.class).print(true);
    }
}
