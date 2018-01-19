java -jar ../lib/ldfu-standalone-0.10.1-SNAPSHOT.jar -i data/timbl-focus.ttl data/timbl-1.ttl data/timbl-2.ttl data/timbl-3.ttl -o data/timbl-all.nq
cd data/
rapper -i nquads -o turtle timbl-all.nq  > timbl-all.ttl
rapper -i nquads -o nquads timbl-all.nq  > timbl-all.1.nq
mv timbl-all.1.nq timbl-all.nq
