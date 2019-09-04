package fun.codec.eprofiler.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.*;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.process.CapturingProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * runner
 *
 * @author: echo
 * @create: 2018-12-11 09:54
 */
public class ProfilerRunner extends DefaultJavaProgramRunner {

    private Map<Project, File> perfFileMap = new HashMap<>();

    private Logger logger = Logger.getInstance(ProfilerRunner.class);

    private String profilerPath = System.getProperty("java.io.tmpdir") + File.separator + "libcpuprofiler.so";

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        boolean bool;
        try {
            bool = (executorId == ProfilerExecutor.Companion.getEXECUTOR_ID())
                    && (!(profile instanceof RunConfigurationWithSuppressedDefaultRunAction))
                    && (profile instanceof RunConfigurationBase)
                    && (SystemInfo.isMac || SystemInfo.isLinux);
        } catch (Exception ex) {
            bool = false;
        }
        return bool;
    }

    @Override
    public void patch(JavaParameters javaParameters, RunnerSettings settings, RunProfile runProfile, boolean beforeExecution) throws ExecutionException {
        super.patch(javaParameters, settings, runProfile, beforeExecution);
        if (beforeExecution) {
            Project project = ((RunConfigurationBase) runProfile).getProject();
            if (perfFileMap.get(project) != null) return;
            ParametersList vmParametersList = javaParameters.getVMParametersList();
            File tmpFile = this.copyProfilerAgent();
            StringBuilder sb = new StringBuilder()
                    .append("-agentpath:")
                    .append(profilerPath)
                    .append("=start,")
                    .append("file=")
                    .append(tmpFile.getAbsolutePath());
            vmParametersList.add(sb.toString());
            perfFileMap.put(project, tmpFile);
        }
    }

    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment env) throws ExecutionException {
        Project project = env.getProject();
        RunContentDescriptor descriptor = super.doExecute(state, env);
        if (descriptor != null) {
            ProcessHandler processHandler = descriptor.getProcessHandler();
            processHandler.addProcessListener(new CapturingProcessAdapter() {
                @Override
                public void startNotified(@NotNull ProcessEvent event) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            File tmpFile = perfFileMap.get(project);
                            project.getComponent(ProfilerCollector.class).analyse(tmpFile.getAbsolutePath());
                        }
                    }, "ProfilerCollector-Thread");
                    thread.setDaemon(true);
                    thread.start();
                }

                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    logger.info("close profiler...");
                    try {
                        project.getComponent(ProfilerCollector.class).stop();
                        File tmpFile = perfFileMap.get(project);
                        tmpFile.delete();
                        perfFileMap.remove(project);
                    } catch (Exception e) {
                        logger.error("[close profiler error,error msg:]", e);
                    }
                }
            });
        }
        return descriptor;
    }

    /**
     * copy agent to user.home and gen tmp file
     */
    private File copyProfilerAgent() {
        ClassLoader classLoader = getClass().getClassLoader();

        File dylib = new File(profilerPath);
        if (dylib.exists()) dylib.delete();

        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("profiler", ".dd");
            String name = String.format("dylib/%s/libcpuprofiler.so", SystemInfo.isMac ? "mac" : "linux");
            InputStream inputStream = classLoader.getResource(name).openStream();
            FileOutputStream fos = new FileOutputStream(dylib);
            int data;
            while ((data = inputStream.read()) != -1) {
                fos.write(data);
            }
            inputStream.close();
            fos.close();
        } catch (IOException e) {
            logger.error("create tmp file error", e);
        }
        return tmpFile;
    }


}
