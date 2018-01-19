package edu.kit.aifb.fuse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.compress.utils.Charsets;

import edu.kit.aifb.fuse.featureextraction.LogicalSimilarity;
import edu.kit.aifb.fuse.featureextraction.MappingsCallback;
import edu.kit.aifb.fuse.features.PathFeature;
import edu.kit.aifb.ldfu.api.eval.impl.ConsumerCollection;
import edu.kit.aifb.ldfu.api.rdf.Terms;
import edu.kit.aifb.ldfu.api.rdf.factory.ExceptionHandlerImpl;
import edu.kit.aifb.ldfu.api.rdf.factory.SimpleTermFactory;
import edu.kit.aifb.ldfu.api.rdf.factory.TermFactory;
import edu.kit.aifb.ldfu.api.rdf.term.IRI;
import edu.kit.aifb.ldfu.collection.MultiMap;
import edu.kit.aifb.ldfu.collection.mapset.MapSetFactory;
import edu.kit.aifb.ldfu.collection.mapset.impl.MapSetFactoryJavaConcurrent;
import edu.kit.aifb.ldfu.format.parse.NxParser;
import edu.kit.aifb.ldfu.format.parse.TurtleParser;
import edu.kit.aifb.ldfu.plan.translate.operator.physical.SafetyException;

/**
 * Generate the similarity matrix based on the path features and the logical mappings.
 * 
 * The index of the path feature in the file corresponds to the row/column of the matrix.
 * Output matrix contains either 0 (no mapping) or 1 (mapping).
 * 
 * @author aharth
 */
public class MatrixGenerationLogical {
	static final Logger _log;

	static XMLOutputFactory _factory;

	static {
		_factory = XMLOutputFactory.newInstance();
		_factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);	

		System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$s] %5$s %n");

		_log = Logger.getLogger(MatrixGenerationLogical.class.getName());
	}

	/**
	 */
	public static void main(String[] args) throws InterruptedException, XMLStreamException, SafetyException {
		Option input = Option.builder("i")
				.desc("Input file for path features in Nx format")
				.argName("file")
				.hasArg(true)
				.required()
				.build();

		Option mappings = Option.builder("m")
				.desc("Input files for mappings (owl:sameAs, owl:equivalentClass/Property, rdfs:subClassOf/PropertyOf) in Turtle format")
				.argName("file")
				.hasArgs()
				.required()
				.build();

		Option output = Option.builder("o")
				.desc("Output file for similarity matrix")
				.argName("file")
				.hasArg(true)
				.build();

		Option helpO = Option.builder("h")
				.longOpt("help")
				.desc("Print help")
				.hasArg(false)
				.build();

		Options options = new Options();

		options.addOption(input);
		options.addOption(mappings);
		options.addOption(output);
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

		// read path features
		_log.log(Level.INFO, "Reading path features from {0}...", cmd.getOptionValue("i"));

		// list with path features (needs to have order so that the indices in the matrix match)
		List<Terms> paths = new ArrayList<>();

		InputStream is = null;

		try {
			is = new FileInputStream(cmd.getOptionValue("i"));
			NxParser nxp = new NxParser(factory, is, Charsets.UTF_8);
			nxp.parse(new ConsumerCollection<Terms>(paths));
		} catch (Exception e) {
			_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getMessage(), cmd.getOptionValue("i") } );
			e.printStackTrace();
			System.exit(10);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getMessage(), cmd.getOptionValue("i") } );
				e.printStackTrace();
				System.exit(10);
			}
		}

		_log.log(Level.INFO, "...done ({0} path features)", paths.size());

		// read mappings
		MapSetFactory msfactory = new MapSetFactoryJavaConcurrent();

		MultiMap<IRI, IRI> mapclass = msfactory.createMultiMap();
		MultiMap<IRI, IRI> mapprop = msfactory.createMultiMap();
		MultiMap<IRI, IRI> mapsameas = msfactory.createMultiMap();

		MappingsCallback tcv = new MappingsCallback(mapclass, mapprop, mapsameas);

		for (String fname : cmd.getOptionValues('m')) {
			_log.log(Level.INFO, "Reading {0}...", fname);

			try {
				is = new FileInputStream(fname);

				if (fname.endsWith(".gz")) {
					is = new GZIPInputStream(is);
				}

				TurtleParser tpv = new TurtleParser(factory, new File(fname).toURI().toString(), is, Charsets.UTF_8);
				tpv.parse(tcv);
			} catch (Exception e) {
				_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getMessage(), fname } );
				e.printStackTrace();
				System.exit(10);
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getMessage(), fname } );
					e.printStackTrace();
					System.exit(10);
				}
			}
			_log.log(Level.INFO, "...done");
		}

		// the similarity measure for the "logical" similarity based on the owl mappings
		LogicalSimilarity sim = new LogicalSimilarity(factory, mapclass, mapprop, mapsameas);

		// open output file
		FileWriter fw = null;

		try {
			File fo = new File(cmd.getOptionValue("o"));
			fw = new FileWriter(fo);

			// iterate over paths, compare everything with everything
			for (Iterator<Terms> it0 = paths.iterator(); it0.hasNext(); ) {
				Terms p0 = it0.next();
				for (Iterator<Terms> it1 = paths.iterator(); it1.hasNext(); ) {
					Terms p1 = it1.next();

					_log.log(Level.FINE, "Computing distance between {0} and {1}", new Object[] { p0, p1 } );

					double d = sim.distance(new PathFeature(p0), new PathFeature(p1));

					if (d == 0 && !p0.equals(p1)) {
						_log.log(Level.FINE, "Match {0} equal to {1}", new Object[] { p0, p1 } );
					}

					fw.write(d + " ");
				}

				fw.write("\n");
			}
		} catch (IOException e) {
			_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getMessage(), cmd.getOptionValue("o") } );
			e.printStackTrace();
			System.exit(10);
		} finally {
			try {
				fw.close();
			} catch (IOException e) {
				_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getMessage(), cmd.getOptionValue("o") } );
				e.printStackTrace();
				System.exit(10);
			}
		}

		_log.log(Level.INFO, "...done");
	}
}