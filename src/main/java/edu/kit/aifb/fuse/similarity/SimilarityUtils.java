package edu.kit.aifb.fuse.similarity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author andreas
 */
public class SimilarityUtils {
	
	public static long lcsTime = 0;	
	
	public static double lcsMeasure(String val1, String val2) {
		if (val1 == null || val2 == null) {
			System.out.println(val1 + "\t" + val2);
		}
		double longestSubstring = longestSubstr(val1, val2);
		return (2.0 * longestSubstring) / ((double) (val1.length() + val2.length()));
	}

	public static double jaccard(Set<String> set1, Set<String> set2) {
		
		// measure overlap of two sets
		int overlapCounter = 0;
		for (String string : set2) {
			if (set1.contains(string)) {
				overlapCounter++;
			}
		}
		
		set1.addAll(set2);
		double union = set1.size();
		return ((double) overlapCounter) / union;
	}
	
	public static HashSet<String> tokenize(String string) {
		HashSet<String> tokens1 = new HashSet<>();
		// split by any sequence of whitespace
		tokens1.addAll(Arrays.asList(string.split("[_|.|-|\\s+]")));
			
		HashSet<String> tokens = new HashSet<>();
		// split by camel case
		for (String string2 : tokens1) {
			tokens.addAll(Arrays.asList(string2.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")));
		}
		return tokens;
	}
	
	public static Set<String> normalize(Set<String> tokens) {
		HashSet<String> tokens2 = new HashSet<>();
		for (String string2 : tokens) {
			
			// remove any remaining punctuation
			string2 = string2.replaceAll("\\p{P}", "").toLowerCase().trim();
			if (!string2.isEmpty()) tokens2.add(string2);
		}
		return tokens2;
	}
	
	
	// taken from https://code.google.com/p/duke/source/browse/src/main/java/no/priv/garshol/duke/JaroWinkler.java?r=e32a5712dbd51f1d4c81e84cfa438468e217a65d
	/**
	 * Returns normalized score, with 0.0 meaning no similarity at all, and 1.0
	 * meaning full equality.
	 */
	public static double jaroWinkler(String s1, String s2) {
		if (s1.equals(s2))
			return 1.0;

		// ensure that s1 is shorter than or same length as s2
		if (s1.length() > s2.length()) {
			String tmp = s2;
			s2 = s1;
			s1 = tmp;
		}

		// (1) find the number of characters the two strings have in common.
		// note that matching characters can only be half the length of the
		// longer string apart.
		int maxdist = s2.length() / 2;
		int c = 0; // count of common characters
		int t = 0; // count of transpositions
		int prevpos = -1;
		for (int ix = 0; ix < s1.length(); ix++) {
			char ch = s1.charAt(ix);

			// now try to find it in s2
			for (int ix2 = Math.max(0, ix - maxdist); ix2 < Math.min(s2.length(), ix + maxdist); ix2++) {
				if (ch == s2.charAt(ix2)) {
					c++; // we found a common character
					if (prevpos != -1 && ix2 < prevpos)
						t++; // moved back before earlier
					prevpos = ix2;
					break;
				}
			}
		}

		// we don't divide t by 2 because as far as we can tell, the above
		// code counts transpositions directly.

		// we might have to give up right here
		if (c == 0)
			return 0.0;

		// first compute the score
		double score = ((c / (double) s1.length()) + (c / (double) s2.length()) + ((c - t) / (double) c)) / 3.0;

		// (2) common prefix modification
		int p = 0; // length of prefix
		int last = Math.min(4, s1.length());
		for (; p < last && s1.charAt(p) == s2.charAt(p); p++)
			;

		score = score + ((p * (1 - score)) / 10);

		// (3) longer string adjustment
		// I'm confused about this part. Winkler's original source code includes
		// it, and Yancey's 2005 paper describes it. However, Winkler's list of
		// test cases in his 2006 paper does not include this modification. So
		// is this part of Jaro-Winkler, or is it not? Hard to say.
		//
		// if (s1.length() >= 5 && // both strings at least 5 characters long
		// c - p >= 2 && // at least two common characters besides prefix
		// c - p >= ((s1.length() - p) / 2)) // fairly rich in common chars
		// {
		// System.out.println("ADJUSTED!");
		// score = score + ((1 - score) * ((c - (p + 1)) /
		// ((double) ((s1.length() + s2.length())
		// - (2 * (p - 1))))));
		// }

		// (4) similar characters adjustment
		// the same holds for this as for (3) above.

		return score;
	}


	private static int longestSubstr(String first, String second) {
		long seconds = System.currentTimeMillis();
	    if (first == null || second == null || first.length() == 0 || second.length() == 0) {
	        return 0;
	    }
	 
	    int maxLen = 0;
	    int fl = first.length();
	    int sl = second.length();
	    int[][] table = new int[fl+1][sl+1];
	 
	    for(int s=0; s <= sl; s++)
	      table[0][s] = 0;
	    for(int f=0; f <= fl; f++)
	      table[f][0] = 0;
	 
	    for (int i = 1; i <= fl; i++) {
	        for (int j = 1; j <= sl; j++) {
	            if (first.charAt(i-1) == second.charAt(j-1)) {
	                if (i == 1 || j == 1) {
	                    table[i][j] = 1;
	                }
	                else {
	                    table[i][j] = table[i - 1][j - 1] + 1;
	                }
	                if (table[i][j] > maxLen) {
	                    maxLen = table[i][j];
	                }
	            }
	        }
	    }
	    lcsTime += System.currentTimeMillis() - seconds;
	    return maxLen;
	}
}