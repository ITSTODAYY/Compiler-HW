package submit;

// some useful things to import. add any additional imports you need.
import joeq.Compiler.Quad.*;
import flow.Flow;

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
            addQE = quadIter.successors1().contains(null);
            reIQ = quadIter.predecessors1().contains(null);
            cq = quadIter.getCurrentQuad();
            if (reIQ)
                entryQ = cq;
            if (addQE)
                exitQ.add(cq)
        }

        if (exitQ.isEmpty())
            return
        if (entryQ == null)
            return

        boolean isConverge = false;

        if (!analysis.isForward()){
            // backward analysis
            for (;!isConverge; isConverge = true){
                quadIter = new QuadIterator(cfg, false);
                while (quadIter.hasPrevious()){
                    var opt = analysis.newTempVar();
                    opt.setToTop();
                    var cq = quadIter.previous();
                    for(var sq: quadIter.successors1()){
                        if (sq == null){
                            opt.meetWith(analysis.getExit());
                            continue;
                        }
                        opt.meetWith(analysis.getIn(sq));
                    }

                    analysis.serOut(cq, opt);
                    var ipt = analysis.getIn(cq);
                    analysis.processQuad(cq);

                    //check converage
                    if (analysis.getIn(cq).equals(oldIn)){
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
                    var ipt = analysis.newTempVar();
                    var cq = quadIter.next();
                    for (var pq: quadIter.predecessors1()){
                        if (pq == null){
                            ipt.meetWith(analysis.getEntry());
                            continue;
                        }
                        ipt.meetWith(analysis.getOut(pq));
                    }
                    analysis.setIn(cq, ipt);
                    var opt = analysis.getOut(cq);
                    analysis.processQuad(cq);
                    if (analysis.getOut(cq).equals(oldIn)){
                        continue;
                    }
                    isConverge = false;
                }
            }
            var exitVal = analysis.newTempVar();
            exitVal.setToTop();
            for(var q: exitQ)
                exitVal.meetWith(analysis.getOut(q));
            analysis.setExit(exitVal);
        }
        // this needs to come last.
        analysis.postprocess(cfg);
    }
}
