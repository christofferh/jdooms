# Java Distributed Object-Oriented Memory Sharing (JDOOMS)
JDOOMS is a library for sharing Java objects in a cluster computer environment. For inter-node communication, MPI (Open MPI) is used.

### Build JDOOMS core:
	mvn compile assembly:single

### Build Benchmarks:
	mvn package

### Running benchmarks:
E.g. Running LU-Fact in cluster config with a hostfile, number of processes (NP) 2, threads 2, matrix size 1024 and block size 64 in debug mode. Remove -d to disable debugging.

	run.sh -h hostfile -n 2 -t 2 -b lufact.LUFactWorker -m 1024 -s 64 -d

### Dependencies
Open MPI 1.9 with Java bindings (only available in the nightly trunk).