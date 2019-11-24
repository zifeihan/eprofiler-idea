package fun.codec.eprofiler.runner;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import fun.codec.eprofiler.runner.calltree.CollectionUtil;
import fun.codec.eprofiler.runner.calltree.merge.Merge;
import fun.codec.eprofiler.runner.calltree.model.MultiMap;
import fun.codec.eprofiler.runner.calltree.model.ProfilerCallTreeWindow;
import fun.codec.eprofiler.runner.calltree.model.Stack;
import fun.codec.eprofiler.runner.calltree.model.StackFrame;
import fun.codec.eprofiler.runner.netty.HttpStaticFileServer;
import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * data collector
 *
 * @author: echo
 * @create: 2018-12-12 13:06
 */
public class ProfilerCollector implements ProjectComponent {

    private volatile boolean start;

    private String hotMethodPerfFile;

    private static volatile long position;

    private BlockingQueue<Stack> blockingQueue;

    private ProfilerCollector.PrintTree printTree;

    private ProfilerCallTreeWindow profilerCallTreeWindow;

    private static final Logger logger = Logger.getInstance(ProfilerExecutor.class);

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static final String HOME_URL = System.getProperty("user.home") + File.separator + "eprofiler" + File.separator + "flameGraph";

    static {
        try {
            Path path = Paths.get(HOME_URL);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            new Thread(() -> HttpStaticFileServer.main(null), "netty-start-thread").start();
        } catch (IOException e) {
            logger.error("create HOME_URL error. ", e);
        }

    }


    public ProfilerCollector() {
        this.blockingQueue = new ArrayBlockingQueue<>(100000);
    }

    public void setProfilerCallTreeWindow(ProfilerCallTreeWindow treeWindow) {
        this.profilerCallTreeWindow = treeWindow;
    }

    private void cleanStackTree() {
        if (profilerCallTreeWindow != null) {
            profilerCallTreeWindow.getRoot().removeAllChildren();
            profilerCallTreeWindow.reload();
        }
    }

    private void start(String perfFile) {
        //reset position
        this.position = 0;
        this.start = true;
        this.hotMethodPerfFile = perfFile;
        this.cleanStackTree();

        //print tree node.
        printTree = this.new PrintTree();
        printTree.startPrinter();
    }

    void stop() {
        //reset the position
        this.position = 0;
        this.start = false;
        this.blockingQueue.clear();

        //cancel timer
        printTree.cancel();

        //merge the total stack frame
        this.mergeTotalHotMethod();
    }

    private void createFlameGraph(String svgSavePath) throws IOException {

        Path path = Paths.get(hotMethodPerfFile);

        Path tempFile = Files.createTempFile("eprofiler", ".txt");

        Path svgPath = Paths.get(svgSavePath);

        List<String> list = Files.readAllLines(path);

        StringBuilder sb = new StringBuilder();

        final int[] temp = {0};
        List<String> stackFrame = new ArrayList<>();
        list.forEach(e -> {
            boolean start = e.startsWith("sample:");
            // resolve new line
            if (start) {
                temp[0] = Integer.valueOf(e.split(":")[1].trim());
            } else if (StringUtils.isNotBlank(e)) {
                stackFrame.add(e.trim());
            } else {
                // end a stack
                for (int i = stackFrame.size() - 1; i >= 0; i--) {
                    sb.append(stackFrame.get(i));
                    if (i != 0) sb.append(";");
                }
                sb.append(" ").append(temp[0]).append("\n");
                stackFrame.clear();
            }
        });

        Files.write(tempFile, sb.toString().getBytes(StandardCharsets.UTF_8));

        String flameGraph = ProfilerRunner.flamegraph;
        // create flameGraph
        String[] cmd = new String[]{flameGraph, "--colors=java", tempFile.toString()};

        List<String> lists = runNative(cmd);

        try (BufferedWriter bw = Files.newBufferedWriter(svgPath)) {
            for (String e : lists) {
                bw.append(e).append("\n");
            }
            bw.flush();
        }

    }

