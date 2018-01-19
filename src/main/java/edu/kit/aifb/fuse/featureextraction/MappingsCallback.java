package edu.kit.aifb.fuse.featureextraction;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.kit.aifb.ldfu.api.eval.Consumer;
import edu.kit.aifb.ldfu.api.rdf.Triple;
import edu.kit.aifb.ldfu.api.rdf.term.IRI;
import edu.kit.aifb.ldfu.api.rdf.term.impl.IRIs;
import edu.kit.aifb.ldfu.collection.MultiMap;

/**
 * Read a Turtle file with mapping triples into Java Map objects.
 * The characteristics of equality (symmetry, transitivity...) in the mapping triples have to be materialised in the Turtle file.
 * 
 * @author aharth
 */
public class MappingsCallback implements Consumer<Triple> {
	static final Logger _log = Logger.getLogger(MappingsCallback.class.getName());

	private final MultiMap<IRI, IRI> _classes;
	private final MultiMap<IRI, IRI> _properties;
	private final MultiMap<IRI, IRI> _sameas;

	/**
	 */
	public MappingsCallback(MultiMap<IRI, IRI> classes, MultiMap<IRI, IRI> properties, MultiMap<IRI, IRI> sameas) {
		_classes = classes;
		_properties = properties;
		_sameas = sameas;
	}

	/**
	 */
	@Override
	public void consume(Triple item) throws InterruptedException, IOException {
		// only one direction
		if (IRIs.RDFS_SUBCLASSOF.equals(item.getPredicate())) {
			if (item.getSubject() instanceof IRI && item.getObject() instanceof IRI) {
				_classes.put((IRI)item.getSubject(), (IRI)item.getObject());
			} else {
				_log.log(Level.INFO, "Cannot use triple {0}", item);
			}
		}

		if (IRIs.RDFS_SUBPROPERTYOF.equals(item.getPredicate())) {
			if (item.getSubject() instanceof IRI && item.getObject() instanceof IRI) {
				_properties.put((IRI)item.getSubject(), (IRI)item.getObject());
			} else {
				_log.log(Level.INFO, "Cannot use triple {0}", item);
			}
		}

		// transitivity and symmetry materialised
		if (IRIs.OWL_EQUIVALENTCLASS.equals(item.getPredicate())) {
			if (item.getSubject() instanceof IRI && item.getObject() instanceof IRI) {
				_classes.put((IRI)item.getSubject(), (IRI)item.getObject());
			} else {
				_log.log(Level.INFO, "Cannot use triple {0}", item);
			}
		}

		if (IRIs.OWL_EQUIVALENTPROPERTY.equals(item.getPredicate())) {
			if (item.getSubject() instanceof IRI && item.getObject() instanceof IRI) {
				_properties.put((IRI)item.getSubject(), (IRI)item.getObject());
			} else {
				_log.log(Level.INFO, "Cannot use triple {0}", item);
			}
		}

		if (IRIs.OWL_SAMEAS.equals(item.getPredicate())) {
			if (item.getSubject() instanceof IRI && item.getObject() instanceof IRI) {
				_sameas.put((IRI)item.getSubject(), (IRI)item.getObject());
			} else {
				_log.log(Level.INFO, "Cannot use triple {0}", item);
			}
		}
	}
}