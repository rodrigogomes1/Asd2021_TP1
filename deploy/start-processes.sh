#!/bin/bash

processes=$1

i=0
port=10000

while [ $i -lt $processes ]
do
	(java -jar target/asdProj.jar -conf babel_config.properties interface=eth0 address=192.168.18.251 port=$[$port+$i] contact=192.168.18.251:$port my_index=$(($i + 1)) | tee results/results-$(hostname)-$[$port+$i].txt)&
	i=$[$i+1]	
done
