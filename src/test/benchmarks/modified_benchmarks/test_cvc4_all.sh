#!/bin/bash

BESTER_JAR=/mnt/c/Users/peleg/code/partialcorrectness/target/scala-2.13/bester_synthesizer-full.jar
SOLUTIONS=/mnt/c/Users/peleg/code/partialcorrectness/src/test/benchmarks/solutions.txt

echo 'Generating CVC4 dropped example files'
filesdir=`mktemp -d -p .`
echo "CVC4 files in $filesdir"

python make_cvc4.py $filesdir

export for_cvc4=$filesdir

echo "**************************************************"
printf "\nRunning CVC4-subsets\n"
echo "**************************************************"
./test_cvc4.sh > cvc4_subsets.out
echo "benchmark,rank of gold standard,time"
java -cp $BESTER_JAR ProcessCVC4Output cvc4_subsets.out $SOLUTIONS

echo "**************************************************"
printf "\nRunning CVC4-timeout\n"
echo "**************************************************"
./test_cvc4_timeout.sh > cvc4_timeout.out
echo "benchmark,rank of gold standard,time"
java -cp $BESTER_JAR ProcessCVC4Output cvc4_timeout.out $SOLUTIONS

echo 'Deleting temp files'
rm -r $filesdir