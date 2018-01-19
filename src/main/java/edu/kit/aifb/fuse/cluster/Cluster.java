package edu.kit.aifb.fuse.cluster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import edu.kit.aifb.fuse.features.PathFeature;
import edu.kit.aifb.fuse.similarity.OntologyAlignmentRec;
import edu.kit.aifb.fuse.similarity.SimilarityMeasure;
import edu.kit.aifb.ldfu.api.rdf.Terms;
import edu.kit.aifb.ldfu.api.rdf.impl.TermsImpl;

/**
 * 
 * @author andreas & Steffen
 */
public class Cluster {
	
	private double lambda;
	private int mergeCount = 0;
	private int id;
	private LinkedList<Terms> contents;
	
	public Cluster(int id, double lambda) {
		this.id = id;
		this.lambda = lambda;
	}

	public LinkedList<Terms> getContents() {
		return contents;
	}
	
	public PathFeature getRepresentativePerSource(String hostname) {
		// if size is 1, we return the element
		if (contents.size() == 1) {
			return new PathFeature(contents.get(0));
		}
		
		// check if extended features are present
		boolean hasExtended = false;
		for (Terms comparableFeature : contents) {
			PathFeature p = new PathFeature(comparableFeature);
			if (p.isExtended()) {
				hasExtended = true;
			}
		}
		
		// if it has extended features, use the one that is most documented.
		if (hasExtended) {
			HashMap<Terms, Integer> map = new HashMap<>();
			// count per statement
			for (Terms comparableFeature : contents) {
				PathFeature p = new PathFeature(comparableFeature);
				if (p.getHostString().trim().equals(hostname.trim())) {
					Terms first = new TermsImpl(p.getFirstTriple());
					Integer count = map.get(first);
					if (count == null) {
						map.put(first, 1);
					} else {
						map.put(first, count + 1);
					}
				}
			}
			Integer max = 0;
			Terms maxStatement = new TermsImpl();
			for (Terms s : map.keySet()) {
				Integer current = map.get(s);
				if (current > max) {
					max = current;
					maxStatement = s;
				}
			}
			for (Terms comparableFeature : contents) {
				PathFeature p = new PathFeature(comparableFeature);
				if ((new TermsImpl(p.getFirstTriple())).equals(maxStatement)) {
					return p;
				}
			}
			System.out.println("Error: No source representative!");
			
		// has no extended features - do similarity maximum to get centroid
		} else {
			SimilarityMeasure measure = new OntologyAlignmentRec(lambda);
			HashMap<PathFeature, Double> map = new HashMap<>();
			for (int i = 0; i < contents.size(); i++) {
				PathFeature ft1 = new PathFeature(contents.get(i));
				for (int j = i + 1; j < contents.size(); j++) {
					PathFeature ft2 = new PathFeature(contents.get(j));
					double k = measure.similarity(ft1, ft2);
					Double ft1v = map.get(ft1);
					Double ft2v = map.get(ft2);
					if (ft1v == null) {
						map.put(ft1, k);
					} else {
						map.put(ft1, ft1v + k);
					}
					if (ft2v == null) {
						map.put(ft2, k);
					} else {
						map.put(ft2, ft2v + k);
					}
				}
			}
			PathFeature max = null;
			Double maxValue = 0.0;
			for (PathFeature comparableFeature : map.keySet()) {
				double value = map.get(comparableFeature);
				if (maxValue < value && comparableFeature.getHostString().equals(hostname)) {
					maxValue = value;
					max = comparableFeature;
				}
			}
			return max;
		}
		// should not happen
		System.out.println("ERROR! Representative selection failed!");
		return null;
	}

