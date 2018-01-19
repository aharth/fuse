package edu.kit.aifb.fuse.featureextraction;

/**
 * Counting object creation.
 * 
 * @author aharth
 */
public class TermFactoryStats {
	/** Number of IRIs */
	private long _iris = 0;

	/** Number of Literals */
	private long _literals = 0;

	/** Number of blank nodes */
	private long _bnodes = 0;

	/** Number of terms */
	private long _terms = 0;

	/** Number of triples */
	private long _triples = 0;

	/** Number of generalised triples */
	private long _gtriples = 0;

	/**
	 */
	public void incrementIRIs() {
		_iris++;
	}

	/**
	 */
	public void incrementBNodes() {
		_bnodes++;
	}

	/**
	 */
	public void incrementLiterals() {
		_literals++;
	}

	/**
	 */
	public void incrementTerms() {
		_terms++;
	}

	/**
	 */
	public void incrementTriples() {
		_triples++;
	}

	/**
	 */
	public void incrementGeneralisedTriples() {
		_gtriples++;
	}

	/**
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("IRIs: ");
		sb.append(_iris);
		sb.append(", literals: ");
		sb.append(_literals);
		sb.append(", blank nodes: ");
		sb.append(_bnodes);
		sb.append(", terms: ");
		sb.append(_terms);
		sb.append(", triples: ");
		sb.append(_triples);
		sb.append(", generalise triples: ");
		sb.append(_gtriples);

		return sb.toString();
	}
}