java -jar ../bin/fuse-1.0.0-SNAPSHOT-jar-with-dependencies.jar Evaluator -o1 ./evaluation/timbl-all_silver_purity.csv -o2 ./evaluation/timbl-all_silver_nmi.csv -i ./evaluation/timbl-all-silver.xml -d clustering/timbl-all -l 0.0 0.25 0.5 0.75 1.0 -n hclust_height -m single average complete -c 0.25 0.5 0.75 -e 0.25 0.5 0.75 1.0
java -jar ../bin/fuse-1.0.0-SNAPSHOT-jar-with-dependencies.jar Evaluator -o1 ./evaluation/timbl-all_gold_purity.csv -o2 ./evaluation/timbl-all_gold_nmi.csv -i ./evaluation/timbl-all-gold.xml -d clustering/timbl-all -l 0.0 0.25 0.5 0.75 1.0 -n hclust_height -m single average complete -c 0.25 0.5 0.75 -e 0.25 0.5 0.75 1.0
