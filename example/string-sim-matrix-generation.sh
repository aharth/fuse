for e in ./feature-extraction/*.nx; do java -jar ../bin/fuse-1.0.0-SNAPSHOT-jar-with-dependencies.jar SimilarityMatrix -n ${e} -o ${e%.nx} -l 0.5 0.25 0.75 0.0 1.0;done
