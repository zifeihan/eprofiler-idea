package fun.codec.eprofiler.runner.calltree.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * stackFrame
 *
 * @author: echo
 * @create: 2018-12-12 14:25
 */
public class StackFrame {

    private float percent;

    private String name;

    private int sample;

    private List<StackFrame> childList = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSample() {
        return sample;
    }

    public void setSample(int sample) {
        this.sample = sample;
    }

    public List<StackFrame> getChildList() {
        return childList;
    }

    public void setChildList(List<StackFrame> childList) {
        this.childList = childList;
    }

    public float getPercent() {
        return percent;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }

    @Override
    public String toString() {
        return "StackFrame{" +
                "percent=" + percent +
                ", name='" + name + '\'' +
                ", sample=" + sample +
                ", childList=" + childList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StackFrame that = (StackFrame) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
