#ifndef __SIMD_H__
#define __SIMD_H__

#define PFETCH_DISTANCE 16

void pFetch1 ( void *a0, void *a1 ) ;
void pFetch2 ( void *a0, void *a1, void *a2 ) ;
void pFetch3 ( void *a0, void *a1, void *a2, void *a3 ) ;

void factorizeDiagonal_SIMD ( void *a0, void *a1 ) ;
void factorizeDRow_SIMD ( void *a0, void *a1, void *a2 ) ;
void factorizeDCol_SIMD ( void *a0, void *a1, void *a2 ) ;
void factorizeLU_SIMD ( void *a0, void *a1, void *a2, void *a3 ) ;

#endif	/*	__SIMD_H__	*/
