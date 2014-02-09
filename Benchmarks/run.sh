#!/bin/bash
####################
## JDOOMS Worker
####################

MPI_HOME=/
JAVA_HOME=/
HOSTFILE=hostfile

## Default

NP=1
THREADS=2
MEMORY=1024
SIZE=64
BLOCK_SIZE=16

all: compile

compile: clean
	mkdir out
	$(JAVA_HOME)/bin/javac -d out/ -cp .:../jdooms/classes/artifacts/jdooms_jar/jdooms.jar src/*.java

clean:
	rm -fr out

debug:
	$(MPI_HOME)/bin/mpirun -np $(NP) $(CLUSTER) $(JAVA_HOME)/bin/java -Xmx$(MEMORY)m -cp .:out/:../jdooms/classes/artifacts/jdooms_jar/jdooms.jar se.uu.it.jdooms.node.Server GaussSeidelWorker $(THREADS) $(SIZE) $(BLOCK_SIZE)

benchmark:
	$(MPI_HOME)/bin/mpirun -np $(NP) $(JAVA_HOME)/bin/java -Xmx$(MEMORY)m -cp .:out/:../jdooms/classes/artifacts/jdooms_bench/jdooms-bench.jar se.uu.it.jdooms.node.Server GaussSeidelWorker $(THREADS) $(SIZE) $(BLOCK_SIZE)

