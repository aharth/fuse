package edu.kit.aifb.fuse.featureextraction;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.kit.aifb.ldfu.api.query.SelectClause;
import edu.kit.aifb.ldfu.api.query.SelectQuery;
import edu.kit.aifb.ldfu.api.query.SparqlQuery;
import edu.kit.aifb.ldfu.api.query.fun.Expression;
import edu.kit.aifb.ldfu.api.query.fun.ExpressionComparison;
import edu.kit.aifb.ldfu.api.query.fun.ExpressionFunction;
import edu.kit.aifb.ldfu.api.query.impl.BasicGraphPatternImpl;
import edu.kit.aifb.ldfu.api.query.impl.FilterConditionImpl;
import edu.kit.aifb.ldfu.api.query.impl.TriplePatternImpl;
import edu.kit.aifb.ldfu.api.query.impl.VariableImpl;
import edu.kit.aifb.ldfu.api.query.pattern.BasicGraphPattern;
import edu.kit.aifb.ldfu.api.query.pattern.GraphGraphPattern;
import edu.kit.aifb.ldfu.api.query.pattern.GroupGraphPattern;
import edu.kit.aifb.ldfu.api.query.pattern.TriplePattern;
import edu.kit.aifb.ldfu.api.rdf.factory.TermFactory;
import edu.kit.aifb.ldfu.api.rdf.term.impl.IRIs;
import edu.kit.aifb.ldfu.collection.distinct.DistinctFilterFactory;
import edu.kit.aifb.ldfu.collection.distinct.impl.DistinctFilterFactoryJava;
import edu.kit.aifb.ldfu.collection.mapset.MapSetFactory;
import edu.kit.aifb.ldfu.collection.mapset.impl.MapSetFactoryJava;
import edu.kit.aifb.ldfu.operator.logical.Operator;
import edu.kit.aifb.ldfu.operator.logical.PrintOperatorPlanDot;
import edu.kit.aifb.ldfu.operator.physical.Evaluator;
import edu.kit.aifb.ldfu.operator.physical.PrintEvaluatorPlanDot;
import edu.kit.aifb.ldfu.plan.translate.operator.logical.TranslateProgram;
import edu.kit.aifb.ldfu.plan.translate.operator.physical.SafetyException;
import edu.kit.aifb.ldfu.plan.translate.operator.physical.TranslateLogicalOperatorPlan;
import edu.kit.aifb.ldfu.run.process.PlanProcessor;
import edu.kit.aifb.ldfu.run.process.PlanProcessorImpl;

/**
 * The query plan for creating path features.
 * Based on a set of SPARQL queries.
 * 
 * @author aharth
 */
public class QueryPlan {
	static final Logger _log = Logger.getLogger(QueryPlan.class.getName());

	/**
	 */
	public static SelectQuery getPath1(TermFactory factory) {
    	TriplePattern tp0 = new TriplePatternImpl(new VariableImpl("s"), new VariableImpl("p"), new VariableImpl("ol"));
    	TriplePattern tp1 = new TriplePatternImpl(factory.createBlankNode(), factory.createIRI("http://xmlns.com/foaf/0.1/focus"), new VariableImpl("s"));
    	TriplePattern tp2 = new TriplePatternImpl(new VariableImpl("p"), IRIs.RDFS_LABEL, new VariableImpl("pl"));

    	BasicGraphPattern bgp = new BasicGraphPatternImpl();
    	bgp.add(tp1);
    	bgp.add(tp2);

    	GroupGraphPattern qc = new GroupGraphPattern(bgp);

    	BasicGraphPattern bgp1 = new BasicGraphPatternImpl();
    	bgp1.add(tp0);

    	GraphGraphPattern ggp = new GraphGraphPattern(new VariableImpl("g"), new GroupGraphPattern(bgp1));
    	qc.add(ggp);

    	Expression expr = new ExpressionFunction(IRIs.SPARQL_ISLITERAL, new VariableImpl("ol"));
    	qc.add(new FilterConditionImpl(expr));

    	return new SelectQuery(qc, new SelectClause(new VariableImpl("s"), new VariableImpl("p"), new VariableImpl("ol"), new VariableImpl("pl"), new VariableImpl("g")));
	}

	/**
	 */
	public static SelectQuery getPath2(TermFactory factory) {
    	TriplePattern tp0 = new TriplePatternImpl(new VariableImpl("s"), new VariableImpl("p"), new VariableImpl("o"));
    	TriplePattern tp1 = new TriplePatternImpl(new VariableImpl("o"), new VariableImpl("p2"), new VariableImpl("ol"));
    	TriplePattern tp2 = new TriplePatternImpl(factory.createBlankNode(), factory.createIRI("http://xmlns.com/foaf/0.1/focus"), new VariableImpl("s"));
    	TriplePattern tp3 = new TriplePatternImpl(new VariableImpl("p"), IRIs.RDFS_LABEL, new VariableImpl("pl"));
    	TriplePattern tp4 = new TriplePatternImpl(new VariableImpl("p2"), IRIs.RDFS_LABEL, new VariableImpl("p2l"));

    	BasicGraphPattern bgp = new BasicGraphPatternImpl();
    	bgp.add(tp1);
    	bgp.add(tp2);
    	bgp.add(tp3);
    	bgp.add(tp4);

    	GroupGraphPattern qc = new GroupGraphPattern(bgp);

    	BasicGraphPattern bgp1 = new BasicGraphPatternImpl();
    	bgp1.add(tp0);

    	GraphGraphPattern ggp = new GraphGraphPattern(new VariableImpl("g"), new GroupGraphPattern(bgp1));
    	qc.add(ggp);

    	Expression expr1 = new ExpressionFunction(IRIs.SPARQL_ISLITERAL, new VariableImpl("ol"));
    	qc.add(new FilterConditionImpl(expr1));

    	Expression expr2 = new ExpressionComparison(IRIs.SPARQL_NOTEQUAL, new VariableImpl("p"), IRIs.OWL_SAMEAS);
    	qc.add(new FilterConditionImpl(expr2));

    	Expression expr3 = new ExpressionComparison(IRIs.SPARQL_NOTEQUAL, new VariableImpl("s"), new VariableImpl("o"));
    	qc.add(new FilterConditionImpl(expr3));

    	return new SelectQuery(qc, new SelectClause(new VariableImpl("s"), new VariableImpl("p"), new VariableImpl("o"), new VariableImpl("p2"), new VariableImpl("ol"), new VariableImpl("pl"), new VariableImpl("p2l"), new VariableImpl("g")));
	}

	/**
     */
    public static PlanProcessor getPlanProcessor(TermFactory tfactory, Collection<SparqlQuery> queries) throws SafetyException {
		// create plan
		Collection<Operator> ops = TranslateProgram.translate(null, queries);

		PrintOperatorPlanDot pop = new PrintOperatorPlanDot(ops);
		_log.log(Level.CONFIG, "{0}", pop);

		MapSetFactory cfactory = new MapSetFactoryJava();
		DistinctFilterFactory dfactory = new DistinctFilterFactoryJava();

		Collection<Evaluator> evs = TranslateLogicalOperatorPlan.translate(tfactory, cfactory, dfactory, ops);

		PrintEvaluatorPlanDot pep = new PrintEvaluatorPlanDot(evs);
		_log.log(Level.CONFIG, "{0}", pep);

		PlanProcessor planproc = new PlanProcessorImpl(ops, evs);

		return planproc;
	}
}