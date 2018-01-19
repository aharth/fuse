package edu.kit.aifb.fuse.similarity;

import edu.kit.aifb.fuse.features.PathFeature;

/**
 * 
 * @author andreas
 */
public interface SimilarityMeasure {

	/**
	 * @return a double value between 0 and 1, with 0 meaning identity
	 */
	public double similarity(PathFeature t1, PathFeature t2);
}
