package edu.kit.aifb.fuse.featureextraction;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.kit.aifb.fuse.features.PathFeature;
import edu.kit.aifb.ldfu.api.rdf.Quad;
import edu.kit.aifb.ldfu.api.rdf.Term;
import edu.kit.aifb.ldfu.api.rdf.Terms;
import edu.kit.aifb.ldfu.api.rdf.Triple;
import edu.kit.aifb.ldfu.api.rdf.factory.TermFactory;
import edu.kit.aifb.ldfu.api.rdf.term.IRI;
import edu.kit.aifb.ldfu.collection.MultiMap;

/**
 * Implementing similarity for matrix based on logical mappings.
 * 
 * Return 0: if path feature p0 can be mapped to path feature p1.
 * Return 1: if path feature p0 cannot be mapped to path feature p1.
 * 
 * Disregard string labels when computing similarity, operate only on path feature level.
 * 
 * @author aharth
 */
public class LogicalSimilarity {
	static final Logger _log = Logger.getLogger(LogicalSimilarity.class.getName());

	private final TermFactory _factory;

	private final MultiMap<IRI, IRI> _classes;
	private final MultiMap<IRI, IRI> _properties;
	private final MultiMap<IRI, IRI> _sameas;

	public LogicalSimilarity(TermFactory factory, MultiMap<IRI, IRI> classes, MultiMap<IRI, IRI> properties, MultiMap<IRI, IRI> sameas) {
		_factory = factory;

		_classes = classes;
		_properties = properties;
		_sameas = sameas;
	}

	/**
	 * Compute distance/similarity (in the case of LogicalSimilarty, check equivalence).
	 * 
	 * 0: different
	 * 1: same
	 * 
	 * We assume the first element of all path features are equivalent (focus entities).
	 */
	public double distance(PathFeature p0, PathFeature p1) {
		// different size? then 1
		if (p0.isExtended() != p1.isExtended()) {
			return 1;
		}

		// equal? then 0
		if (p0.equals(p1)) {
			return 0;
		}

		// first element is the same anyways, so throw out
		// also throw out the source and the string representations of the properties
		Terms pt0, pt1;

		Quad p0q = p0.getQuad();
		Quad p1q = p1.getQuad();

		if (p0.isExtended()) {
			Triple p0t = p0.getTriple();
			Triple p1t = p1.getTriple();

			pt0 = _factory.createTerms(p0q.getPredicate(), p0q.getObject(), p0t.getPredicate(), p0t.getObject());
			pt1 = _factory.createTerms(p1q.getPredicate(), p1q.getObject(), p1t.getPredicate(), p1t.getObject());
		} else {
			pt0 = _factory.createTerms(p0q.getPredicate(), p0q.getObject());
			pt1 = _factory.createTerms(p1q.getPredicate(), p1q.getObject());
		}

		_log.log(Level.FINE, "Computing similarity between {0} and {1}", new Object[] { pt0, pt1 } );

		// same path feature (modulo first element and labels)?
		if (pt0.equals(pt1)) {
			return 0;
		}

		// can we translate pt0 to pt1?
		Set<Terms> spt0 = translateFirstProperty(pt0);
		for (Terms t : spt0) {
			if (pt1.equals(t)) {
				return 0;
			}

			// path feature length 2
			if (p0.isExtended()) {
				Set<Terms> sptt0 = translateFirstObject(t);
				for (Terms t1 : sptt0) {
					_log.log(Level.FINE, "Comparing {0} to {1}", new Object[] { pt1, t1 } );

					if (pt1.equals(t1)) {
						return 0;
					} else {
						Set<Terms> spttt0 = translateSecondProperty(t1);
						for (Terms t2 : spttt0) {
							_log.log(Level.FINE, "Comparing {0} to {1}", new Object[] { pt1, t2 } );

							if (pt1.equals(t2)) {
								return 0;
							}
						}
					}
				}
			}
		}

		// can we translate pt1 to pt0?
		Set<Terms> spt1 = translateFirstProperty(pt1);
		for (Terms t : spt1) {
			if (pt0.equals(t)) {
				return 0;
			}

			// path feature length 2
			if (p0.isExtended()) {
				Set<Terms> sptt0 = translateFirstObject(t);
				for (Terms t1 : sptt0) {
					if (pt0.equals(t1)) {
						return 0;
					} else {
						Set<Terms> spttt0 = translateSecondProperty(t1);
						for (Terms t2 : spttt0) {
							if (pt0.equals(t2)) {
								return 0;
							}
						}
					}
				}
			}
		}

		// not possible to map: 1
		return 1;
	}

	/**
	 * Translate the first property of a path feature.
	 * 
	 * Path feature starts with the property URI as first element.
	 */
	private Set<Terms> translateFirstProperty(Terms p) {
		Set<Terms> result = new HashSet<Terms>();

		// get translations of first property
		IRI prop = (IRI)p.get(0);
		Iterator<IRI> it = _properties.get(prop);

		if (it != null) {
			while (it.hasNext()) {
				IRI prop2 = it.next();

				// substitute pt0 with equivalent URI for property
				Terms ptt0 = replace(p, prop2, 0);

				result.add(ptt0);
			}
		}

		return result;
	}

	/**
	 * Translate the first object of a path feature.
	 * 
	 * Path feature starts with the property URI as first element.
	 */
	private Set<Terms> translateFirstObject(Terms p) {
		Set<Terms> result = new HashSet<Terms>();

		// get translations of object
		IRI obj = (IRI)p.get(1);
		Iterator<IRI> it = _sameas.get(obj);

		if (it != null) {
			while (it.hasNext()) {
				IRI obj2 = it.next();

				// substitute pt0 with equivalent URI for property
				Terms ptt0 = replace(p, obj2, 1);

				result.add(ptt0);
			}
		}

		return result;
	}

	/**
	 * Translate the second property of a path feature.
	 * 
	 * Path feature starts with the property URI as first element.
	 */
	private Set<Terms> translateSecondProperty(Terms p) {
		Set<Terms> result = new HashSet<Terms>();

		// get translations of second property
		IRI prop = (IRI)p.get(2);
		Iterator<IRI> it = _properties.get(prop);

		if (it != null) {
			while (it.hasNext()) {
				IRI prop2 = it.next();

				// substitute pt0 with equivalent URI for property
				Terms ptt0 = replace(p, prop2, 2);

				result.add(ptt0);
			}
		}

		return result;
	}

	/**
	 * Replace a single term within terms.
	 */
	private Terms replace(Terms in, Term t, int index) {
		Term[] terms = new Term[in.size()];

		for (int i = 0; i < in.size(); i++) {
			if (i == index) {
				terms[i] = t;
			} else {
				terms[i] = in.get(i);
			}
		}

		return _factory.createTerms(terms);
	}
}