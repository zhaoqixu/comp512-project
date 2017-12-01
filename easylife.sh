#!/bin/bash


if [ $USER = "zxu32" ]
then
	export CLASSPATH=/home/2014/zxu32/DSP/server_512/servercode:/home/2014/zxu32/DSP/client_512/clientsrc:/home/2014/zxu32/DSP/MidInterface/MidInterface.jar:/home/2014/zxu32/DSP/middleware_512/middlewaresrc:/home/2014/zxu32/DSP/ResInterface/ResInterface.jar:/home/2014/zxu32/DSP/MidInterface/MidInterface.jar:/home/2014/zxu32/DSP
	MW_PATH="-Djava.rmi.server.codebase=file:/home/2014/zxu32/DSP/middleware_512/middlewaresrc/"
	RM_PATH="-Djava.rmi.server.codebase=file:/home/2014/zxu32/DSP/server_512/servercode/"
elif [ $USER = "zzhang334" ]
then
	export CLASSPATH=/home/2015/zzhang334/DSP/client_512/clientsrc:/home/2015/zzhang334/DSP/server_512/servercode:/home/2015/zzhang334/DSP/middleware_512/middlewaresrc:/home/2015/zzhang334/DSP/MidInterface/MidInterface.jar:/home/2015/zzhang334/DSP/ResInterface/ResInterface.jar:/home/2015/zzhang334/DSP
	MW_PATH="-Djava.rmi.server.codebase=file:home/2015/zzhang334/DSP/middleware_512/middlewaresrc/"
	RM_PATH="-Djava.rmi.server.codebase=file:/home/2015/zzhang334/DSP/server_512/servercode/"
fi

if [ $1 = 'c' ]
then
	cd client_512/clientsrc
	javac client.java
	java client $2 1088
elif [ $1 = 'm' ] 
then
	cd middleware_512/middlewaresrc
	killall -9 rmiregistry
	rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 1088 &
	javac MidImpl/MiddleWareImpl.java
	java -Djava.security.policy=java.policy $MW_PATH  MidImpl.MiddleWareImpl $2 $3 $4
elif [ $1 = 's' ]
then
	cd server_512/servercode
	killall -9 rmiregistry
	rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 2199 &
	javac ResImpl/ResourceManagerImpl.java
	java -Djava.security.policy=java.policy $RM_PATH  ResImpl.ResourceManagerImpl 2199 $2
elif [ $1 = 'r' ]
then
	cd server_512/servercode
	killall -9 rmiregistry
	rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 2199 &
	javac ResImpl/ResourceManagerImpl.java
	java -Djava.security.policy=java.policy $RM_PATH  ResImpl.ResourceManagerImpl 2199 $2 $3 1088
elif [ $1 = 'z' ]
then
	find . -name '*.txt' -delete
	find . -name '*.log' -delete
	find . -name '*.class' -delete
fi