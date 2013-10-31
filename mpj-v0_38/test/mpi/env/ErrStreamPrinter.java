package mpi.env; 

import mpi.*;
 
public class ErrStreamPrinter {

  public ErrStreamPrinter() {
  }

  public ErrStreamPrinter(String[] args) throws Exception {
    int me, size ;
    String[] nargs = MPI.Init(args);
    me = MPI.COMM_WORLD.Rank() ; 
    size = MPI.COMM_WORLD.Size() ;   

    if(me == 0) { 
      System.out.println(" this is output stream "); 
      System.err.println(" this is error stream"); 
    }

    if(me == 0)  System.out.println("ErrStreamPrinter TEST COMPLETE\n");
    MPI.Finalize();
  }
}
