#include <xmmintrin.h>
#include <stdint.h>

#include "kernel.h"
#include "SIMD.h"

enum pLevel { None = 0, Low, Moderate, High };
enum pMode { Read = 0, Write };

#define PFETCH_RH(value) __builtin_prefetch(&(value), Read, High)
#define PFETCH_WL(value) __builtin_prefetch(&(value), Write, Low)

void pFetch1 ( void *a0, void *a1 ) {
	uint32_t i, j;
	float *A = (float *)a0;
	uint32_t Blck = *((uint32_t *)a1);

	for (i=0; i<Blck; i++) {
		for (j=0; j<Blck; j+=PFETCH_DISTANCE) {
			PFETCH_RH(*(A+j));
		}
		A += MAX_MATRIX_SIZE;
	}
/*
	for (i=0; i<Blck; i++) {
		for (j=0; j<Blck; j+=4) {
			PFETCH_RH(A[i*MAX_MATRIX_SIZE+j]);
		}
	}
*/
}

void pFetch2 ( void *a0, void *a1, void *a2 ) {
	uint32_t i, j;
	float *D = (float *)a0;
	float *A = (float *)a1;
	uint32_t Blck = *((uint32_t *)a2);

	for (i=0; i<Blck; i++) {
		for (j=0; j<Blck; j+=PFETCH_DISTANCE) {
			PFETCH_RH(*(A+j));
			PFETCH_RH(*(D+j));
		}
		A += MAX_MATRIX_SIZE;
		D += MAX_MATRIX_SIZE;
	}
/*
	for (i=0; i<Blck; i++) {
		for (j=0; j<Blck; j+=4) {
			PFETCH_RH(D[i*MAX_MATRIX_SIZE+j]);
			PFETCH_RH(A[i*MAX_MATRIX_SIZE+j]);
		}
	}
*/
}

void pFetch3 ( void *a0, void *a1, void *a2, void *a3 ) {
	uint32_t i, j;
	float *L = (float *)a0;
	float *A = (float *)a1;
	float *U = (float *)a2;
	uint32_t Blck = *((uint32_t *)a3);

	for (i=0; i<Blck; i++) {
		for (j=0; j<Blck; j+=PFETCH_DISTANCE) {
			PFETCH_WL(*(A+j));
			PFETCH_RH(*(U+j));
			PFETCH_RH(*(L+j));
		}
		A += MAX_MATRIX_SIZE;
		U += MAX_MATRIX_SIZE;
		L += MAX_MATRIX_SIZE;
	}
/*
	for (i=0; i<Blck; i++) {
		for (j=0; j<Blck; j+=4) {
			PFETCH_RH(L[i*MAX_MATRIX_SIZE+j]);
			PFETCH_RH(U[i*MAX_MATRIX_SIZE+j]);
			PFETCH_WL(A[i*MAX_MATRIX_SIZE+j]);
		}
	}
*/
}

