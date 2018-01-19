package edu.kit.aifb.fuse;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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

import edu.kit.aifb.ldfu.api.eval.Consumer;
import edu.kit.aifb.ldfu.api.eval.cast.TripleOrQuadConsumerToTripleConsumerCast;
import edu.kit.aifb.ldfu.api.rdf.TripleOrQuad;
import edu.kit.aifb.ldfu.api.rdf.factory.ExceptionHandlerImpl;
import edu.kit.aifb.ldfu.api.rdf.factory.SimpleTermFactory;
import edu.kit.aifb.ldfu.api.rdf.factory.SyntaxException;
import edu.kit.aifb.ldfu.api.rdf.factory.TermFactory;
import edu.kit.aifb.ldfu.api.rdf.term.impl.IRIs;
import edu.kit.aifb.ldfu.format.parse.NQuadsParser;
import edu.kit.aifb.ldfu.format.serialise.TripleSerialiser;
import edu.kit.aifb.ldfu.format.serialise.impl.NTriplesSerialiser;
import edu.kit.aifb.ldfu.plan.translate.operator.physical.SafetyException;

/**
 * Extract owl:sameAs, owl:equivalentClass/Property, rdfs:subClassOf/PropertyOf triples.
 * 
 * @author aharth
 */
public class FilterMappings {
	static final Logger _log;

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$s] %5$s %n");

		_log = Logger.getLogger(FilterMappings.class.getName());
	}

	/**
	 */
	public static void main(String[] args) throws InterruptedException, XMLStreamException, SafetyException {
		Option inputO = Option.builder("i")
				.desc("Input file in NQuads format")
				.argName("file")
				.hasArg(true)
				.required()
				.build();

		Option outputO = Option.builder("o")
				.desc("Output file with mapping triples")
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
		options.addOption(outputO);
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
				formatter.printHelp(pw, 80, "FilterMappings", header, options, 2, 2, null);
				pw.flush();
				System.exit(0);
			}
		} catch (MissingOptionException e) {
			HelpFormatter formatter = new HelpFormatter();
			String header = null;
			PrintWriter pw = new PrintWriter(System.out);
			formatter.printHelp(pw, 80, "FilterMappings", header, options, 2, 2, null);
			pw.flush();
			System.exit(10);
		} catch (org.apache.commons.cli.ParseException e) {
			_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getClass().getSimpleName(), e.getMessage() });
			_log.log(Level.INFO, "Try '-h' for more information.");

			System.exit(10);
		}

		// setup
		TermFactory factory = new SimpleTermFactory(TermFactory.Mode.Lax, new ExceptionHandlerImpl());

		// process input data
		String fnamei = cmd.getOptionValue('i');
		_log.log(Level.INFO, "Opening input NQuads file {0}...", fnamei);

		InputStream is = null;

		try {
			is = new FileInputStream(fnamei);

			if (fnamei.endsWith(".gz")) {
				is = new GZIPInputStream(is);
			}
		} catch (Exception e) {
			_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getMessage(), fnamei } );
			e.printStackTrace();
			System.exit(10);
		}

		_log.log(Level.INFO, "...done");

		String fnameo = cmd.getOptionValue("o");
		_log.log(Level.INFO, "Opening output N-Triples file {0}...", fnameo);

		FileOutputStream fpos = null;
		TripleSerialiser ts = null;

		try {
			fpos = new FileOutputStream(fnameo);
			ts = new NTriplesSerialiser(fpos);
		} catch (IOException e) {
			_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getMessage(), fnamei } );
			e.printStackTrace();
			System.exit(10);
		}

		_log.log(Level.INFO, "...done");

		_log.log(Level.INFO, "Reading and writing...");

		try {
			ts.start();
		} catch (IOException e) {
			_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getMessage(), fnamei } );
			e.printStackTrace();
			System.exit(10);
		}

		NQuadsParser tpi = new NQuadsParser(factory, is, Charsets.UTF_8);
		try {
			tpi.parse(new FilterMappingTriples(new TripleOrQuadConsumerToTripleConsumerCast(ts)));

			_log.log(Level.INFO, "...done");

			is.close();
		} catch (IOException | SyntaxException e) {
			_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getMessage(), fnamei } );
			e.printStackTrace();
			System.exit(10);
		}

		_log.log(Level.INFO, "Closing files...");

		try {
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
 * Filter out triples with certain predicates.
 */
class FilterMappingTriples implements Consumer<TripleOrQuad> {
	Consumer<TripleOrQuad> _c;

	/**
	 */
	public FilterMappingTriples(Consumer<TripleOrQuad> c) {
		_c = c;
	}

	/**
	 */
	@Override
	public void consume(TripleOrQuad item) throws InterruptedException, IOException {
		if (item.getPredicate().equals(IRIs.OWL_SAMEAS)) {
			_c.consume(item);
		} else if (item.getPredicate().equals(IRIs.OWL_EQUIVALENTCLASS)) {
			_c.consume(item);
		} else if (item.getPredicate().equals(IRIs.OWL_EQUIVALENTPROPERTY)) {
			_c.consume(item);
		} else if (item.getPredicate().equals(IRIs.RDFS_SUBCLASSOF)) {
			_c.consume(item);
		} else if (item.getPredicate().equals(IRIs.RDFS_SUBPROPERTYOF)) {
			_c.consume(item);
		}
	}
}