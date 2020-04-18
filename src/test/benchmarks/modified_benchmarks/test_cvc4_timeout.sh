#!/bin/bash
export CVC4_BIN=~/cvc4-1.7-x86_64-linux-opt
export for_cvc4=./for_cvc4

test_file () {
	STARTTIME=$(date +%s)
	timeout 45 ./test_cvc4_inner.sh $1
	ENDTIME=$(date +%s)
	echo "$(($ENDTIME - $STARTTIME))s"
	echo ----
}
for f in contradiction/*.sl; do
  test_file $f
done
for f in returns_garbage/*.sl; do
  test_file $f
done