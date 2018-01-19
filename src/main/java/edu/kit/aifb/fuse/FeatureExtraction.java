package edu.kit.aifb.fuse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.compress.utils.Charsets;

import edu.kit.aifb.fuse.featureextraction.QueryPlan;
import edu.kit.aifb.ldfu.api.eval.Consumer;
import edu.kit.aifb.ldfu.api.eval.impl.ConsumerCollection;
import edu.kit.aifb.ldfu.api.query.SelectQuery;
import edu.kit.aifb.ldfu.api.query.SparqlQuery;
import edu.kit.aifb.ldfu.api.rdf.Terms;
import edu.kit.aifb.ldfu.api.rdf.TripleOrQuad;
import edu.kit.aifb.ldfu.api.rdf.factory.ExceptionHandlerImpl;
import edu.kit.aifb.ldfu.api.rdf.factory.SimpleTermFactory;
import edu.kit.aifb.ldfu.api.rdf.factory.TermFactory;
import edu.kit.aifb.ldfu.api.rdf.term.Literal;
import edu.kit.aifb.ldfu.api.rdf.term.impl.IRIs;
import edu.kit.aifb.ldfu.format.parse.NQuadsParser;
import edu.kit.aifb.ldfu.format.parse.turtle.TokenMgrError;
import edu.kit.aifb.ldfu.format.serialise.TermsSerialiser;
import edu.kit.aifb.ldfu.format.serialise.impl.SparqlResultsNxSerialiser;
import edu.kit.aifb.ldfu.operator.physical.PrintEvaluatorPlanDot;
import edu.kit.aifb.ldfu.plan.translate.operator.physical.SafetyException;
import edu.kit.aifb.ldfu.run.process.PlanProcessor;

/**
 * Extract path features from an input file in Turtle format.
 * The input file needs to contain "foaf:focus" triples that specify the set of URIs for an entity.
 * 
 * @author aharth
 */
