#ifndef __KERNEL_H__
#define __KERNEL_H__

#define RAND(MAX) (MAX*(rand()/(RAND_MAX+1.0)))
#define SEED 10701

#define MAXRAND                             100.0
#define PAGE_SIZE               		    4096
#define DEFAULT_N                           4096
#define DEFAULT_P                           1
#define DEFAULT_B                           16

#define MAX_MATRIX_SIZE 4096
#define ALIGNMENT_SIZE 128

void factorizeDiagonal ( void *a0, void *a1 ) ;
void factorizeDRow ( void *a0, void *a1, void *a2 ) ;
void factorizeDCol ( void *a0, void *a1, void *a2 ) ;
void factorizeLU ( void *a0, void *a1, void *a2, void *a3 ) ;

#endif /* __KERNEL_H__ */
