package submit;

// some useful things to import. add any additional imports you need.
import joeq.Compiler.Quad.*;
import flow.Flow;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Skeleton class for implementing a reaching definition analysis
 * using the Flow.Analysis interface.
 */
public class ReachingDefs implements Flow.Analysis {

    public static class PairDef implements Comparable {
        public int position;
        public String registed;
        PairDef(String registed, int position) {
            this.registed = registed;
            this.position = position;
        }
        @Override
        public boolean equals(Object o) {
            if (o instanceof PairDef) {
                PairDef p = (PairDef)o;
                if(this.registed.equals(p.registed) && this.postion == p.position)
                    return true;
                else
                    return false;
            }
            else
                return false;
        }
        public int compareTo(Object o) {
            PairDef p = (PairDef)o;
            if (this.position < p.position)
                return -1;
            else if (this.position == p.position)
                return 0;
            else
                return 1;
        }
        @Override
        public int hashCode() {
            int rst = this.registed.hashCode() + new Integer(this.position).hashCode();
            return rst;
        }
    }

    /**
     * Class for the dataflow objects in the ReachingDefs analysis.
     * You are free to change this class or move it to another file.
     */
    public static class DefineSet implements Flow.DataflowObject {
        public static Set<PairDef> uniSet;
        public Set<PairDef> set;

        // initialize
        public DefineSet() {
            this.set = new TreeSet<PairDef>();
        }

        public void copy (Flow.DataflowObject o) {
            this.set = new TreeSet<PairDef>(((DefineSet)o).set);
        }

        public void setToTop() {
            this.set = new TreeSet<PairDef>();
        }

        public void setToBottom() {
            this.set = new TreeSet<PairDef>(uniSet);
        }

        public void meetWith (Flow.DataflowObject o) {
            this.set.addAll(((DefineSet)o).set);
        }
        
        
        public void gen(PairDef val) {
            this.set.add(val);
        }

        public void kill(String registed) {
            Iterator<PairDef> iter = this.set.iterator();
            while(iter.hasNext()) {
                PairDef define = iter.next();
                if (define.registed.equals(registed)) {
                    iter.remove();
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DefineSet) {
                DefineSet s = (DefineSet)o;
                if (this.set.equals(s.set))
                    return true;
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder rst = new StringBuilder("[");
            Iterator<PairDef> iter = this.set.iterator();
            if (iter.hasNext()) 
                rst.append(iter.next().position);

            while (iter.hasNext()) {
                rst.append(", ");
                rst.append(iter.next().position);
            }
            rst.append(']');
            return rst.toString();
        }
    }

    private DefineSet[] in, out;
    private DefineSet entry, exit;

    /**
     * This method initializes the datflow framework.
     *
     * @param cfg  The control flow graph we are going to process.
     */
    public void preprocess(ControlFlowGraph cfg) {
        // this line must come first.
        System.out.println("Method: "+cfg.getMethod().getName().toString());

        // get the amount of space we need to allocate for the in/out arrays.
        QuadIterator qit = new QuadIterator(cfg);
        int max = 0;
        while (qit.hasNext()) {
            int id = qit.next().getID();
            if (id > max)
                max = id;
        }
        max += 1;

        // allocate the in and out arrays.
        in = new DefineSet[max];
        out = new DefineSet[max];

        // initialize the contents of in, out and universal set.
        TreeSet<PairDef> uni= new TreeSet<PairDef>();
        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            int id = qit.next().getID();
            out[id] = new DefineSet();
            in[id] = new DefineSet();
            if (!qit.getCurrentQuad().getDefinedRegisters().isEmpty()) {
                for (Operand.RegisterOperand define: qit.getCurrentQuad().getDefinedRegisters())
                    uni.add(new PairDef(define.getRegister().toString(), id));
            }
        }
        DefineSet.uniSet = uni;
        // initialize the entry and exit points.
        entry = new DefineSet();
        exit = new DefineSet();
    }

    /**
     * This method is called after the fixpoint is reached.
     * It must print out the dataflow objects associated with
     * the entry, exit, and all interior points of the CFG.
     * Unless you modify in, out, entry, or exit you shouldn't
     * need to change this method.
     *
     * @param cfg  Unused.
     */
    public void postprocess (ControlFlowGraph cfg) {
        System.out.println("entry: " + entry.toString());
        for (int i=0; i<in.length; i++) {
            if (in[i] != null) {
                System.out.println(i + " in:  " + in[i].toString());
                System.out.println(i + " out: " + out[i].toString());
            }
        }
        System.out.println("exit: " + exit.toString());
    }

    /**
     * Other methods from the Flow.Analysis interface.
     * See Flow.java for the meaning of these methods.
     * These need to be filled in.
     */
    public boolean isForward () {
        return true;
    }

    public Flow.DataflowObject getOut(Quad q) {
        Flow.DataflowObject result = newTempVar();
        result.copy(out[q.getID()]);
        return result;
    }
    public Flow.DataflowObject newTempVar() {
        return new DefineSet();
    }
    
    public void processQuad(Quad q) {
        DefineSet temp = new DefineSet();
        temp.copy(in[q.getID()]);
        for (Operand.RegisterOperand define: q.getDefinedRegisters())
            temp.kill(define.getRegister().toString());
        for (Operand.RegisterOperand define: q.getDefinedRegisters())
            temp.gen(new PairDef(define.getRegister().toString(), q.getID()));
        out[q.getID()].copy(val);
    }

    public void setIn(Quad q, Flow.DataflowObject value) {
        in[q.getID()].copy(value);
    }

    public Flow.DataflowObject getExit() {
        Flow.DataflowObject result = newTempVar();
        result.copy(exit);
        return result;
    }

    public void setEntry(Flow.DataflowObject value) {
        entry.copy(value);
    }

    public void setExit(Flow.DataflowObject value) {
        exit.copy(value);
    }

    public void setOut(Quad q, Flow.DataflowObject value) {
        out[q.getID()].copy(value);
    }

    public Flow.DataflowObject getEntry() {
        Flow.DataflowObject result = newTempVar();
        result.copy(entry);
        return result;
    }

    public Flow.DataflowObject getIn(Quad q) {
        Flow.DataflowObject result = newTempVar();
        result.copy(in[q.getID()]);
        return result;
    }
}

