package edu.kit.aifb.fuse;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import edu.kit.aifb.ldfu.plan.translate.operator.physical.SafetyException;

/**
 * Main class that can call the various steps in the processing pipeline.
 * 
 * @author aharth
 */
public class Main {
	static final Logger _log;

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$s] %5$s %n");

		_log = Logger.getLogger(FeatureExtraction.class.getName());
	}

	private static final String PREFIX = "edu.kit.aifb.fuse.";
	private static final String USAGE = "Usage: org.kit.aifb.fuse.Main <utility> [options...]";

	/**
	 */
	public static void main(String[] args) throws InterruptedException, XMLStreamException, SafetyException {
		
		try {
			if (args.length < 1) {			
				StringBuffer sb = new StringBuffer();
				sb.append("where <utility> is one of");
				sb.append("\n\tSimilarityMatrix         Computing a similarity matrix based on nx-file");
				sb.append("\n\tClusterAnalysis          Cluster Merging and Representative Selection steps");
				sb.append("\n\tEvaluator                Analyse the results of executing the pipeline");
				sb.append("\n\tFeatureExtraction        Extract path features");
				sb.append("\n\tFilterMappings           Filter out logical mappings (sameAs, equivalentProperty/Class, subPropertyOf/ClassOf)");
				sb.append("\n\tMatrixGenerationLogical  Generate similarity matrix files from path features based on logical mappings");

				_log.severe(USAGE);
				_log.severe(sb.toString());
				System.exit(-1);
			}

			Class<?> cls = Class.forName(PREFIX + args[0]);

			Method mainMethod = cls.getMethod("main", new Class[] { String[].class });

			String[] mainArgs = new String[args.length - 1];
			System.arraycopy(args, 1, mainArgs, 0, mainArgs.length);

			long time = System.currentTimeMillis();
			mainMethod.invoke(null, new Object[] { mainArgs });
			long time1 = System.currentTimeMillis();
			_log.info("Elapsed time " + (time1-time) + " ms");
		} catch (Throwable e) {
			e.printStackTrace();
			Throwable cause = e.getCause();
			cause.printStackTrace();
			_log.severe(USAGE);
			_log.severe(e.toString());
			System.exit(-1);
		}
	}
}