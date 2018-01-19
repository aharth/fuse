package edu.kit.aifb.fuse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.kit.aifb.fuse.evaluation.Tuple;
import edu.kit.aifb.ldfu.format.parse.nx.ParseException;

/**
 * 
 * @author Steffen
 */
public class Evaluator {
	
	static final Logger _log;
	static XMLOutputFactory _factory;
	static {
		_factory = XMLOutputFactory.newInstance();
		_factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);	
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$s] %5$s %n");
		_log = Logger.getLogger(MatrixGenerationLogical.class.getName());
	}
	
	public static void main(String[] args) throws IOException, URISyntaxException, ParseException, InterruptedException, SAXException, ParserConfigurationException {
		
		Option o1 = Option.builder("o1")
				.desc("Output file for purity")
				.argName("file")
				.hasArg(true)
				.required()
				.build();
		
		Option o2 = Option.builder("o2")
				.desc("Output file for NMI")
				.argName("file")
				.hasArg(true)
				.required()
				.build();

		Option i = Option.builder("i")
				.desc("Input file for silver clusters")
				.argName("file")
				.hasArg(true)
				.required()
				.build();
		
		Option d = Option.builder("d")
				.desc("Input directory of the clusters + entity name.")
				.argName("directory")
				.hasArg(true)
				.required()
				.build();
		
		Option lambda = Option.builder("l")
				.desc("Lambda values for weigthing head and tail similarity.")
				.argName("values")
				.hasArgs()
				.required()
				.build();
		
		Option meth = Option.builder("n")
				.desc("Method (e.g. hclust_height)")
				.argName("name")
				.hasArgs()
				.required()
				.build();
		
		Option metr = Option.builder("m")
				.desc("Metric (e.g. single, complete, average)")
				.argName("values")
				.hasArgs()
				.required()
				.build();
		
		Option value = Option.builder("c")
				.desc("cluster cut-off value / count")
				.argName("values")
				.hasArgs()
				.required()
				.build();
		
		Option epsilons = Option.builder("e")
				.desc("Epsilon values for merging.")
				.argName("values")
				.hasArgs()
				.required()
				.build();
		
		Option helpO = Option.builder("h")
				.longOpt("help")
				.desc("Print help")
				.hasArg(false)
				.build();

		Options options = new Options();

		options.addOption(o1);
		options.addOption(o2);
		options.addOption(i);
		options.addOption(d);
		options.addOption(meth);
		options.addOption(metr);
		options.addOption(value);		
		options.addOption(lambda);
		options.addOption(epsilons);
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
				formatter.printHelp(pw, 80, "Evaluator", header, options, 2, 2, null);
				pw.flush();
				System.exit(0);
			}
		} catch (MissingOptionException e) {
			HelpFormatter formatter = new HelpFormatter();
			String header = null;
			PrintWriter pw = new PrintWriter(System.out);
			formatter.printHelp(pw, 80, "Evaluator", header, options, 2, 2, null);
			pw.flush();
			System.out.println(e);
			System.exit(10);
		} catch (org.apache.commons.cli.ParseException e) {
			_log.log(Level.SEVERE, "{0}: {1}", new Object[] { e.getClass().getSimpleName(), e.getMessage() });
			_log.log(Level.INFO, "Try '-h' for more information.");
			System.out.println(e);
			System.exit(10);
		}
			
		
		FileWriter fwPurity = new FileWriter(new File(cmd.getOptionValue("o1")));
		FileWriter fwNMI = new FileWriter(new File(cmd.getOptionValue("o2")));
		
		File silver = new File(cmd.getOptionValue("i"));
		
		
		String[] lam = cmd.getOptionValues("l");
		String[] method = cmd.getOptionValues("n");
		String[] count = cmd.getOptionValues("c");
		String[] metric = cmd.getOptionValues("m");
		String[] epsilon = cmd.getOptionValues("e");
		String directory = cmd.getOptionValue("d");
		
		fwPurity.write(";");
		fwNMI.write(";");
		for (String l : lam) {
			for (String e : epsilon) {
				fwPurity.write("lambda=" + l + " epsilon=" + e + ";");
				fwNMI.write("lambda=" + l + " epsilon=" + e + ";");
			}
		}	
		fwPurity.write("\n");
		fwNMI.write("\n");
		
		for (String m : method) {
			for (String c : count) {
				for (String me : metric) {
					fwPurity.write(m + "_" + c + "_" + me + ";");
					fwNMI.write(m + "_" + c + "_" + me + ";");
					for (String l : lam) {
						for (String e : epsilon) {
							String purity = "-";
							String NMI = "-";
							File file = new File(directory +"_" + l + "_" + m + "_" + c + "_" + me + "_" + e + ".xml");
							if (file.exists()) {
								Object[] purityAndNMI = purityAndNMI(file, silver);
								if (purityAndNMI != null) {
									purity = "" + purityAndNMI[0];
									NMI = "" + purityAndNMI[1];
								}
							}
							fwPurity.write(purity + ";");
							fwNMI.write(NMI + ";");
						}
					}
					fwPurity.write("\n");
					fwNMI.write("\n");
				}
			}
		}
		fwPurity.close();
		fwNMI.close();
	}	


	public static Object[] purityAndNMI(File outputCluster, File silverCluster) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document output = db.parse(new FileInputStream(outputCluster));
		Document silver = db.parse(new FileInputStream(silverCluster));
		
		
		HashMap<String, Set<Integer>> silverClusterMap = new HashMap<String, Set<Integer>>();
		HashMap<String, Set<Integer>> outputClusterMap = new HashMap<String, Set<Integer>>();
		
		HashSet<Tuple<String, Integer>> outputTuples = new HashSet<Tuple<String, Integer>>();
		NodeList outClusters = output.getElementsByTagName("cluster");
		for (int i = 0; i < outClusters.getLength(); i++) {
			Element cluster = (Element) outClusters.item(i);
			NodeList cluster_path_features = cluster.getElementsByTagName("path-feature");
			for (int j = 0; j < cluster_path_features.getLength(); j++) {
				Element path = (Element) cluster_path_features.item(j);
				Element triple = (Element) path.getElementsByTagName("triple").item(0);

				Element s = (Element) triple.getElementsByTagName("s").item(0);
				String s_iri = s.getAttribute("iri");

				Element p = (Element) triple.getElementsByTagName("p").item(0);
				String p_iri = p.getAttribute("iri");

				Element o = (Element) triple.getElementsByTagName("o").item(0);
				String o_iri = o.getAttribute("iri");

				if (o_iri.equals("")) {
					o_iri = o.getAttribute("literal");
				}

				outputTuples.add(new Tuple<String, Integer>(s_iri + p_iri + o_iri, i));
			}	
		}
			
		int silverCount = 0;
		NodeList silverClusters = silver.getElementsByTagName("cluster");
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int i = 0; i < silverClusters.getLength(); i++) {
			HashSet<Tuple<String, Integer>> silverTuples = new HashSet<Tuple<String, Integer>>();
			
			Element cluster = (Element) silverClusters.item(i);
			
			NodeList cluster_path_features = cluster.getElementsByTagName("path-feature");
			
			for (int j = 0; j < cluster_path_features.getLength(); j++) {
				Element path = (Element) cluster_path_features.item(j);
				Element triple = (Element) path.getElementsByTagName("triple").item(0);

				Element s = (Element) triple.getElementsByTagName("s").item(0);
				String s_iri = s.getAttribute("iri");

				Element p = (Element) triple.getElementsByTagName("p").item(0);
				String p_iri = p.getAttribute("iri");

				Element o = (Element) triple.getElementsByTagName("o").item(0);
				String o_iri = o.getAttribute("iri");

				if (o_iri.equals("")) {
					o_iri = o.getAttribute("literal");
				}

				silverTuples.add(new Tuple<String, Integer>(s_iri + p_iri + o_iri, i));
			}
			if (silverTuples.size() > 1) {
				ids.add(i);
				
				for (Tuple<String, Integer> s : silverTuples) {
					String silverString = s.getFirst();
					int silverID = s.getSecond();
					for (Tuple<String, Integer> o : outputTuples) {
						String outputString = o.getFirst();
						if (silverString.equals(outputString)) {
							
							int outputID = o.getSecond();

							if (outputClusterMap.containsKey("" + outputID)) {
								outputClusterMap.get("" + outputID).add(silverCount);
							} else {
								outputClusterMap.put("" + outputID, new HashSet<Integer>());
								outputClusterMap.get("" + outputID).add(silverCount);
							}
							
							if (silverClusterMap.containsKey("" + silverID)) {
								silverClusterMap.get("" + silverID).add(silverCount);
							} else {
								silverClusterMap.put("" + silverID, new HashSet<Integer>());
								silverClusterMap.get("" + silverID).add(silverCount);
							}
							
							silverCount++;
						}
					}
				}
			}
		}
		if (silverCount > 0 && silverClusterMap.size() + outputClusterMap.size() > 2) {
			double purity = computePurity(silverClusterMap, outputClusterMap, silverCount);
			double NMI = computeNMIscore(silverClusterMap, outputClusterMap, silverCount);

			Object[] result = new Object[2];
			result[0] = purity;
			result[1] = NMI;

			return result;
		} else {
			return null;
		}
	}
	
	public static double computePurity(HashMap<String, Set<Integer>> silverClusters, HashMap<String, Set<Integer>> outputClusters, int silverCount) {
		int count = 0;
		for (String label : outputClusters.keySet()) {
			Set<Integer> labels = outputClusters.get(label);
			int correctlyAssigned = 0;
			for (String silverLabel : silverClusters.keySet()) {
				Set<Integer> silver = silverClusters.get(silverLabel);
				Set<Integer> output = new HashSet<Integer>(labels);
				output.retainAll(silver);
				if (output.size() >= correctlyAssigned)
					correctlyAssigned = output.size();
			}
			count += correctlyAssigned;
		}
		return count * 1.0 / silverCount;
	}

	public static double computeNMIscore(HashMap<String, Set<Integer>> silverClusters, HashMap<String, Set<Integer>> outputClusters, int silverCount) {
		double MIscore = 0.0;
		for (String label : outputClusters.keySet()) {
			Set<Integer> labels = outputClusters.get(label);
			for (String silverLabel : silverClusters.keySet()) {
				Set<Integer> silver = silverClusters.get(silverLabel);
				Set<Integer> output = new HashSet<Integer>(labels);
				output.retainAll(silver);
				double correctlyAssigned = output.size() * 1.0;
				if (correctlyAssigned == 0.0)
					continue;
				MIscore += (correctlyAssigned / silverCount) * Math.log(correctlyAssigned * silverCount / (labels.size() * silver.size()));
			}
		}
		double entropy = 0.0;
		for (String label : outputClusters.keySet()) {
			Set<Integer> docs = outputClusters.get(label);
			entropy += (-1.0 * docs.size() / silverCount) * Math.log(1.0 * docs.size() / silverCount);
		}
		for (String label : silverClusters.keySet()) {
			Set<Integer> docs = silverClusters.get(label);
			entropy += (-1.0 * docs.size() / silverCount) * Math.log(1.0 * docs.size() / silverCount);
		}		
		return 2 * MIscore / entropy;
	}
}
