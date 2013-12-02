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

all: compile

compile: clean
	mkdir out
	$(JAVA_HOME)/bin/javac -d out/ -cp .:../jdooms/classes/artifacts/jdooms_jar/jdooms.jar src/se/uu/it/jdooms/worker/java/*.java

clean:
	rm -fr out

run:
	$(MPI_HOME)/bin/mpirun -np $(NP) $(JAVA_HOME)/bin/java -cp .:out/:../jdooms/classes/artifacts/jdooms_jar/jdooms.jar se.uu.it.jdooms.node.Server se.uu.it.jdooms.worker.java.Worker 1

shit:
	$(info dump..)

love:
	$(info cannot make love)
