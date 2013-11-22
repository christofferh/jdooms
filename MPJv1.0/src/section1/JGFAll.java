package section1;
/**************************************************************************
*                                                                         *
*             Java Grande Forum Benchmark Suite - MPJ Version 1.0         *
*                                                                         *
*                            produced by                                  *
*                                                                         *
*                  Java Grande Benchmarking Project                       *
*                                                                         *
*                                at                                       *
*                                                                         *
*                Edinburgh Parallel Computing Centre                      *
*                                                                         * 
*                email: epcc-javagrande@epcc.ed.ac.uk                     *
*                                                                         *
*                                                                         *
*      This version copyright (c) The University of Edinburgh, 2001.      *
*                         All rights reserved.                            *
*                                                                         *
**************************************************************************/


import jgfutil.*; 
import mpi.*;

public class JGFAll{ 

  public static int nprocess;
  public static int rank;

  public static void main(String argv[]) throws MPIException{

/* Initialise MPI */
    MPI.Init(argv);
    rank = MPI.COMM_WORLD.Rank();
    nprocess = MPI.COMM_WORLD.Size();

    if(rank==0) {
      JGFInstrumentor.printHeader(1,0,nprocess);
    }

    /*JGFAlltoallBench ata = new JGFAlltoallBench(nprocess);
    ata.JGFrun();*/

    /*JGFBarrierBench ba = new JGFBarrierBench(nprocess);
    ba.JGFrun();*/

    JGFBcastBench bc = new JGFBcastBench(nprocess);
    bc.JGFrun();

    JGFGatherBench ga = new JGFGatherBench(nprocess);
    ga.JGFrun();

    JGFPingPongBench pp = new JGFPingPongBench(nprocess);
    pp.JGFrun();

    JGFReduceBench rd = new JGFReduceBench(nprocess);
    rd.JGFrun();

    JGFScatterBench sc = new JGFScatterBench(nprocess);
    sc.JGFrun();

/* Finalise MPI */
    MPI.Finalize();
 
  }
}


