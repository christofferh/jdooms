#!/bin/bash
####################
## JDOOMS Worker
####################
ifndef MPI_HOME
$(error MPI_HOME is not set)
endif

ifndef JAVA_HOME
$(error JAVA_HOME is not set)
endif

NP = 2
LU_SIZE = 1000
MEMORY = 1024m

all: compile

compile: clean
	mkdir out
	$(MPI_HOME)/bin/mpijavac -d out/ src/lubench/*.java

clean:
	rm -fr out

run: 
	$(MPI_HOME)/bin/mpirun -np $(NP) -hostfile $(MPI_HOME)/hostfile $(JAVA_HOME)/bin/java -Xmx$(MEMORY)  -cp .:out/ lubench.JGFLUFactBenchSize $(LU_SIZE) | tee log

shit:
	$(info dump..)

love:
	$(info cannot make love)
