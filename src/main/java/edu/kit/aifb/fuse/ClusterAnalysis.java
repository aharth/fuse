package edu.kit.aifb.fuse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.compress.utils.Charsets;

import edu.kit.aifb.fuse.cluster.Cluster;
import edu.kit.aifb.fuse.features.PathFeature;
import edu.kit.aifb.ldfu.api.eval.impl.ConsumerCollection;
import edu.kit.aifb.ldfu.api.rdf.Terms;
import edu.kit.aifb.ldfu.api.rdf.factory.ExceptionHandlerImpl;
import edu.kit.aifb.ldfu.api.rdf.factory.SimpleTermFactory;
import edu.kit.aifb.ldfu.api.rdf.factory.SyntaxException;
import edu.kit.aifb.ldfu.api.rdf.factory.TermFactory;
import edu.kit.aifb.ldfu.api.rdf.impl.TermsImpl;
import edu.kit.aifb.ldfu.format.parse.NxParser;

/**
 * Implements two steps of the pipeline: cluster merging and representative selection.
 * 
 * @author andreas & Steffen
 *
 */
public class ClusterAnalysis {
	
	static final Logger _log;
	static XMLOutputFactory _factory;
	static {
		_factory = XMLOutputFactory.newInstance();
		_factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);	
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$s] %5$s %n");
		_log = Logger.getLogger(MatrixGenerationLogical.class.getName());
	}
	
	public static void main(String[] args) throws IOException, URISyntaxException, SyntaxException, InterruptedException, NumberFormatException, XMLStreamException {
		
		Option nx = Option.builder("n")
				.desc("Input file for path features in Nx format")
				.argName("file")
				.hasArg(true)
				.required()
				.build();

		Option clu = Option.builder("c")
				.desc("Input file for clusters (.clu)")
				.argName("file")
				.hasArg(true)
				.required()
				.build();
		
		Option lambda = Option.builder("l")
				.desc("Lambda value for weigthing head and tail similarity.")
				.argName("file")
				.hasArg(true)
				.required()
				.build();
		
		Option remove = Option.builder("s")
				.desc("1-entry clusters will be removed.")
				.argName("boolean")
				.hasArg(false)
				.build();
		
		Option representatives = Option.builder("r")
				.desc("Representatives will be removed.")
				.argName("boolean")
				.hasArg(false)
				.build();
		
		Option epsilons = Option.builder("e")
				.desc("Epsilon values for merging.")
				.argName("file")
				.hasArgs()
				.required()
				.build();
		
		Option output = Option.builder("o")
				.desc("Output filename for clusters (filename_epsilon.xml)")
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
		options.addOption(clu);
		options.addOption(lambda);
		options.addOption(epsilons);
		options.addOption(remove);
		options.addOption(representatives);
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
			System.out.println(e);
			System.exit(10);
		} catch (org.apache.commons.cli.ParseException e) {
			_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getClass().getSimpleName(), e.getMessage() });
			_log.log(Level.INFO, "Try '-h' for more information.");
			System.out.println(e);
			System.exit(10);
		}
		
		String nxFile = cmd.getOptionValue("n");
		
		String cluFile = cmd.getOptionValue("c");
		String outputFile = cmd.getOptionValue("o");
		
		String[] epsilon = cmd.getOptionValues("e");
		Double[] eps = new Double[epsilon.length];
		for (int i = 0; i < eps.length; i++) {
			eps[i] = Double.parseDouble(epsilon[i]);
		}
		
		Arrays.sort(eps, Collections.reverseOrder());
		boolean removeSingle = cmd.hasOption("s");
		boolean removeRepresentatives = cmd.hasOption("r");
		mergeAndRepresentative(nxFile, cluFile, Double.parseDouble(cmd.getOptionValue("l")), eps, removeSingle, removeRepresentatives, outputFile);
	}	
	
	public static void mergeAndRepresentative(String mapFile, String cluFile, double lambda, Double[] eps, boolean removeSingle, boolean removeRepresentatives, String outputFile) throws IOException, URISyntaxException, SyntaxException, InterruptedException, XMLStreamException {
		File f = new File(mapFile);
		FileInputStream in = new FileInputStream(f);

		TermFactory factory = new SimpleTermFactory(TermFactory.Mode.Lax, new ExceptionHandlerImpl());
		NxParser nxp = new NxParser(factory, in, Charsets.UTF_8);
		List<Terms> paths = new ArrayList<>();
		
		nxp.parse(new ConsumerCollection<Terms>(paths));
		
		for (double epsilon : eps) {
			
			HashMap<Integer, Cluster> clusters = getClusters(paths, cluFile, lambda);
			
			Set<Integer> keyset = clusters.keySet();
			
			int [] arry = new int [keyset.size()];
			int index = 0;
			for (Integer i : keyset) {
				arry [index] = i;
				index++;
			}
			
			// MERGE		
			ArrayList<HashSet<Integer>> mergers = new ArrayList<>();
			for (int i = 0; i < arry.length; i++) {
				for (int j = i + 1; j < arry.length; j++) {
					Integer clusterId1 = arry[i];
					Integer clusterId2 = arry[j];
					Cluster c1 = clusters.get(clusterId1);
					Cluster c2 = clusters.get(clusterId2);
					
					double overlap = clusterOverlap(c1, c2);
					if (overlap >= epsilon) {
						// find out if one of the two is already contained in a group
						HashSet<Integer> part1 = getGroup(clusterId1, mergers);
						HashSet<Integer> part2 = getGroup(clusterId2, mergers);
						
						// do what needs to be done
						if (part1 == null && part2 == null) {
							HashSet<Integer> set = new HashSet<>();
							set.add(clusterId1);
							set.add(clusterId2);
							mergers.add(set);
						} else if ((part1 == null && part2 != null) || (part2 == null && part1 != null)) {
							if (part1 == null) {
								part2.add(clusterId1);
							} else if (part2 == null) {
								part1.add(clusterId2);
							}
						} else if (part1 != null && part2 != null && !part1.equals(part2)) {
							part1.addAll(part2);
							mergers.remove(part2);
						}
					}
				}
			}
			
			for (HashSet<Integer> hashSet : mergers) {
				Cluster newC = new Cluster(-1, lambda);
				newC.setContents(new LinkedList<Terms>());
				for (Integer integer : hashSet) {
					Cluster k = clusters.remove(integer);
					newC.merge(k);
				}
				int id = newC.getId();
				clusters.put(id, newC);
			}
			
			int featurecount = 0;
			for (Integer integer : keyset) {
				Cluster k = clusters.get(integer);
				featurecount = featurecount + k.getSize();
			}
			
			System.out.print("Writing " + outputFile+ "_" + epsilon + ".xml ...");
			
			XMLStreamWriter ch = _factory.createXMLStreamWriter(new FileOutputStream(new File(outputFile+ "_" + epsilon + ".xml")), "utf-8");

			ch.writeStartDocument("UTF-8", "1.0");
			ch.writeStartElement("clusters");
			ch.writeNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
			
			
			for (Integer integer : keyset) {
				Cluster k = clusters.get(integer);
				if (removeSingle) {
					if (k.getSize() > 1) {
						k.writeToXML(removeRepresentatives, ch);						
					}
				} else {
					k.writeToXML(removeRepresentatives, ch);
				}
			}

			// clusters
			ch.writeEndElement();
			ch.writeEndDocument();
			ch.close();
			
			System.out.println("done");
		}
	}
	
	public static HashSet<Integer> getGroup(Integer i, ArrayList<HashSet<Integer>> all) {
		for (HashSet<Integer> list : all) {
			if (list.contains(i)) {
				return list;
			}
		}
		return null;
	}
	
	private static HashMap<Integer, Cluster> getClusters(List<Terms> paths, String cluFile, double lambda) throws IOException, URISyntaxException {

		HashMap<Integer, Cluster> clusters = new HashMap<>();
	
		/*
		 * Read in cluster file (the line number is the entry from map).
		 */
		BufferedReader readerClusters = new BufferedReader(new FileReader(new File(cluFile)));
		HashMap<Integer, LinkedList<Integer>> clusterParts = new HashMap<>();
		String current = readerClusters.readLine();
		int counter = 0;
		while (current != null) {
			int clusterNo = Integer.parseInt(current);
			LinkedList<Integer> parts = clusterParts.get(clusterNo);
			if (parts == null) {
				parts = new LinkedList<>();
				clusterParts.put(clusterNo, parts);
			}
			parts.add(counter);
			
			counter++;
			current = readerClusters.readLine();
		}
		readerClusters.close();
		
		for (Integer id : clusterParts.keySet()) {
			Cluster c = new Cluster(id, lambda);
			LinkedList<Integer> integers = clusterParts.get(id);
			LinkedList<Terms> terms = new LinkedList<>();
			for (Integer i : integers) {
				Terms term = paths.get(i);
				if (term != null) {
					terms.add(term);	
				}
			}
			c.setContents(terms);
			clusters.put(id, c);
		}
		
		return clusters;
	}

	public static double clusterOverlap(Cluster c1, Cluster c2) {
		LinkedList<Terms> ftsList1 = c1.getContents();
		LinkedList<Terms> ftsList2 = c2.getContents();

		HashSet<Terms> fts1 = new HashSet<>();
		for (Terms ft : ftsList1) {
			PathFeature p = new PathFeature(ft);
			Terms s = new TermsImpl(p.getFirstTriple());
			if (p.isExtended()) {
				fts1.add(s);
			}
		}
		
		HashSet<Terms> fts2 = new HashSet<>();
		for (Terms ft : ftsList2) {
			PathFeature p = new PathFeature (ft);
			Terms s = new TermsImpl(p.getFirstTriple());
			fts2.add(s);
		}
		HashSet<Terms> fts3 = new HashSet<>();
		fts3.addAll(fts2);
		
		int overlap = 0;
		for (Terms st : fts1) {
			if (fts2.contains(st)) {
				overlap++;
			}
			fts3.add(st);
		}
		double union = fts3.size();
		return ((double) overlap) / union;
	}
}
