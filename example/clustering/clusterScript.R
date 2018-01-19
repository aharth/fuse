#!/usr/bin/env Rscript
args = commandArgs(trailingOnly=TRUE)
# args[1] - input file
# args[2] - map file
# args[3] - output file
# args[4] - count of clusters / cutoff cluster distance
# args[5] - cluster approach i.e. hclust, hclust_height, hclust_old, kmeans
# args[6] - cluster method for hclust: [single, complete, average, median, centroid, mcquitty, ward.D, ward.D2] 
# args[7] - svg dendrogram plot file for hclust
#
# Rscript ./clusterScript.R /slow/users/ath/nqfiles/union_cleaned/trec/part2/http%3A%2F%2Fdbpedia.org%2Fresource%2FIEEE_Engineering_in_Medicine_and_Biology_Society.mtx /slow/users/ath/nqfiles/union_cleaned/trec/part2/http%3A%2F%2Fdbpedia.org%2Fresource%2FIEEE_Engineering_in_Medicine_and_Biology_Society.map /slow/users/sto/textR.txt 10 hclust single plot.svg


if (length(args)<5) {
  	stop("Error: More arguments must be supplied!", call.=FALSE)
} else if (length(args)>7) {
	stop("Error: Less arguments must be supplied!", call.=FALSE)
} else {
	inputFile = args[1]
	mapFile = args[2]
	outputFile = args[3]
	clusterCount = args[4]
	clusterApproach = args[5]
	clustermetric = args[6]
	plotFile = args[7]
}

M <- read.table(inputFile)

# load headers
#if (nchar(mapFile) > 2) {
#	map <- read.table(mapFile, colClasses=c("character","character"), sep="\t")
#	colnames(M) <- map[,1]
#}


if (clusterApproach == "hclust") {
	clusters <- hclust(as.dist(M),method=clustermetric)
	result <- cutree(clusters,k=clusterCount)
	if (length(args)==7) {
		svg(plotFile,width=160, height=90)
		plot(clusters)
	}
} else if (clusterApproach == "hclust_height") {
	clusters <- hclust(as.dist(M),method=clustermetric)
	if (clustermetric == "average") {
		clusters$height <- round(clusters$height, 8) 
	}
	result <- cutree(clusters,h=clusterCount)
	if (length(args)==7) {
		svg(plotFile,width=160, height=90)
		plot(clusters)
	}
} else if (clusterApproach == "hclust_old") {
	d <- dist(M, method = "euclidean")
	clusters <- hclust(d,method=clustermetric)
	result <- cutree(clusters,k=clusterCount)
	if (length(args)==7) {
		svg(plotFile,width=160, height=90)
		plot(clusters)
	}
} else if (clusterApproach == "kmeans") {
	clusters <- kmeans(x = as.dist(M), clusterCount)
	result <- clusters$cluster
}

write(result, file = outputFile ,ncolumns=1)
