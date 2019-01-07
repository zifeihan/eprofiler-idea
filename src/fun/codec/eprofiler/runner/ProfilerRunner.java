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
import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * runner
 *
 * @author: echo
 * @create: 2018-12-11 09:54
 */
public class ProfilerRunner extends DefaultJavaProgramRunner {

    private Logger logger = Logger.getInstance(ProfilerRunner.class);

    private File tmpFile;

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        boolean bool;
        try {
            bool = (executorId == ProfilerExecutor.Companion.getEXECUTOR_ID())
                    && (!(profile instanceof RunConfigurationWithSuppressedDefaultRunAction))
                    && ((profile instanceof RunConfigurationBase));
        } catch (Exception ex) {
            bool = false;
        }
        return bool;
    }

    @Override
    public void patch(JavaParameters javaParameters, RunnerSettings settings, RunProfile runProfile, boolean beforeExecution) throws ExecutionException {
        super.patch(javaParameters, settings, runProfile, beforeExecution);
        if (beforeExecution) {
            ParametersList vmParametersList = javaParameters.getVMParametersList();
            ClassLoader classLoader = getClass().getClassLoader();

            File dir = new File(System.getProperty("user.home") + File.separator + ".perf");
            if (!dir.exists()) dir.mkdir();

            File dylib = new File(dir.getAbsolutePath() + File.separator + "libasyncProfiler.so");
            if (dylib.exists()) dylib.delete();
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

            StringBuilder sb = new StringBuilder()
                    .append("-agentpath:")
                    .append(dylib)
                    .append("=start,")
                    .append("file=")
                    .append(tmpFile.getAbsolutePath());
            vmParametersList.add(sb.toString());
        }
    }


    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment env) throws ExecutionException {
        RunContentDescriptor descriptor = super.doExecute(state, env);
        if (descriptor != null) {
            ProcessHandler processHandler = descriptor.getProcessHandler();
            processHandler.addProcessListener(new CapturingProcessAdapter() {
                @Override
                public void startNotified(@NotNull ProcessEvent event) {
                    ProfilerCollector.setStart(true);
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ProfilerCollector.analyse(tmpFile.getAbsolutePath());
                        }
                    }, "ProfilerCollector-Thread");
                    thread.setDaemon(true);
                    thread.start();
                }

                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    logger.info("close profiler...");
                    try {
                        ProfilerCollector.setStart(false);
                        tmpFile.delete();
                    } catch (Exception e) {
                        logger.error("[close profiler error,error msg:]", e);
                    }
                }
            });
        }
        return descriptor;
    }
}
