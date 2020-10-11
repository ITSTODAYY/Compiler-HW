package submit;

// some useful things to import. add any additional imports you need.
import joeq.Compiler.Quad.*;
import flow.Flow;

import java.util.ArrayList;

/**
 * Skeleton class for implementing the Flow.Solver interface.
 */
public class MySolver implements Flow.Solver {

    protected Flow.Analysis analysis;

    /**
     * Sets the analysis.  When visitCFG is called, it will
     * perform this analysis on a given CFG.
     *
     * @param analyzer The analysis to run
     */
    public void registerAnalysis(Flow.Analysis analyzer) {
        this.analysis = analyzer;
    }

    /**
     * Runs the solver over a given control flow graph.  Prior
     * to calling this, an analysis must be registered using
     * registerAnalysis
     *
     * @param cfg The control flow graph to analyze.
     */
    public void visitCFG(ControlFlowGraph cfg) {

        // this needs to come first.
        analysis.preprocess(cfg);

        Quad entryQuad = null;
        ArrayList<Quad> exitQuads = new ArrayList<Quad>();

        QuadIterator qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            qit.next();
            if (qit.successors1().contains(null)) {
                exitQuads.add(qit.getCurrentQuad());
            }
            if (qit.predecessors1().contains(null)) {
                entryQuad = qit.getCurrentQuad();
            }
        }

        if (entryQuad == null || exitQuads.isEmpty()) {
            return;
        }

        if (analysis.isForward()) {
            boolean converged = false;
            while (!converged) {
                converged = true;
                qit = new QuadIterator(cfg, true);
                while (qit.hasNext()) {
                    Quad curQuad = qit.next();
                    Flow.DataflowObject newIn = analysis.newTempVar();
                    for (Quad preQuad: qit.predecessors1()) {
                        if (preQuad == null) {
                            newIn.meetWith(analysis.getEntry());
                        } else {
                            newIn.meetWith(analysis.getOut(preQuad));
                        }
                    }
                    analysis.setIn(curQuad, newIn);
                    Flow.DataflowObject oldOut = analysis.getOut(curQuad);
                    analysis.processQuad(curQuad);
                    if (!analysis.getOut(curQuad).equals(oldOut)) {
                        converged = false;
                    }
                }
            }
            Flow.DataflowObject exitValue = analysis.newTempVar();
            exitValue.setToTop();
            for (Quad q: exitQuads) {
                exitValue.meetWith(analysis.getOut(q));
            }
            analysis.setExit(exitValue);
        } else {
            boolean converged = false;
            while (!converged) {
                converged = true;
                qit = new QuadIterator(cfg, false);
                while (qit.hasPrevious()) {
                    Quad curQuad = qit.previous();
                    Flow.DataflowObject newOut = analysis.newTempVar();
                    newOut.setToTop();
                    for (Quad succQuad: qit.successors1()) {
                        if (succQuad == null) {
                            newOut.meetWith(analysis.getExit());
                        } else {
                            newOut.meetWith(analysis.getIn(succQuad));
                        }
                    }
                    analysis.setOut(curQuad, newOut);
                    Flow.DataflowObject oldIn = analysis.getIn(curQuad);
                    analysis.processQuad(curQuad);
                    if (!analysis.getIn(curQuad).equals(oldIn)) {
                        converged = false;
                    }
                }
            }
            analysis.setEntry(analysis.getIn(entryQuad));
        }

        // this needs to come last.
        analysis.postprocess(cfg);
    }
}
