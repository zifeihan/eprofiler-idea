package fun.codec.eprofiler.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.*;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.process.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.sun.tools.attach.VirtualMachine;
import fun.codec.process.Jps;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Map;

/**
 * runner
 *
 * @author: echo
 * @create: 2018-12-11 09:54
 */
public class ProfilerRunner extends DefaultJavaProgramRunner {

    private Logger logger = Logger.getInstance(ProfilerRunner.class);

    private String profilerPath = System.getProperty("java.io.tmpdir") + "libasyncProfiler.so";

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        boolean bool;
        try {
            bool = (executorId == ProfilerExecutor.Companion.getEXECUTOR_ID())
                    && (!(profile instanceof RunConfigurationWithSuppressedDefaultRunAction))
                    && ((profile instanceof RunConfigurationBase) && SystemInfo.isUnix);
        } catch (Exception ex) {
            bool = false;
        }
        return bool;
    }

    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment env) throws ExecutionException {
        Project project = env.getProject();
        RunContentDescriptor descriptor = super.doExecute(state, env);
        RunProfile runProfile = env.getRunProfile();
        ApplicationConfiguration configuration = (ApplicationConfiguration) runProfile;
        String mainClass = configuration.getMainClassName();
        if (descriptor != null) {
            ProcessHandler processHandler = descriptor.getProcessHandler();
            File tmpFile = this.copyProfilerAgent();
            Map<String, String> map = Jps.getPID(new String[]{"-l"});
            String PID = map.get(mainClass);
            processHandler.addProcessListener(new CapturingProcessAdapter() {
                @Override
                public void startNotified(@NotNull ProcessEvent event) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {

                            ProfilerRunner.this.profiler(PID, tmpFile.getAbsolutePath());

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
                        tmpFile.delete();
                    } catch (Exception e) {
                        logger.error("[close profiler error,error msg:]", e);
                    }
                }
            });
        }
        return descriptor;
    }

    private void profiler(String pid, String tmpFilePath) {
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgentPath(profilerPath, "start,file=" + tmpFilePath);
            vm.detach();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

            InputStream inputStream = classLoader.getResource("dylib/libasyncProfiler.so").openStream();
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
