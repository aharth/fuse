java -jar ../bin/fuse-1.0.0-SNAPSHOT-jar-with-dependencies.jar FeatureExtraction -i data/timbl-all.nq -o feature-extraction/timbl-all.nx
# remove first line
sed -i 1d feature-extraction/timbl-all.nx
