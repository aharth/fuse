package edu.kit.aifb.fuse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLOutputFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.compress.utils.Charsets;

import edu.kit.aifb.fuse.features.PathFeature;
import edu.kit.aifb.fuse.similarity.OntologyAlignmentRec;
import edu.kit.aifb.fuse.similarity.SimilarityMeasure;
import edu.kit.aifb.ldfu.api.eval.impl.ConsumerCollection;
import edu.kit.aifb.ldfu.api.rdf.Terms;
import edu.kit.aifb.ldfu.api.rdf.factory.ExceptionHandlerImpl;
import edu.kit.aifb.ldfu.api.rdf.factory.SimpleTermFactory;
import edu.kit.aifb.ldfu.api.rdf.factory.SyntaxException;
import edu.kit.aifb.ldfu.api.rdf.factory.TermFactory;
import edu.kit.aifb.ldfu.format.parse.NxParser;


/**
 * This class converts a given resource into a similarity matrix of path features
 * 
 * @author andreas & Steffen
 */
public class SimilarityMatrix {
	
	static final Logger _log;
	static XMLOutputFactory _factory;
	static {
		_factory = XMLOutputFactory.newInstance();
		_factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);	
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$s] %5$s %n");
		_log = Logger.getLogger(MatrixGenerationLogical.class.getName());
	}
	
	public static void main(String[] args) throws IOException, SyntaxException, InterruptedException {
		
		Option nx = Option.builder("n")
				.desc("Input file for path features in Nx format")
				.argName("file")
				.hasArg(true)
				.required()
				.build();
		
		Option lambdas = Option.builder("l")
				.desc("Lambda values for weigthing head and tail similarity.")
				.argName("file")
				.hasArgs()
				.required(false)
				.build();
		
		Option output = Option.builder("o")
				.desc("Output filename for similarity matrix (filename_lambda.mtx)")
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

		options.addOption(nx);
		options.addOption(lambdas);
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
				formatter.printHelp(pw, 80, "ClusterAnalysis", header, options, 2, 2, null);
				pw.flush();
				System.exit(0);
			}
		} catch (MissingOptionException e) {
			HelpFormatter formatter = new HelpFormatter();
			String header = null;
			PrintWriter pw = new PrintWriter(System.out);
			formatter.printHelp(pw, 80, "ClusterAnalysis", header, options, 2, 2, null);
			pw.flush();
			System.exit(10);
		} catch (org.apache.commons.cli.ParseException e) {
			_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getClass().getSimpleName(), e.getMessage() });
			_log.log(Level.INFO, "Try '-h' for more information.");

			System.exit(10);
		}
		
		
		String file = cmd.getOptionValue("n");
		String outputFile = cmd.getOptionValue("o");
		
		String[] lambda = cmd.getOptionValues("l");
		double[] l = new double[0];
		if (lambda != null) {
			l = new double[lambda.length];
			for (int i = 0; i < lambda.length; i++) {
				l[i] = Double.parseDouble(lambda[i]);
			}
		}
				
		System.out.println("PROCESSING file " + file);
		computeEntitySimiarity(file, outputFile, l);	
	}
	
	
	public static void computeEntitySimiarity(String file, String outputFile, double[] lambda) throws IOException, SyntaxException, InterruptedException {
		File f = new File(file);
		FileInputStream in = new FileInputStream(f);

		TermFactory factory = new SimpleTermFactory(TermFactory.Mode.Lax, new ExceptionHandlerImpl());
		NxParser nxp = new NxParser(factory, in, Charsets.UTF_8);
		List<Terms> paths = new ArrayList<>();
		
		nxp.parse(new ConsumerCollection<Terms>(paths));
		
		// distribution of sources
		HashMap<String, Integer> sources = new HashMap<>();
		
		System.out.print("Creating source file...");
		for (Terms t : paths) {
			// retrieve occurrences of source
			Integer number = sources.get((new PathFeature(t)).getSourceString());
			if (number != null) {
				sources.put((new PathFeature(t)).getSourceString(), number + 1);
			} else {
				sources.put((new PathFeature(t)).getSourceString(), 1);
			}
		}
		
		FileWriter writerSOURCE = new FileWriter(outputFile + ".source");
		Set<String> keys = sources.keySet();
		for (String string : keys) {
			writerSOURCE.append(string + "\t" + sources.get(string) + "\n");
		}
		writerSOURCE.close();
		System.out.println(" done");
		
		for (double l : lambda) {
			System.out.print("Starting similarity measure for lambda = " + l +" ...");
			// compute distance matrix
			double [] [] distanceMatrix = new double [paths.size()] [paths.size()];
			SimilarityMeasure measure = new OntologyAlignmentRec(l);
			long s = System.currentTimeMillis();
			for (int i = 0; i < paths.size(); i++) {
				
				Terms t1 = paths.get(i);
				
				// only do n * (n-1) / 2 comparisons
				for (int j = i; j < paths.size(); j++) {
					if (i == j) {
						distanceMatrix [i] [j] = 0;
					} else {
						Terms t2 = paths.get(j);

						double distance = 1 - measure.similarity(new PathFeature(t1), new PathFeature(t2));
						distanceMatrix [i] [j] = distance;
						distanceMatrix [j] [i] = distance;
					}
				}
			}
			System.out.println(" done");
			
			// output distance matrix
			long diff = System.currentTimeMillis() -  s;
			System.out.println("Similarity matrix computed in " + diff / 1000 + " seconds");
			
			FileWriter writerMTX = new FileWriter(outputFile + "_" + l + ".mtx");
			for (int i = 0; i < distanceMatrix.length; i++) {
				for (int j = 0; j < distanceMatrix.length; j++) {
					writerMTX.append(distanceMatrix[i][j] + " ");
				}
				writerMTX.append("\n");
				writerMTX.flush();
			}
			writerMTX.flush();
			writerMTX.close();
		}
	}
}