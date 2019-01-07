package fun.codec.eprofiler.runner.calltree.model;

/**
 * stack
 *
 * @author: echo
 * @create: 2018-12-12 14:23
 */
public class Stack {

    private int sample;

    private StackFrame top;

    public int getSample() {
        return sample;
    }

    public void setSample(int sample) {
        this.sample = sample;
    }

    public StackFrame getTop() {
        return top;
    }

    public void setTop(StackFrame top) {
        this.top = top;
    }

    @Override
    public String toString() {
        return "Stack{" +
                "sample=" + sample +
                ", top=" + top +
                '}';
    }
}
