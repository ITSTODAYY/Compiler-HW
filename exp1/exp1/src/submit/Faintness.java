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
    public class FaintStructure implements Flow.DataflowObject {
        /**
         * Methods from the Flow.DataflowObject interface.
         * See Flow.java for the meaning of these methods.
         * These need to be filled in.
         */
        public static Set<String> uniSet;
        public Set<string> set;
        public FaintStructure(){
            this.set = new TreeSet<String>();
        }

        public void setToTop() {
            this.set = new TreeSet<String>(this.uniSet);
        }

        public void setToBottom() {
            this.set = new TreeSet<String>();
        }

        public void meetWith (Flow.DataflowObject o) {
            FaintStructure s = (FaintStructure) o;
            this.set.retainAll(s.set);
        }

        public void copy (Flow.DataflowObject o) {
            this.set = new TreeSet<String>(((FaintStructure) o).set);
        }

        public void remove(String name){
            this.set.remove(name);
        }

        public boolean isFaint(String name){
            return this.set.contains(name);
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
        public boolean equals(Object o){
            if (!(o instanceof FaintStructure)){
                return false
            }
            FaintStructure s = (FaintStructure) o;
            return this.set.equals(s.set);
        }

        @Override
        public int hashCode(){
            return this.set.hashCode();
        }

        @Override
        public String toString() { 
            return this.set.toString(); 
        }
    }

    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        FaintStructure val;
        @Override
        public void visitBinary(Quad q) {
            ArrayList<Operand> sourceOps = new ArrayList<Operand>();
            sourceOps.add(Operator.Binary.getSrc1(q));
            sourceOps.add(Operator.Binary.getSrc2(q));
            String destName = Operator.Binary.getDest(q).getRegister().toString();
            for (Operand sourceOp: sourceOps) {
                if (!(sourceOp instanceof Operand.RegisterOperand))
                    continue;
                String sourceName = ((Operand.RegisterOperand)sourceOp).getRegister().toString();
                if (souceName.equals(destName))
                    continue;
                if (!val.isFaint(destName))
                    val.remove(sourceName);
            }
        }

        @Override
        public void visitMove(Quad q) {
            if (!(Operator.Move.getSrc(q) instanceof Operand.RegisterOperand))
                return;
            
            String sourceName = ((Operand.RegisterOperand)Operator.Move.getSrc(q)).getRegister().toString();
            String destName = Operator.Move.getDest(q).getRegister().toString();
            if (sourceName.equals(destName))
                return;
        
            if (!val.isFaint(destName))
                val.remove(sourceName);
        }

        @Override
        public void visitQuad(Quad q) {
            Operator op = q.getOperator();
            if (op instanceof Operator.Move)
                return;
            if (op instanceof Operator.Binary)
                return;
            for (Operand.RegisterOperand used: q.getUsedRegisters()) {
                val.remove(used.getRegister().toString());
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
    private FaintStructure[] in, out;
    private FaintStructure entry, exit;

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

        // build universe set
        int numz = cfg.getMethod().getParamTypes().length;
        TreeSet<String> uni = new TreeSet<String>();
        for (int j = 0; j < numz; j ++ )
            uni.add("R" + (string)i ); 
        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            Quad q = qit.next();
            for (Operand.RegisterOperand use: q.getUsedRegisters())
                uni.add(use.getRegister().toString());
            for (Operand.RegisterOperand def: q.getDefinedRegisters()) 
                uni.add(def.getRegister().toString());
        }
        FaintStructure.uniSet = uni;


        // allocate the in and out arrays.
        in = new MyDataflowObject[max];
        out = new MyDataflowObject[max];

        // initialize the contents of in and out.
        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            int id = qit.next().getID();
            in[id] = new MyDataflowObject();
            out[id] = new MyDataflowObject();
        }

        // initialize the entry and exit points.
        entry = new MyDataflowObject();
        exit = new MyDataflowObject();

        /************************************************
         * Your remaining initialization code goes here *
         ************************************************/
        exit.setToTop();
        tranferFunction.val = new FaintStructure();

        System.out.println("Initialization done.");
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
    public void setIn(Quad q, Flow.DataflowObject value) {
        in[q.getID()].copy(value);
    }

    public void setOut(Quad q, Flow.DataflowObject value) {
        out[q.getID()].copy(value);
    }

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

    public Flow.DataflowObject newTempVar() {
        return new FaintStructure();
    }

    public void processQuad(Quad q) {
        tranferFunction.val.copy(out[q.getID()]);
        Helper.runPass(q, tranferFunction);
        in[q.getID()].copy(tranferFunction.val);
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
}
