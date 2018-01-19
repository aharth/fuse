package edu.kit.aifb.fuse.similarity;

import java.util.Set;

import edu.kit.aifb.fuse.features.PathFeature;

/**
 * 
 * @author andreas & Steffen
 */
public class OntologyAlignmentRec implements SimilarityMeasure {

	// default lambda = 0.5
	private double lambda = 0.5;
	
	public OntologyAlignmentRec(double newLambda) {
		this.lambda = newLambda;
	}
	/**
	 * @return a double value between 0 and 1, with 1 meaning identity
	 */
	@Override
	public double similarity(PathFeature t1, PathFeature t2) {
		
		return (lambda * propertySimilarity(t1, t2)) + ((1 - lambda) * valueSimilarity(t1, t2));
	}

	private double propertySimilarity(PathFeature t1, PathFeature t2) {
		// get first predicate label
		String val1 = t1.getFirstPredicateString();
		String val2 = t2.getFirstPredicateString();
		
		Set<String> tokens1 = SimilarityUtils.normalize(SimilarityUtils.tokenize(val1));
		Set<String> tokens2 = SimilarityUtils.normalize(SimilarityUtils.tokenize(val2));
		
		if (tokens1.size() == 1 && tokens2.size() == 1) {
			// both are unsplittable strings
			String s1 = (String) tokens1.toArray() [0];
			String s2 = (String) tokens2.toArray() [0];
			return SimilarityUtils.jaroWinkler(s1, s2);
			
		} else if ((tokens1.size() > 1 && tokens2.size() > 1)
				|| (tokens1.size() == 1 && tokens2.size() > 1)
				|| (tokens1.size() > 1 && tokens2.size() == 1)) {
			// at least one of the two sets is bigger than 1 
			return SimilarityUtils.jaccard(tokens1, tokens2);
		} else {
			// one of the two has size 0
			return 0;
		}
	}
	
	private double valueSimilarity(PathFeature t1, PathFeature t2) {
		// get last object label
		String val1 = t1.getLastObjectString();
		String val2 = t2.getLastObjectString();
		
		Set<String> tokens1 = SimilarityUtils.normalize(SimilarityUtils.tokenize(val1));
		Set<String> tokens2 = SimilarityUtils.normalize(SimilarityUtils.tokenize(val2));
		
		if (tokens1.size() == 1 && tokens2.size() == 1) {
			// both are unsplittable strings
			String s1 = (String) tokens1.toArray() [0];
			String s2 = (String) tokens2.toArray() [0];
			return SimilarityUtils.jaroWinkler(s1, s2);
			
		} else if ((tokens1.size() > 1 && tokens2.size() > 1)
				|| (tokens1.size() == 1 && tokens2.size() > 1)
				|| (tokens1.size() > 1 && tokens2.size() == 1)) {
			// at least one of the two sets is bigger than 1 
			return SimilarityUtils.jaccard(tokens1, tokens2);
		} else {
			// one of the two has size 0
			return 0;
		}
	}
}
