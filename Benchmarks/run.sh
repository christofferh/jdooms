#!/bin/bash
####################
## JDOOMS Benchmark tests
####################

MPI_HOME=${MPI_HOME}
JAVA_HOME=${JAVA_HOME}
HOSTFILE=${MPI_HOSTFILE}
JDOOMS_JAR="../jdooms/target/jdooms-core-1.0-SNAPSHOT-jar-with-dependencies.jar"

## Defaults
NP=1
THREADS=2
MEMORY=1024m
SIZE=64
BLOCK_SIZE=16
BENCHMARK=se.uu.it.jdooms.benchmarks.lufact.LUFactWorker

OPTIND=1 # Reset is necessary if getopts was used previously in the script.  It is a good idea to make this local in a function.
while getopts "n:t:b:h:m:s:d" opt; do
    case "$opt" in
        n)
            NP=$OPTARG
            ;;
        t)
            THREADS=$OPTARG
            ;;
        b)
            if [ "$OPTARG" == "lu" ]; then
                BENCHMARK=se.uu.it.jdooms.benchmarks.lufact.LUFactWorker
            elif [ "$OPTARG" == "gauss" ]; then
                BENCHMARK=se.uu.it.jdooms.benchmarks.gausseidel.GaussSeidelWorker
            else
                echo "-b must be 'lu' or 'gauss'"
                exit 1
            fi
            ;;
        m)
            SIZE=$OPTARG
            ;;
        s)
            BLOCK_SIZE=$OPTARG
            ;;
        h)
            HOSTFILE="-hostfile $OPTARG"
            ;;
        d)  DEBUG="-Dlog4j.configuration=log4j-debug.properties"
            ;;
        '?')
            exit 1
            ;;
    esac
done
shift $((OPTIND-1)) # Shift off the options and optional --.

$MPI_HOME/bin/mpirun -np $NP $HOSTFILE $JAVA_HOME/bin/java -Xmx$MEMORY $DEBUG -cp target/jdooms-benchmarks-1.0.jar:$JDOOMS_JAR se.uu.it.jdooms.node.Server $BENCHMARK $THREADS $SIZE $BLOCK_SIZE