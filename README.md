# FusE: Entity-centric Data Fusion on Linked Data 

To run:

    $ java -jar bin/fuse-1.0.0-SNAPSHOT-jar-with-dependencies.jar

## Example

As example, we use the files in example/data/.

### Record Linkage

The file timbl-focus.ttl contains all URIs identifying Tim Berners-Lee (the focus entity).
The timbl-focus.ttl file is created during the record linkage step.

### Data Retrieval

The files timbl-1.ttl, timbl-2.ttl and timbl-3.ttl contain various triples describing Tim Berners-Lee.
These files are collected during the data retrieval step.

The script data-retrieval.sh simulates data retrieval for the example.
The result is a file data/timbl-all.nq in N-Quads format.

### Feature Extraction

To generate path-1 and path-2 features (currently without source) for the example, run feature-extraction.sh.

### MatrixGenerationLogical

To generate the similarity matrix based on owl:sameAs, owl:equivalentProperty and owl:equivalentClass triples.
Requires a mapping Turtle file where transitivity and symmetry of the equivalence relation are materialised.

### SimilarityMatrix

	$ java -jar bin/fuse-1.0.0-SNAPSHOT-jar-with-dependencies.jar SimilarityMatrix -n example/feature-extraction/timbl-all-path.nx -o example/feature-extraction/timbl-all -l 0.5 0.25 0.75 0.0 1.0

### ClusterAnalysis

	$ java -jar bin/fuse-1.0.0-SNAPSHOT-jar-with-dependencies.jar ClusterAnalysis -c example/clustering/timbl-all_0.5_hclust_max_average.clu -l 0.5 -n example/feature-extraction/timbl-all-path.nx -o  example/clustering/timbl-all_0.5_hclust_max_average -e 0.5 0.25 0.75 1.00

#### Feature Extraction

First step: generate path features.

    $ java -jar bin/fuse-1.0.0-SNAPSHOT-jar-with-dependencies.jar FeatureExtraction -i Abu_Dhabi.nq -o Abu_Dhabi.nx

# Build

You need Linked Data-Fu as dependency.

For your convenience, the jar is available in lib/.

To build, run:

    $ mvn clean package

and then:

    $ java -jar bin/fuse-1.0.0-SNAPSHOT-jar-with-dependencies.jar