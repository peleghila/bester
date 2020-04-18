#!/bin/bash

filename=`echo $1 | cut -d'/' -f 2`
echo "$filename"
base_filename="${filename%.*}"
out=`timeout 20 $CVC4_BIN $1`
retval=$?
if [ $retval = 0 ]; then
	echo $out
fi
for cvc4_file in $for_cvc4/$base_filename*.sl; do
	out=`timeout 20 $CVC4_BIN $cvc4_file`
	retval=$?
	if [ $retval = 0 ]; then
		echo $out
	fi
done