void factorizeDiagonal_SIMD ( void *a0, void *a1 ) {
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

void factorizeDRow_SIMD ( void *a0, void *a1, void *a2 ) {
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

void factorizeDCol_SIMD ( void *a0, void *a1, void *a2 ) {
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

void factorizeLU_SIMD ( void *a0, void *a1, void *a2, void *a3 ) {
	uint32_t i, j, k;
	float *L = (float *)a0;
	float *A = (float *)a1;
	float *U = (float *)a2;
	uint32_t Blck = *((uint32_t *)a3);

	__v4sf L_0_0, L_1_0, L_2_0, L_3_0;
	__v4sf U_0_0, U_0_1, U_0_2, U_0_3;
	__v4sf R_0_0, R_0_1, R_0_2, R_0_3;
	__v4sf R_1_0, R_1_1, R_1_2, R_1_3;
	__v4sf R_2_0, R_2_1, R_2_2, R_2_3;
	__v4sf R_3_0, R_3_1, R_3_2, R_3_3;
	__v4sf WR_0, WR_1, WR_2, WR_3;

	for (i=0; i<Blck; i+=4) {
		for (j=0; j<Blck; j+=4) {
			L_0_0 = _mm_load_ps(&L[i*MAX_MATRIX_SIZE ]);
			L_1_0 = _mm_load_ps(&L[(i+1)*MAX_MATRIX_SIZE ]);
			L_2_0 = _mm_load_ps(&L[(i+2)*MAX_MATRIX_SIZE ]);
			L_3_0 = _mm_load_ps(&L[(i+3)*MAX_MATRIX_SIZE ]);

			U_0_0 = _mm_load_ps(&U[j]);
			U_0_1 = _mm_load_ps(&U[MAX_MATRIX_SIZE +j]);
			U_0_2 = _mm_load_ps(&U[2*MAX_MATRIX_SIZE +j]);
			U_0_3 = _mm_load_ps(&U[3*MAX_MATRIX_SIZE +j]);

			_MM_TRANSPOSE4_PS(U_0_0, U_0_1, U_0_2, U_0_3);

			R_0_0 = _mm_mul_ps(L_0_0, U_0_0);
			R_0_1 = _mm_mul_ps(L_0_0, U_0_1);
			R_0_2 = _mm_mul_ps(L_0_0, U_0_2);
			R_0_3 = _mm_mul_ps(L_0_0, U_0_3);

			R_1_0 = _mm_mul_ps(L_1_0, U_0_0);
			R_1_1 = _mm_mul_ps(L_1_0, U_0_1);
			R_1_2 = _mm_mul_ps(L_1_0, U_0_2);
			R_1_3 = _mm_mul_ps(L_1_0, U_0_3);

			R_2_0 = _mm_mul_ps(L_2_0, U_0_0);
			R_2_1 = _mm_mul_ps(L_2_0, U_0_1);
			R_2_2 = _mm_mul_ps(L_2_0, U_0_2);
			R_2_3 = _mm_mul_ps(L_2_0, U_0_3);

			R_3_0 = _mm_mul_ps(L_3_0, U_0_0);
			R_3_1 = _mm_mul_ps(L_3_0, U_0_1);
			R_3_2 = _mm_mul_ps(L_3_0, U_0_2);
			R_3_3 = _mm_mul_ps(L_3_0, U_0_3);

			for (k=4; k<Blck; k+=4) {
				L_0_0 = _mm_load_ps(&L[i*MAX_MATRIX_SIZE +k]);
				L_1_0 = _mm_load_ps(&L[(i+1)*MAX_MATRIX_SIZE +k]);
				L_2_0 = _mm_load_ps(&L[(i+2)*MAX_MATRIX_SIZE +k]);
				L_3_0 = _mm_load_ps(&L[(i+3)*MAX_MATRIX_SIZE +k]);

				U_0_0 = _mm_load_ps(&U[k*MAX_MATRIX_SIZE +j]);
				U_0_1 = _mm_load_ps(&U[(k+1)*MAX_MATRIX_SIZE +j]);
				U_0_2 = _mm_load_ps(&U[(k+2)*MAX_MATRIX_SIZE +j]);
				U_0_3 = _mm_load_ps(&U[(k+3)*MAX_MATRIX_SIZE +j]);

				_MM_TRANSPOSE4_PS(U_0_0, U_0_1, U_0_2, U_0_3);

				R_0_0 = _mm_add_ps(_mm_mul_ps(L_0_0, U_0_0), R_0_0);
				R_0_1 = _mm_add_ps(_mm_mul_ps(L_0_0, U_0_1), R_0_1);
				R_0_2 = _mm_add_ps(_mm_mul_ps(L_0_0, U_0_2), R_0_2);
				R_0_3 = _mm_add_ps(_mm_mul_ps(L_0_0, U_0_3), R_0_3);

				R_1_0 = _mm_add_ps(_mm_mul_ps(L_1_0, U_0_0), R_1_0);
				R_1_1 = _mm_add_ps(_mm_mul_ps(L_1_0, U_0_1), R_1_1);
				R_1_2 = _mm_add_ps(_mm_mul_ps(L_1_0, U_0_2), R_1_2);
				R_1_3 = _mm_add_ps(_mm_mul_ps(L_1_0, U_0_3), R_1_3);

				R_2_0 = _mm_add_ps(_mm_mul_ps(L_2_0, U_0_0), R_2_0);
				R_2_1 = _mm_add_ps(_mm_mul_ps(L_2_0, U_0_1), R_2_1);
				R_2_2 = _mm_add_ps(_mm_mul_ps(L_2_0, U_0_2), R_2_2);
				R_2_3 = _mm_add_ps(_mm_mul_ps(L_2_0, U_0_3), R_2_3);

				R_3_0 = _mm_add_ps(_mm_mul_ps(L_3_0, U_0_0), R_3_0);
				R_3_1 = _mm_add_ps(_mm_mul_ps(L_3_0, U_0_1), R_3_1);
				R_3_2 = _mm_add_ps(_mm_mul_ps(L_3_0, U_0_2), R_3_2);
				R_3_3 = _mm_add_ps(_mm_mul_ps(L_3_0, U_0_3), R_3_3);
			}
			WR_0 = _mm_load_ps(&A[i*MAX_MATRIX_SIZE +j]);
			WR_1 = _mm_load_ps(&A[(i+1)*MAX_MATRIX_SIZE +j]);
			WR_2 = _mm_load_ps(&A[(i+2)*MAX_MATRIX_SIZE +j]);
			WR_3 = _mm_load_ps(&A[(i+3)*MAX_MATRIX_SIZE +j]);

			_MM_TRANSPOSE4_PS(R_0_0, R_0_1, R_0_2, R_0_3);
			WR_0 -= (R_0_0 + R_0_1 + R_0_2 + R_0_3);
			_mm_store_ps(&A[i*MAX_MATRIX_SIZE +j],WR_0);

			_MM_TRANSPOSE4_PS(R_1_0, R_1_1, R_1_2, R_1_3);
			WR_1 -= (R_1_0 + R_1_1 + R_1_2 + R_1_3);
			_mm_store_ps(&A[(i+1)*MAX_MATRIX_SIZE +j],WR_1);

			_MM_TRANSPOSE4_PS(R_2_0, R_2_1, R_2_2, R_2_3);
			WR_2 -= (R_2_0 + R_2_1 + R_2_2 + R_2_3);
			_mm_store_ps(&A[(i+2)*MAX_MATRIX_SIZE +j],WR_2);

			_MM_TRANSPOSE4_PS(R_3_0, R_3_1, R_3_2, R_3_3);
			WR_3 -= (R_3_0 + R_3_1 + R_3_2 + R_3_3);
			_mm_store_ps(&A[(i+3)*MAX_MATRIX_SIZE +j],WR_3);
		}
	}
}

/*
void factorizeLU_SIMD ( void *a0, void *a1, void *a2, void *a3 ) {
	uint32_t i, j, k;
	float *L = (float *)a0;
	float *A = (float *)a1;
	float *U = (float *)a2;
	uint32_t Blck = *((uint32_t *)a3);

	for (i=0; i<Blck; i++) {
		for (j=0; j<Blck; j++) {
			for (k=0; k<Blck; k++) {
				A[i*MAX_MATRIX_SIZE + j] -= L[i*MAX_MATRIX_SIZE + k] * U[k*MAX_MATRIX_SIZE + j];
			}
		}
	}
}
*/