	public PathFeature getRepresentative() {
		
		// if size is 1, we return the element
		if (contents.size() == 1) {
			return new PathFeature(contents.get(0));
		}
		
		// check if extended features are present
		boolean hasExtended = false;
		for (Terms comparableFeature : contents) {
			PathFeature p = new PathFeature(comparableFeature);
			if (p.isExtended()) {
				hasExtended = true;
				break;
			}
		}
		
		
		// if it has extended features, use the one that is most documented.
		if (hasExtended) {
			
			HashMap<Terms, Integer> map = new HashMap<>();
			for (Terms comparableFeature : contents) {
				PathFeature p = new PathFeature(comparableFeature);
				Terms first = new TermsImpl(p.getFirstTriple());
				Integer count = map.get(first);
				if (count == null) {
				 	map.put(first, 1);
				} else {
					map.put(first, count + 1);
				}
			}
			Integer max = 0;
			Terms maxStatement = null;
			for (Terms s : map.keySet()) {
				Integer current = map.get(s);
				if (current > max) {
					max = current;
					maxStatement = s;
				}
			}
			
			HashMap<String, Integer> hostMap = new HashMap<>();
			for (Terms comparableFeature : contents) {
				PathFeature p = new PathFeature(comparableFeature);
				if ((new TermsImpl(p.getFirstTriple())).equals(maxStatement)) {
					String host = p.getHostString();
					Integer count = hostMap.get(host);
					if (count == null) {
						hostMap.put(host, 1);
					} else {
						hostMap.put(host, count + 1);
					}										
				}
			}
			
			max = 0;
			String host = null;
			for (String s : hostMap.keySet()) {
				Integer current = hostMap.get(s);
				if (current > max) {
					max = current;
					host = s;
				}
			}
			
			for (Terms comparableFeature : contents) {
				PathFeature p = new PathFeature(comparableFeature);
				if ((new TermsImpl(p.getFirstTriple())).equals(maxStatement)) {
					if (p.getHostString().equals(host)) {
						return p; 
					}												
				}
			}
			System.out.println("cluster-representative failed.");
			
		// has no extended features - do similarity maximum to get centroid
		} else {
			SimilarityMeasure measure = new OntologyAlignmentRec(lambda);
			HashMap<PathFeature, Double> map = new HashMap<>();
			
			
			for (int i = 0; i < contents.size(); i++) {
				PathFeature ft1 = new PathFeature(contents.get(i));
				for (int j = i + 1; j < contents.size(); j++) {
					PathFeature ft2 = new PathFeature(contents.get(j));
					double k = measure.similarity(ft1, ft2);
					Double ft1v = map.get(ft1);
					Double ft2v = map.get(ft2);
					if (ft1v == null) {
						map.put(ft1, k);
					} else {
						map.put(ft1, ft1v + k);
					}
					if (ft2v == null) {
						map.put(ft2, k);
					} else {
						map.put(ft2, ft2v + k);
					}
				}
			}
			PathFeature max = null;
			Double maxValue = Double.MAX_VALUE;
			for (PathFeature comparableFeature : map.keySet()) {
				double value = map.get(comparableFeature);
				if (maxValue > value) {
					maxValue = value;
					max = comparableFeature;
				}
			}
			return max;
		}
		// should not happen
		System.out.println("Error! Selection of cluster-representative failed!");
		
		return null;
	}

	public void setContents(LinkedList<Terms> terms) {
		this.contents = terms;
	}


	public int getId() {
		return id;
	}
	
	// merges the current cluster with the given one.
	public Cluster merge(Cluster k) {
		this.mergeCount = mergeCount + k.getMergeCount() + 1;
		this.contents.addAll(k.getContents());
		this.id = k.getId();
		return this;
	}
	
	public HashSet<String> getSources() {
		HashSet<String> hostnames = new HashSet<>();
		for (Terms terms : contents) {
			PathFeature p = new PathFeature(terms);
			hostnames.add(p.getHostString().trim());
		}
		return hostnames;
	}

	public int getMergeCount() {
		return mergeCount;
	}
	
	public int getSize() {
		return this.contents.size();
	}

	public void writeToXML(boolean removeRepresentatives, XMLStreamWriter ch) throws XMLStreamException {
		if (contents.size() > 0) {
			ch.writeStartElement("cluster");

			ch.writeAttribute("id", id+"");
			ch.writeAttribute("count", contents.size()+"");
			ch.writeAttribute("sources", getSources().size()+"");
		
			if (!removeRepresentatives) {
				PathFeature f1 = getRepresentative();

				ch.writeStartElement("cluster-representative");
				ch.writeAttribute("host", f1.getHostString());
			
				(new PathFeature(new TermsImpl(f1.getFirstTriple()))).toXMLString(ch);
				ch.writeEndElement();

				ch.writeStartElement("source-representatives");
				for (String hostname : getSources()) {
					PathFeature f = getRepresentativePerSource(hostname);
					if (f != null) {
						ch.writeStartElement("source");
						ch.writeAttribute("host", f.getSourceString());
						(new PathFeature(new TermsImpl(getRepresentativePerSource(hostname).getFirstTriple()))).toXMLString(ch);
						ch.writeEndElement();
					}
				}
				ch.writeEndElement();
			}
			ch.writeStartElement("path-features");		
		
			HashSet<PathFeature> pathfeatures = new HashSet<PathFeature>();
			for (Terms features : contents) {
				pathfeatures.add(new PathFeature(features));
			}		
			for (PathFeature p : pathfeatures) {
				ch.writeStartElement("path-feature");
				ch.writeAttribute("host", p.getSourceString());
				p.toXMLString(ch);
				ch.writeEndElement();
			}
		
			ch.writeEndElement();
		
			ch.writeEndElement();
		}
	}
}
