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

    public static class DefPair implements Comparable {
        String reg;
        int pos;
        DefPair(String reg, int pos) {
            this.reg = reg;
            this.pos = pos;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DefPair) {
                DefPair p = (DefPair)o;
                return reg.equals(p.reg) && pos == p.pos;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return reg.hashCode() + new Integer(pos).hashCode();
        }

        public int compareTo(Object o) {
            DefPair p = (DefPair)o;
            if (pos > p.pos) {
                return 1;
            } else if (pos < p.pos) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Class for the dataflow objects in the ReachingDefs analysis.
     * You are free to change this class or move it to another file.
     */
    public static class DefSet implements Flow.DataflowObject {
        private Set<DefPair> set;
        public static Set<DefPair> universalSet;
        public DefSet() {
            set = new TreeSet<DefPair>();
        }
        public void setToTop() {
            set = new TreeSet<DefPair>();
        }
        public void setToBottom() {
            set = new TreeSet<DefPair>(universalSet);
        }
        public void meetWith (Flow.DataflowObject o) {
            DefSet a = (DefSet)o;
            set.addAll(a.set);
        }
        public void copy (Flow.DataflowObject o) {
            DefSet a = (DefSet)o;
            set = new TreeSet<DefPair>(a.set);
        }

        public void kill(String reg) {
            for (Iterator<DefPair> it = set.iterator(); it.hasNext();) {
                DefPair def = it.next();
                if (def.reg.equals(reg)) {
                    it.remove();
                }
            }
        }

        public void gen(DefPair val) {
            set.add(val);
        }

        /**
         * toString() method for the dataflow objects which is used
         * by postprocess() below.  The format of this method must
         * be of the form "[ID0, ID1, ID2, ...]", where each ID is
         * the identifier of a quad defining some register, and the
         * list of IDs must be sorted.  See src/test/Test.rd.out
         * for example output of the analysis.  The output format of
         * your reaching definitions analysis must match this exactly.
         */
        @Override
        public String toString() {
            StringBuilder result = new StringBuilder("[");
            Iterator<DefPair> it = set.iterator();
            if (it.hasNext()) {
                result.append(it.next().pos);
            }
            while (it.hasNext()) {
                result.append(", ");
                result.append(it.next().pos);
            }
            result.append(']');
            return result.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DefSet) {
                DefSet s = (DefSet)o;
                return set.equals(s.set);
            }
            return false;
        }
    }

    /**
     * Dataflow objects for the interior and entry/exit points
     * of the CFG. in[ID] and out[ID] store the entry and exit
     * state for the input and output of the quad with identifier ID.
     *
     * You are free to modify these fields, just make sure to
     * preserve the data printed by postprocess(), which relies on these.
     */
    private DefSet[] in, out;
    private DefSet entry, exit;

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
        in = new DefSet[max];
        out = new DefSet[max];

        // initialize the contents of in, out and universal set.
        DefSet.universalSet = new TreeSet<DefPair>();
        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            int id = qit.next().getID();
            in[id] = new DefSet();
            out[id] = new DefSet();
            if (!qit.getCurrentQuad().getDefinedRegisters().isEmpty()) {
                for (Operand.RegisterOperand def: qit.getCurrentQuad().getDefinedRegisters()) {
                    DefSet.universalSet.add(new DefPair(def.getRegister().toString(), id));
                }
            }
        }

        // initialize the entry and exit points.
        entry = new DefSet();
        exit = new DefSet();
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
    public Flow.DataflowObject getEntry() {
        Flow.DataflowObject result = newTempVar();
        result.copy(entry);
        return result;
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
    public Flow.DataflowObject getIn(Quad q) {
        Flow.DataflowObject result = newTempVar();
        result.copy(in[q.getID()]);
        return result;
    }
    public Flow.DataflowObject getOut(Quad q) {
        Flow.DataflowObject result = newTempVar();
        result.copy(out[q.getID()]);
        return result;
    }
    public void setIn(Quad q, Flow.DataflowObject value) {
        in[q.getID()].copy(value);
    }
    public void setOut(Quad q, Flow.DataflowObject value) {
        out[q.getID()].copy(value);
    }
    public Flow.DataflowObject newTempVar() {
        return new DefSet();
    }
    public void processQuad(Quad q) {
        DefSet val = new DefSet();
        val.copy(in[q.getID()]);
        for (Operand.RegisterOperand def: q.getDefinedRegisters()) {
            val.kill(def.getRegister().toString());
        }
        for (Operand.RegisterOperand def: q.getDefinedRegisters()) {
            val.gen(new DefPair(def.getRegister().toString(), q.getID()));
        }
        out[q.getID()].copy(val);
    }
}
