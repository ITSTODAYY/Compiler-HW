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

        ArrayList<Quad> exitQ = new ArrayList<Quad>();
        Quad entryQ = null;
        QuadIterator quadIter = new QuadIterator(cfg);

        // analyse the map
        while (quadIter.hasNext()) {
            quadIter.next();
            boolean addQE = quadIter.successors1().contains(null);
            boolean reIQ = quadIter.predecessors1().contains(null);
            Quad cq = quadIter.getCurrentQuad();
            if (reIQ)
                entryQ = cq;
            if (addQE)
                exitQ.add(cq);
        }

        if (exitQ.isEmpty())
            return;
        if (entryQ == null)
            return;

        boolean isConverge = false;

        if (!analysis.isForward()){
            // backward analysis
            for (;!isConverge; isConverge = true){
                quadIter = new QuadIterator(cfg, false);
                while (quadIter.hasPrevious()){
                    Flow.DataflowObject opt = analysis.newTempVar();
                    opt.setToTop();
                    Quad cq = quadIter.previous();
                    for(Quad sq: quadIter.successors1()){
                        if (sq == null){
                            opt.meetWith(analysis.getExit());
                            continue;
                        }
                        opt.meetWith(analysis.getIn(sq));
                    }

                    analysis.serOut(cq, opt);
                    Flow.DataflowObject ipt = analysis.getIn(cq);
                    analysis.processQuad(cq);

                    //check converage
                    if (analysis.getIn(cq).equals(ipt)){
                        continue;
                    }
                    isConverge = false;     
                }  
            }
            analysis.setEntry(analysis.getIn(entryQ));
        }
        if (analysis.isForward()){
            // forward analysis
            for (;!isConverge; isConverge = true){
                quadIter = new QuadIterator(cfg, true);
                while (quadIter.hasNext()){
                    Flow.DataflowObject ipt = analysis.newTempVar();
                    Quad cq = quadIter.next();
                    for (Quad pq: quadIter.predecessors1()){
                        if (pq == null){
                            ipt.meetWith(analysis.getEntry());
                            continue;
                        }
                        ipt.meetWith(analysis.getOut(pq));
                    }
                    analysis.setIn(cq, ipt);
                    Flow.DataflowObject opt = analysis.getOut(cq);
                    analysis.processQuad(cq);
                    if (analysis.getOut(cq).equals(opt)){
                        continue;
                    }
                    isConverge = false;
                }
            }
            Flow.DataflowObject exitVal = analysis.newTempVar();
            exitVal.setToTop();
            for(Quad q: exitQ)
                exitVal.meetWith(analysis.getOut(q));
            analysis.setExit(exitVal);
        }
        // this needs to come last.
        analysis.postprocess(cfg);
    }
}