    private static List<String> runNative(String[] cmdToRunWithArgs) throws IOException {
        Process p;
        try {
            p = Runtime.getRuntime().exec(cmdToRunWithArgs);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }

        ArrayList<String> sa = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sa.add(line);
            }
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.error("runNative error.", e);
        }
        return sa;
    }

    //merge the total stack frame
    private void mergeTotalHotMethod() {
        //build all stack tree
        if (null == profilerCallTreeWindow) {
            return;
        }
        this.cleanStackTree();
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode("merge the total hotmethod data ...");
        profilerCallTreeWindow.getRoot().add(treeNode);
        profilerCallTreeWindow.reload();

        this.parseHotMethodFile();
        this.cleanStackTree();
        printTree.print(true);
        printTree.buildTree();
        profilerCallTreeWindow.reload();
    }

    public void print(boolean print) {
        if (!start) return;
        this.printTree.print(print);
    }

    /**
     * 导出火焰图
     */
    public void dumpFlameGraph() {
        try {
            // svg path
            String fileName = HOME_URL + File.separator + formatter.format(LocalDateTime.now()) + ".svg";

            createFlameGraph(fileName);
        } catch (Exception e) {
            logger.error("create flameGraph error :", e);
        }
    }

    public void analyse(String perfFilePath) {
        this.start(perfFilePath);
        while (start) {
            this.parseHotMethodFile();
        }
    }

    private void parseHotMethodFile() {
        RandomAccessFile randomAccessFile = null;
        try {
            File file = new File(this.hotMethodPerfFile);
            if (file.length() > position) {
                randomAccessFile = new RandomAccessFile(file, "r");
                randomAccessFile.seek(position);

                buildStackFrame(randomAccessFile);
                position = randomAccessFile.getFilePointer();
            } else {
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (Exception e) {
            logger.error("parse stack error,msg:", e);
        } finally {
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    logger.error("close file error,msg:", e);
                }
            }
        }
    }

    private void buildStackFrame(RandomAccessFile randomAccessFile) throws IOException {
        //Started [cpu] profiling
        randomAccessFile.readLine();
        String str;
        while ((str = randomAccessFile.readLine()) != null) {
            if (str.startsWith("sample:")) {
                String sample = str.substring(7).trim();
                //build the stack
                Stack stack = new Stack();
                stack.setSample(Integer.parseInt(sample));
                blockingQueue.add(stack);

                //build the stackFrame
                StackFrame stackFrame = new StackFrame();
                stackFrame.setSample(Integer.parseInt(sample));
                stack.setTop(stackFrame);

                buildStack(stackFrame, randomAccessFile);
            }
        }
    }

    private static void buildStack(StackFrame stackFrame, RandomAccessFile randomAccessFile) throws IOException {
        String str = randomAccessFile.readLine();
        if (str != null && !str.equals("")) {
            //next stackFrame
            StackFrame child = new StackFrame();
            child.setSample(stackFrame.getSample());

            stackFrame.setName(str);
            stackFrame.getChildList().add(child);
            buildStack(child, randomAccessFile);
        } else {
            stackFrame = null;
        }
    }

    public class PrintTree {

        private Timer timer;

        private volatile boolean printing = true;

        void startPrinter() {
            timer = new Timer("PRINT-timer", true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {

                    //remove prev stack
                    if (printing) {
                        ProfilerCollector.this.cleanStackTree();
                    }

                    PrintTree.this.buildTree();

                    if (printing && profilerCallTreeWindow != null) {
                        profilerCallTreeWindow.reload();
                    }

                }
            }, 10000, 5000);
        }

        void buildTree() {
            try {
                if (blockingQueue.isEmpty()) return;
                List<Stack> stacks = new ArrayList<>();
                blockingQueue.drainTo(stacks);
                if (!printing) return;
                if (profilerCallTreeWindow == null) return;
                MultiMap<String, StackFrame> multiMap = new MultiMap();
                for (Stack stack : stacks) {
                    multiMap.add(stack.getTop().getName(), stack.getTop());
                }

                List<StackFrame> stackFrames = new ArrayList<>();
                for (Map.Entry<String, List<StackFrame>> map : multiMap.entrySet()) {
                    List<StackFrame> stackFrameList = map.getValue();
                    StackFrame stackFrame = null;
                    //save top stackFrame
                    for (int i = 0; i < stackFrameList.size(); i++) {
                        if (i == 0) {
                            stackFrame = stackFrameList.get(0);
                        } else {
                            stackFrame = Merge.mergeStack(stackFrame, stackFrameList.get(i));
                        }
                    }
                    stackFrames.add(stackFrame);
                }

                //calculate stack percent
                calculatePercent(stackFrames);
                for (int i = 0; i < stackFrames.size(); i++) {
                    if (i > 100) {
                        break;
                    }
                    StackFrame stackFrame = stackFrames.get(i);
                    this.buildTreeNode(stackFrame, profilerCallTreeWindow.getRoot());
                }
            } catch (Exception e) {
                logger.error("[PrintTree] occur exception msg:", e);
            }
        }

        private void buildTreeNode(StackFrame stackFrame, DefaultMutableTreeNode parent) {
            if (stackFrame != null) {
                DecimalFormat decimalFormat = new DecimalFormat("##0.00");
                String percent = decimalFormat.format(stackFrame.getPercent() * 100);
                String format = String.format("    (%s%%)", percent);
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(stackFrame.getName() + format);
                parent.add(treeNode);
                List<StackFrame> childList = stackFrame.getChildList();
                for (StackFrame frame : childList) {
                    if (CollectionUtil.isEmpty(frame.getChildList())) return;
                    buildTreeNode(frame, treeNode);
                }
            }
        }

        private void calculatePercent(List<StackFrame> stackFrames) {
            if (stackFrames == null) return;
            stackFrames.sort(new Comparator<StackFrame>() {
                public int compare(StackFrame stackFrame, StackFrame stackFrame1) {
                    return Integer.compare(stackFrame1.getSample(), stackFrame.getSample());
                }
            });
            float samples = (float) stackFrames.stream().mapToInt(StackFrame::getSample).sum();
            for (StackFrame stackFrame : stackFrames) {
                float percent = stackFrame.getSample() / samples;
                stackFrame.setPercent(percent);
                List<StackFrame> stackFrameList = stackFrame.getChildList();
                calculatePercent(stackFrameList);
            }
        }

        void print(boolean print) {
            printing = print;
        }

        void cancel() {
            timer.cancel();
        }
    }
}