public class FeatureExtraction {
	static final Logger _log;

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$s] %5$s %n");

		_log = Logger.getLogger(FeatureExtraction.class.getName());
	}

	/**
	 */
	public static void main(String[] args) throws InterruptedException, XMLStreamException, SafetyException {
		Option inputO = Option.builder("i")
				.desc("Input file in N-Quads format")
				.argName("file")
				.hasArg(true)
				.required()
				.build();

		Option pathO = Option.builder("o")
				.desc("Output file for path features")
				.argName("file")
				.hasArg(true)
				.required()
				.build();

		Option helpO = Option.builder("h")
				.longOpt("help")
				.desc("Print help")
				.hasArg(false)
				.build();

		Options options = new Options();

		options.addOption(inputO);
		options.addOption(pathO);
		options.addOption(helpO);

		CommandLineParser cmdparser = new DefaultParser();
		CommandLine cmd = null;

		// check for help or version option
		try {
			// throw an exception on unknown options
			cmd = cmdparser.parse(options, args);

			if (cmd.hasOption('h')) {
				HelpFormatter formatter = new HelpFormatter();
				String header = null;
				PrintWriter pw = new PrintWriter(System.out);
				formatter.printHelp(pw, 80, "FeatureExtraction", header, options, 2, 2, null);
				pw.flush();
				System.exit(0);
			}
		} catch (MissingOptionException e) {
			HelpFormatter formatter = new HelpFormatter();
			String header = null;
			PrintWriter pw = new PrintWriter(System.out);
			formatter.printHelp(pw, 80, "FeatureExtraction", header, options, 2, 2, null);
			pw.flush();
			System.exit(10);
		} catch (org.apache.commons.cli.ParseException e) {
			_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getClass().getSimpleName(), e.getMessage() });
			_log.log(Level.INFO, "Try '-h' for more information.");

			System.exit(10);
		}

		// setup
		TermFactory factory = new SimpleTermFactory(TermFactory.Mode.Lax, new ExceptionHandlerImpl());

		_log.log(Level.INFO, "Generating path features...");

		// create query plan processor
		Set<Terms> path1 = new HashSet<>();
		Set<Terms> path2 = new HashSet<>();

		Consumer<Terms> tc1 = new ConsumerCollection<>(path1);
		Consumer<Terms> tc2 = new ConsumerCollection<>(path2);

		Set<SparqlQuery> queries = new HashSet<>();
		SelectQuery q1 = QueryPlan.getPath1(factory);
		SelectQuery q2 = QueryPlan.getPath2(factory);
		queries.add(q1);
		queries.add(q2);

		_log.log(Level.INFO, "path-1: {0}", q1.sparqlString());
		_log.log(Level.INFO, "path-2: {0}", q2.sparqlString());

		PlanProcessor planproc = QueryPlan.getPlanProcessor(factory, queries);

		// register consumer for query
		planproc.registerResultTermsConsumer(q1, tc1);
		planproc.registerResultTermsConsumer(q2, tc2);

		// process input data
		String fnamei = cmd.getOptionValue('i');
		_log.log(Level.INFO, "Reading RDF dataset from {0}...", fnamei);

		InputStream is = null;

		try {
			is = new FileInputStream(fnamei);

			if (fnamei.endsWith(".gz")) {
				is = new GZIPInputStream(is);
			}

			NQuadsParser tpi = new NQuadsParser(factory, is, Charsets.UTF_8);
			tpi.parse(new TripleOrQuadFilterConsumer(planproc.getInputConsumer()));
		} catch (Exception | TokenMgrError e) {
			_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getMessage(), fnamei } );
			e.printStackTrace();
			System.exit(10);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getMessage(), fnamei } );
				e.printStackTrace();
				System.exit(10);
			}
		}

		_log.log(Level.INFO, "...done");

		_log.log(Level.INFO, "Plan stats: {0}", planproc.getStatistics());

		PrintEvaluatorPlanDot pepd = new PrintEvaluatorPlanDot(planproc.getRootEvaluators());
		//_log.log(Level.INFO, "{0}", pepd);

		_log.log(Level.INFO, "Term factory stats: {0}", factory);

		_log.log(Level.INFO, "Path-1 features: {0}", path1.size());
		_log.log(Level.INFO, "Path-2 features: {0}", path2.size());

		_log.log(Level.INFO, "Writing path features...");

		try {
			File fp = new File(cmd.getOptionValue("o"));
			FileOutputStream fpos = new FileOutputStream(fp);
			TermsSerialiser ts = new SparqlResultsNxSerialiser(fpos);

			// should be a combination of q1 and q2
			// rather, a single UNION query
			ts.setVariables(q2.getSelectClause().getSelectVariables());
			ts.start();

			for (Terms t : path1) {
				ts.consume(t);
			}

			for (Terms t : path2) {
				ts.consume(t);
			}

			ts.end();

			ts.close();
			fpos.close();
		} catch (IOException e) {
			_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getMessage(), fnamei } );
			e.printStackTrace();
			System.exit(10);
		}

		_log.log(Level.INFO, "...done");
	}
}

/**
 * Cast a Consumer<Terms> into a Consumer<TripleOrQuad>.
 * 
 * Also: filter out triples with mapping predicates, and literals with language tag other than @en.
 * 
 * @author aharth
 */
class TripleOrQuadFilterConsumer implements Consumer<TripleOrQuad> {
	Consumer<TripleOrQuad> _c;

	/**
	 */
	public TripleOrQuadFilterConsumer(Consumer<TripleOrQuad> c) {
		_c = c;
	}

	/**
	 */
	@Override
	public void consume(TripleOrQuad item) throws InterruptedException, IOException {
		// only permit literals without language tag or english language tag
		if (item.getObject() instanceof Literal) {
			Literal l = (Literal)item.getObject();
			String lang = l.getLanguageTag();

			if (lang != null) {
				if (!lang.toLowerCase().equals("en")) {
					return;
				}
			}
		}

		// only permit non mapping triples
		if (item.getPredicate().equals(IRIs.OWL_SAMEAS) ||
				item.getPredicate().equals(IRIs.OWL_EQUIVALENTCLASS) ||
				item.getPredicate().equals(IRIs.OWL_EQUIVALENTPROPERTY) ||
				item.getPredicate().equals(IRIs.RDFS_SUBCLASSOF) ||
				item.getPredicate().equals(IRIs.RDFS_SUBPROPERTYOF)) {
			return;
		}

		_c.consume(item);
	}
}