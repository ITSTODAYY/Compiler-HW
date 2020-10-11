package submit;

// some useful things to import. add any additional imports you need.
import joeq.Compiler.Quad.*;
import flow.Flow;
import joeq.Main.Helper;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * Skeleton class for implementing a faint variable analysis
 * using the Flow.Analysis interface.
 */
public class Faintness implements Flow.Analysis {

    /**
     * Class for the dataflow objects in the Faintness analysis.
     * You are free to change this class or move it to another file.
     */
    public static class FaintSet implements Flow.DataflowObject {
        private Set<String> set;
        public static Set<String> universalSet;
        public FaintSet() {
            set = new TreeSet<String>();
        }
        public void setToTop() {
            set = new TreeSet<String>(universalSet);
        }
        public void setToBottom() {
            set = new TreeSet<String>();
        }
        public void meetWith (Flow.DataflowObject o) {
            FaintSet s = (FaintSet)o;
            set.retainAll(s.set);
        }
        public void copy (Flow.DataflowObject o) {
            FaintSet s = (FaintSet)o;
            set = new TreeSet<String>(s.set);
        }

        public boolean isFaint(String name) {
            return set.contains(name);
        }

        public void remove(String name) {
            set.remove(name);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof FaintSet) {
                FaintSet s = (FaintSet)o;
                return set.equals(s.set);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return set.hashCode();
        }

        /**
         * toString() method for the dataflow objects which is used
         * by postprocess() below.  The format of this method must
         * be of the form "[REG0, REG1, REG2, ...]", where each REG is
         * the identifier of a register, and the list of REGs must be sorted.
         * See src/test/TestFaintness.out for example output of the analysis.
         * The output format of your reaching definitions analysis must
         * match this exactly.
         */
        @Override
        public String toString() {
            return set.toString();
        }
    }

    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        FaintSet val;

        @Override
        public void visitMove(Quad q) {
            if (!(Operator.Move.getSrc(q) instanceof Operand.RegisterOperand)) {
                return;
            }

            String srcName = ((Operand.RegisterOperand)Operator.Move.getSrc(q)).getRegister().toString();
            String dstName = Operator.Move.getDest(q).getRegister().toString();
            if (srcName.equals(dstName)) {
                return;
            }

            if (!val.isFaint(dstName)) {
                val.remove(srcName);
            }
        }

        @Override
        public void visitBinary(Quad q) {
            ArrayList<Operand> srcOps = new ArrayList<Operand>();
            srcOps.add(Operator.Binary.getSrc1(q));
            srcOps.add(Operator.Binary.getSrc2(q));
            String dstName = Operator.Binary.getDest(q).getRegister().toString();

            for (Operand srcOp: srcOps) {
                if (!(srcOp instanceof Operand.RegisterOperand)) {
                    continue;
                }
                String srcName = ((Operand.RegisterOperand)srcOp).getRegister().toString();
                if (srcName.equals(dstName)) {
                    continue;
                }
                if (!val.isFaint(dstName)) {
                    val.remove(srcName);
                }
            }
        }

        @Override
        public void visitQuad(Quad q) {
            Operator operator = q.getOperator();
            if (operator instanceof Operator.Move || operator instanceof Operator.Binary) {
                return;
            }
            for (Operand.RegisterOperand use: q.getUsedRegisters()) {
                val.remove(use.getRegister().toString());
            }
        }
    }

    private TransferFunction tranferFunction = new TransferFunction();

    /**
     * Dataflow objects for the interior and entry/exit points
     * of the CFG. in[ID] and out[ID] store the entry and exit
     * state for the input and output of the quad with identifier ID.
     *
     * You are free to modify these fields, just make sure to
     * preserve the data printed by postprocess(), which relies on these.
     */
    private FaintSet[] in, out;
    private FaintSet entry, exit;

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

        FaintSet.universalSet = new TreeSet<String>();
        int numargs = cfg.getMethod().getParamTypes().length;
        for (int i = 0; i < numargs; i++) {
            FaintSet.universalSet.add("R" + i);
        }

        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            Quad q = qit.next();
            for (Operand.RegisterOperand def: q.getDefinedRegisters()) {
                FaintSet.universalSet.add(def.getRegister().toString());
            }
            for (Operand.RegisterOperand use: q.getUsedRegisters()) {
                FaintSet.universalSet.add(use.getRegister().toString());
            }
        }

        // allocate the in and out arrays.
        in = new FaintSet[max];
        out = new FaintSet[max];

        // initialize the contents of in and out.
        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            int id = qit.next().getID();
            in[id] = new FaintSet();
            in[id].setToTop();
            out[id] = new FaintSet();
        }

        // initialize the entry and exit points.
        entry = new FaintSet();
        exit = new FaintSet();
        exit.setToTop();
        tranferFunction.val = new FaintSet();

        System.out.println("Initialization completed.");
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
        for (int i=1; i<in.length; i++) {
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
        return false;
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
        return new FaintSet();
    }
    public void processQuad(Quad q) {
        tranferFunction.val.copy(out[q.getID()]);
        Helper.runPass(q, tranferFunction);
        in[q.getID()].copy(tranferFunction.val);
    }
}
