RESULT=result.csv
HOSTFILE=/opt/openmpi-1.9-java-object/hostfile
MEMORY=1024m

NP_INIT=4
NP_INC=2
NP_MAX=6

LU_INIT=1024
LU_INC=1024
LU_MAX=4096

echo START $(date) NP: $NP_INIT, $NP_INC, $NP_MAX. LU: $LU_INIT, $LU_INC, $LU_MAX. >> $RESULT
for ((NP = $NP_INIT; NP <= $NP_MAX; NP += $NP_INC)); do
	for ((LU = $LU_INIT; LU <= $LU_MAX; LU += $LU_INC)); do
		MPI_RUN="$MPI_HOME/bin/mpirun -np $NP -hostfile $HOSTFILE $JAVA_HOME/bin/java -Xmx$MEMORY -cp .:out/ lubench.JGFLUFactBenchSize $LU"
		$MPI_RUN >> $RESULT
	done
done
echo STOP >> $RESULT

