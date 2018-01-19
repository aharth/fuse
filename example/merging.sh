for e in ./feature-extraction/*.nx; do for lambda in {0.0,0.25,0.5,0.75,1.0}; do for height in {0.25,0.5,0.75}; do java -jar ../bin/fuse-1.0.0-SNAPSHOT-jar-with-dependencies.jar ClusterAnalysis -c clustering/$(basename ${e%.nx})_${lambda}_hclust_height_${height}_single.clu -l ${lambda} -n feature-extraction/$(basename ${e%.nx}).nx -o  clustering/$(basename ${e%.nx})_${lambda}_hclust_height_${height}_single -e 0.5 0.25 0.75 1.00; done; done; done
for e in ./feature-extraction/*.nx; do for lambda in {0.0,0.25,0.5,0.75,1.0}; do for height in {0.25,0.5,0.75}; do java -jar ../bin/fuse-1.0.0-SNAPSHOT-jar-with-dependencies.jar ClusterAnalysis -c clustering/$(basename ${e%.nx})_${lambda}_hclust_height_${height}_complete.clu -l ${lambda} -n feature-extraction/$(basename ${e%.nx}).nx -o  clustering/$(basename ${e%.nx})_${lambda}_hclust_height_${height}_complete -e 0.5 0.25 0.75 1.00; done; done; done
for e in ./feature-extraction/*.nx; do for lambda in {0.0,0.25,0.5,0.75,1.0}; do for height in {0.25,0.5,0.75}; do java -jar ../bin/fuse-1.0.0-SNAPSHOT-jar-with-dependencies.jar ClusterAnalysis -c clustering/$(basename ${e%.nx})_${lambda}_hclust_height_${height}_average.clu -l ${lambda} -n feature-extraction/$(basename ${e%.nx}).nx -o  clustering/$(basename ${e%.nx})_${lambda}_hclust_height_${height}_average -e 0.5 0.25 0.75 1.00; done; done; done
