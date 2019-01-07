package fun.codec.eprofiler.runner;

import com.intellij.openapi.diagnostic.Logger;
import fun.codec.eprofiler.runner.calltree.CollectionUtil;
import fun.codec.eprofiler.runner.calltree.merge.Merge;
import fun.codec.eprofiler.runner.calltree.model.MultiMap;
import fun.codec.eprofiler.runner.calltree.model.ProfilerCallTreeWindow;
import fun.codec.eprofiler.runner.calltree.model.Stack;
import fun.codec.eprofiler.runner.calltree.model.StackFrame;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
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
public class ProfilerCollector {

    private static Logger logger = Logger.getInstance(ProfilerExecutor.class);

    private static volatile long position;

    private static BlockingQueue<Stack> blockingQueue = new ArrayBlockingQueue<Stack>(100000);

    private static volatile boolean start;

    public static void setStart(boolean start) {
        position = 0;
        ProfilerCollector.start = start;
        ProfilerCallTreeWindow.getRoot().removeAllChildren();

        if (!start) {
            //cancel timer
            PrintTree.timer.cancel();
        }
    }

    public static void analyse(String profFilePath) {

        //print tree node.
        PrintTree printTree = new PrintTree();
        printTree.startPrinter();

        while (start) {
            RandomAccessFile randomAccessFile = null;
            try {
                File file = new File(profFilePath);
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
    }

    private static void buildStackFrame(RandomAccessFile randomAccessFile) throws IOException {
        //Started [cpu] profiling
        randomAccessFile.readLine();
        String str = null;
        while ((str = randomAccessFile.readLine()) != null && str.startsWith("sample:")) {
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

    public static class PrintTree {

        private static Timer timer;

        private static volatile boolean printing = true;

        private final static Logger logger = Logger.getInstance(PrintTree.class);

        public static void startPrinter() {
            timer = new Timer("PRINT-timer", true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    //remove prev stack
                    ProfilerCallTreeWindow.getRoot().removeAllChildren();

                    PrintTree.buildTree();

                    if (printing) ProfilerCallTreeWindow.reload();
                }
            }, 10000, 5000);
        }

        public static void buildTree() {
            if (blockingQueue.isEmpty()) return;
            List<Stack> stacks = new ArrayList<>();
            blockingQueue.drainTo(stacks);
            if (!printing) return;
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
            for (StackFrame stackFrame : stackFrames) {
                PrintTree.buildTreeNode(stackFrame, ProfilerCallTreeWindow.getRoot());
            }
        }

        private static void buildTreeNode(StackFrame stackFrame, DefaultMutableTreeNode parent) {
            if (stackFrame != null) {
                DecimalFormat decimalFormat = new DecimalFormat("##0.00");
                String percent = decimalFormat.format(stackFrame.getPercent() * 100);
                String format = String.format("    (%s%%)", percent);
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(stackFrame.getName() + format);
                parent.add(treeNode);
                List<StackFrame> childList = stackFrame.getChildList();
                for (int i = 0; i < childList.size(); i++) {
                    StackFrame frame = childList.get(i);
                    if (CollectionUtil.isEmpty(frame.getChildList())) return;
                    buildTreeNode(frame, treeNode);
                }
            }
        }

        private static void calculatePercent(List<StackFrame> stackFrames) {
            if (stackFrames == null) return;
            Collections.sort(stackFrames, new Comparator<StackFrame>() {
                public int compare(StackFrame stackFrame, StackFrame stackFrame1) {
                    if (stackFrame.getSample() > stackFrame1.getSample()) {
                        return -1;
                    }
                    if (stackFrame.getSample() == stackFrame1.getSample()) {
                        return 0;
                    }
                    return 1;
                }
            });
            float samples = (float) stackFrames.stream().mapToInt(StackFrame::getSample).sum();
            for (int i = 0; i < stackFrames.size(); i++) {
                StackFrame stackFrame = stackFrames.get(i);
                float percent = stackFrame.getSample() / samples;
                stackFrame.setPercent(percent);
                List<StackFrame> stackFrameList = stackFrame.getChildList();
                calculatePercent(stackFrameList);
            }
        }

        public static void print(boolean print) {
            printing = print;
        }
    }
}
