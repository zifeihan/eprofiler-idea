package fun.codec.eprofiler.runner.calltree.action;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.BackgroundTaskQueue;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import fun.codec.eprofiler.runner.ProfilerCollector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;

/**
 * dump frame
 *
 * @author: echo
 * @create: 2019-11-24
 */
public class FlameDumpAction extends AnAction {

    public FlameDumpAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();

        new BackgroundTaskQueue(project, "DumpFlameGraph").run(new Task.Backgroundable(project, "DumpFlameGraph") {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    boolean success = project.getComponent(ProfilerCollector.class).dumpFlameGraph();

                    if (success) {
                        new NotificationGroup("Profiling", NotificationDisplayType.NONE, true)
                                .createNotification("导出火焰图成功", MessageType.INFO).notify(project);
                    }
                } catch (IOException e) {
                    new NotificationGroup("Profiling", NotificationDisplayType.NONE, true)
                            .createNotification("导出火焰图失败", MessageType.ERROR).notify(project);
                }
            }
        });
    }
}
