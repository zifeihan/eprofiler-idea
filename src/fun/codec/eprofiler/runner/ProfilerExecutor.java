package fun.codec.eprofiler.runner;

import com.intellij.execution.Executor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Executor
 *
 * @author: echo
 * @create: 2018-12-10 21:22
 */
public class ProfilerExecutor extends Executor {

    @Override
    public String getToolWindowId() {
        return getId();
    }

    @Override
    public Icon getToolWindowIcon() {
        Icon icon = AllIcons.Toolwindows.ToolWindowRun;
        return icon;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        Icon icon = IconLoader.getIcon("/icons/start.png", getClass());
        return icon;
    }

    @Override
    public Icon getDisabledIcon() {
        Icon icon = AllIcons.Process.DisabledRun;
        return icon;
    }

    @Override
    public String getDescription() {
        return "Run application with eprofiler agent";
    }

    @NotNull
    @Override
    public String getActionName() {
        return "Run with EProfiler";
    }

    @NotNull
    @Override
    public String getId() {
        return EXECUTOR_ID;
    }

    @NotNull
    @Override
    public String getStartActionText() {
        return "Run with EProfiler";
    }

    @Override
    public String getContextActionId() {
        return getId() + " context-action-does-not-exist";
    }

    @Override
    public String getHelpId() {
        return null;
    }

    private static final String EXECUTOR_ID = "Profiler Executor";

    public static final class Companion
    {
        @NotNull
        public final String getEXECUTOR_ID()
        {
            return ProfilerExecutor.EXECUTOR_ID;
        }
    }

    public static final Companion Companion = new Companion();
}
