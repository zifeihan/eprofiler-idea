package fun.codec.eprofiler.runner.calltree.merge;

import com.intellij.openapi.diagnostic.Logger;
import fun.codec.eprofiler.runner.calltree.CollectionUtil;
import fun.codec.eprofiler.runner.calltree.model.StackFrame;

import java.util.List;

/**
 * merge stackFrame
 * <p>
 *
 * @author: echo
 * @create: 2018-12-12 22:03
 */
public class Merge {
    private static Logger logger = Logger.getInstance(Merge.class);

    public static StackFrame mergeStack(StackFrame stackFrame, StackFrame stackFrame1) {
        if (stackFrame1 == null && stackFrame1 == null) {
            return stackFrame;
        }

        if (CollectionUtil.isEmpty(stackFrame.getChildList()) || CollectionUtil.isEmpty(stackFrame1.getChildList())) {
            return stackFrame;
        }
        // set sample
        if (stackFrame.getName().equals(stackFrame1.getName())) {
            stackFrame.setSample(stackFrame.getSample() + stackFrame1.getSample());
        }

        StackFrame child1 = stackFrame1.getChildList().get(0);
        if (CollectionUtil.isEmpty(child1.getChildList())) {
            return stackFrame;
        }

        List<StackFrame> childList = stackFrame.getChildList();
        if (childList.contains(child1)) {
            for (int i = 0; i < childList.size(); i++) {
                StackFrame child = childList.get(i);
                try {
                    if (child.getName().equals(child1.getName())) {
                        mergeStack(child, child1);
                        break;
                    }
                } catch (Exception e) {
                    logger.error("merge stack error,msg:", e);
                }
            }
        } else {
            childList.add(child1);
        }
        return stackFrame;
    }
}
