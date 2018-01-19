java -jar ../bin/fuse-1.0.0-SNAPSHOT-jar-with-dependencies.jar MatrixGenerationLogical -m data/sameas-mat.ttl -m data/mapping-mat.ttl -i feature-extraction/timbl-all.nx -o evaluation/timbl-all.mtx
Rscript ./clustering/clusterScript.R evaluation/timbl-all.mtx feature-extraction/timbl-all.nx evaluation/timbl-all.clu 0.5 hclust_height single
java -jar ../bin/fuse-1.0.0-SNAPSHOT-jar-with-dependencies.jar ClusterAnalysis -s -r -c evaluation/timbl-all.clu -l 0.5 -n feature-extraction/timbl-all.nx -o  evaluation/timbl-all -e 1.00
mv evaluation/timbl-all_1.0.xml evaluation/timbl-all-silver.xml
