package edu.kit.aifb.fuse.features;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import edu.kit.aifb.ldfu.api.rdf.Quad;
import edu.kit.aifb.ldfu.api.rdf.Term;
import edu.kit.aifb.ldfu.api.rdf.Terms;
import edu.kit.aifb.ldfu.api.rdf.Triple;
import edu.kit.aifb.ldfu.api.rdf.impl.QuadImpl;
import edu.kit.aifb.ldfu.api.rdf.impl.TripleImpl;
import edu.kit.aifb.ldfu.api.rdf.term.BlankNodeOrIRI;
import edu.kit.aifb.ldfu.api.rdf.term.IRI;

/**
 * 
 * @author Steffen
 * @author aharth
 */
public class PathFeature {
	
	private Terms _path;
	
	public PathFeature(Terms t1) {
		_path = t1;
	}

	public boolean isExtended() {
		return _path.size() > 5;
	}
	
	public String getFirstPredicateString() {
		if (_path.size() == 5 || _path.size() == 4) {
			return _path.get(3).toString();
		} else if (_path.size() == 8 || _path.size() == 7) {
			return _path.get(5).toString();
		}
		return null;
	}
	
	public String getLastObjectString() {
		if (_path.size() == 5 || _path.size() == 4) {
			return _path.get(2).toString();
		} else if (_path.size() == 8 || _path.size() == 7) {
			return _path.get(4).toString();
		}
		return null;
	}

	public String getSourceString() {
		if (_path.size() == 5) {
			return _path.get(4).toString();
		} else if (_path.size() == 8) {
			return _path.get(7).toString();
		}
		return "NOSOURCE";
	}

	public String getHostString() {
		String host = getSourceString();
		
		if (host.startsWith("https://")) {
			host = "https://" + host.split("/")[2];
		} else if (host.startsWith("http://")) {
			host = "http://" + host.split("/")[2];
		}
		return host;
	}
	
	public Term[] getFirstTriple() {
		Term[] triple = new Term[3];

		triple[0] = _path.get(0);
		triple[1] = _path.get(1);
		triple[2] = _path.get(2);
		
		return triple;
	}
	
	/**
	 * Return quad (possible for any path feature).
	 * 
	 * @@@ should use factory to create Quad
	 */
	public Quad getQuad() {
		Quad q = null;
		
		if (isExtended()) {
			q = new QuadImpl((BlankNodeOrIRI)_path.get(0), (IRI)_path.get(1), _path.get(2), (IRI)_path.get(7));
		} else {
			q = new QuadImpl((BlankNodeOrIRI)_path.get(0), (IRI)_path.get(1), _path.get(2), (IRI)_path.get(4));			
		}

		return q;
	}

	/**
	 * Only for extended path features.
	 * 
	 * @@@ should use factory to create Triple
	 */
	public Triple getTriple() {
		if (isExtended()) {
			return new TripleImpl((BlankNodeOrIRI)_path.get(2), (IRI)_path.get(3), _path.get(4));
		}

		throw new IllegalArgumentException("Call getTriple() only for extended path features.");
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof PathFeature) {
			return ((PathFeature)o)._path.equals(_path);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return _path.hashCode();
	}


	public void toXMLString(XMLStreamWriter ch) throws XMLStreamException {
		ch.writeStartElement("triple");
		
		ch.writeStartElement("s");
		ch.writeAttribute("iri", _path.get(0).toString());
		ch.writeEndElement();

		ch.writeStartElement("p");
		ch.writeAttribute("iri", _path.get(1).toString());
		ch.writeEndElement();

		ch.writeStartElement("o");
		if (_path.get(2).toString().startsWith("<") || _path.get(2).toString().startsWith("http:")) {
			ch.writeAttribute("iri", _path.get(2).toString());
		} else {
			ch.writeAttribute("literal", _path.get(2).toString());
		}
		ch.writeEndElement();
		
		ch.writeEndElement();
		
		if (this.isExtended()) {
			ch.writeStartElement("triple");
			
			ch.writeStartElement("s");
			ch.writeAttribute("iri", _path.get(2).toString());
			ch.writeEndElement();

			ch.writeStartElement("p");
			ch.writeAttribute("iri", _path.get(3).toString());
			ch.writeEndElement();

			ch.writeStartElement("o");
			if (_path.get(2).toString().startsWith("<") || _path.get(2).toString().startsWith("http:")) {
				ch.writeAttribute("iri", _path.get(4).toString());
			} else {
				ch.writeAttribute("literal", _path.get(4).toString());
			}
			ch.writeEndElement();
			
			ch.writeEndElement();
		}
		
	}
	
	public String toString(){
		String str = "";
		for (Term t : _path) {
			str += t + "; ";
		}
		return str;
	}
}