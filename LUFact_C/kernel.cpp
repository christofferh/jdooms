#include <stdint.h>
#include "kernel.h"

void factorizeDiagonal ( void *a0, void *a1 ) {
	uint32_t i, j, k;
	float *A = (float *)a0;
	uint32_t Blck = *((uint32_t *)a1);

	for (i=0; i<Blck; i++) {
	    for (j=i+1; j<Blck; j++) {
	        A[j*MAX_MATRIX_SIZE + i] /= A[i*MAX_MATRIX_SIZE + i];
	        for (k=i+1; k<Blck; k++) {
	            A[j*MAX_MATRIX_SIZE + k] -= A[j*MAX_MATRIX_SIZE + i]*A[i*MAX_MATRIX_SIZE + k];
	        }
	    }
	}
}

void factorizeDRow ( void *a0, void *a1, void *a2 ) {
	uint32_t i, j, k;
	float *D = (float *)a0;
	float *A = (float *)a1;
	uint32_t Blck = *((uint32_t *)a2);

	for (i=0; i<Blck; i++) {
		for (j=i+1; j<Blck; j++) {
			for (k=0; k<Blck; k++) {
				A[j*MAX_MATRIX_SIZE + k] -= D[j*MAX_MATRIX_SIZE + i] * A[i*MAX_MATRIX_SIZE + k];
			}
		}
	}
}

void factorizeDCol ( void *a0, void *a1, void *a2 ) {
	uint32_t i, j, k;
	float *D = (float *)a0;
	float *A = (float *)a1;
	uint32_t Blck = *((uint32_t *)a2);

	for (i=0; i<Blck; i++) {
		for (j=0; j<Blck; j++) {
			A[j*MAX_MATRIX_SIZE + i] /= D[i*MAX_MATRIX_SIZE + i];
			for (k=i+1; k<Blck; k++) {
				A[j*MAX_MATRIX_SIZE + k] -= A[j*MAX_MATRIX_SIZE + i] * D[i*MAX_MATRIX_SIZE + k];
			}
		}
	}
}

void factorizeLU ( void *a0, void *a1, void *a2, void *a3 ) {
	uint32_t i, j, k;
	float *L = (float *)a0;
	float *A = (float *)a1;
	float *U = (float *)a2;
	uint32_t Blck = *((uint32_t *)a3);

	for (i=0; i<Blck; i++) {
		for (j=0; j<Blck; j++) {
			for (k=0; k<Blck; k++) {
				A[j*MAX_MATRIX_SIZE + k] -= L[j*MAX_MATRIX_SIZE + i] * U[i*MAX_MATRIX_SIZE + k];
			}
		}
	}
}
