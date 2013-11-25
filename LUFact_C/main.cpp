#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <getopt.h>
#include <sys/time.h>
#include <unistd.h>
#include <string.h>
#include <assert.h>

#if defined(__i386__)
#define	PRINT_STR_LLU		"%llu"
#elif defined(__x86_64__)
#define	PRINT_STR_LLU		"%lu"
#else
#error "*** Incompatible Architecture ***"
#endif

#define MAX_MATRIX_SIZE 4096
#define ALIGNMENT_SIZE 128
#define MAXRAND 100.0
#define RAND(MAX) (MAX*(rand()/(RAND_MAX+1.0)))
#define SEED 10701
#define DEFAULT_N                           4096
#define DEFAULT_P                           1
#define DEFAULT_B                           16

float A[MAX_MATRIX_SIZE][MAX_MATRIX_SIZE]			__attribute__ ((aligned(ALIGNMENT_SIZE)));

uint64_t init_t = 0;
uint64_t t_start;
uint64_t t_end;

uint32_t N = DEFAULT_N;
uint32_t P = DEFAULT_P;
uint32_t BlockSize = DEFAULT_B;
uint8_t PrintA = 0;
uint8_t RefLU = 0;
uint8_t vct = 0;
uint8_t DAE = 0;

#define NSIMD_CAE	0b00000000
#define NSIMD_DAE	0b00000001
#define SIMD_CAE	0b00000010
#define SIMD_DAE	0b00000011

void LU ( void ) ;
void LU_SIMD ( void ) ;
void LU_DAE ( void ) ;
void LU_SIMD_DAE ( void ) ;
void LU_Base ( void ) ;

void initA ( uint32_t N ) ;
void printLU ( uint32_t N ) ;
int is_log2 ( int number ) ;

void printPerformance ( uint64_t st, uint64_t end );

static __inline__ uint64_t getTime ( void ) {
	struct timeval tmp;
	gettimeofday(&tmp, NULL);
	return (tmp.tv_sec * 1000000 + tmp.tv_usec);
}

int main ( int argc, char *argv[] ) {
	uint64_t tmp_t;
	int ch;

	tmp_t = getTime();

	while ((ch = getopt(argc, argv, "n:p:b:m:csRvoh")) != -1) {
		switch(ch) {
		case 'n': N = atoi(optarg); break;
		case 'p': P = atoi(optarg); break;
		case 'b': BlockSize = atoi(optarg); break;
		case 'm': if(atoi(optarg)>0) DAE = 1; break;
		case 'R': RefLU = 1; break;
		case 'v': vct = 1; break;
		case 'o': PrintA = 1; break;
		case 'h': printf("Usage: LU <options>\n\n");
		printf("options:\n");
		printf("  -nN : Decompose NxN matrix.\n");
		printf("  -pP : P = number of processors.\n");
		printf("  -bB : Use a block size of B. BxB elements should fit in cache for \n");
		printf("        good performance. Small block sizes (B=8, B=16) work well.\n");
		printf("  -mM : {0,1} Disabled/Enabled decoupled access-execute model.\n");
		printf("  -R  : Reference -sequential- LU factorization. (for debug purposes)\n");
		printf("  -v  : Use SSE-SIMD kernels.\n");
		printf("  -o  : Print out matrix values.\n");
		printf("  -h  : Print out command line options.\n\n");
		printf("Default: LU -n%1d -p%1d -b%1d\n", DEFAULT_N, DEFAULT_P, DEFAULT_B);
		exit(0);
		break;
		}
	}

	if(N > MAX_MATRIX_SIZE){
		perror("Matrix size given greater than max matrix size");
		exit(-1);
	}
	if((N % 16) != 0){
		perror("Matrix size should be 16 elements aligned");
		exit(-1);
	}
	if(is_log2(N) < 0){
		perror("Matrix size must be a power of 2");
		exit(-1);
	}

	if(BlockSize > N){
		perror("Block size given greater than matrix size");
		exit(-1);
	}
	if((BlockSize % 16) != 0){
		perror("Block size should be 16 elements aligned");
		exit(-1);
	}
	if(N % BlockSize){
		perror("Block size should be a multiply of matrix size.\n");
		exit(-1);
	}
	if(is_log2(BlockSize) < 0){
		perror("Block size must be a power of 2");
		exit(-1);
	}

	printf("\n");
	printf("Blocked Dense LU Factorization\n");
	printf("     %d by %d Matrix\n", N, N);
	if(RefLU){
		printf("     Reference LU Factorization\n");
	}
	
	printf("\n");
	printf("\n");

	initA(N);

	init_t = (getTime() - tmp_t);

	if(RefLU){
		t_start = getTime();
		LU_Base();
		t_end = getTime();
		printPerformance(t_start, t_end);
	}

	if(PrintA){
		printLU(N);
	}
}


void initA ( uint32_t N ) {
	uint32_t i, j;

	srand(SEED);

	for(i=0; i<N; i++){
		for(j=0; j<N; j++){
			A[i][j] = RAND(MAXRAND);
		}
	}
}

void printLU ( uint32_t N ) {
	uint32_t i, j;
	FILE *fp;

	if(RefLU){
		fp = fopen("reference.dump", "w");
		assert(fp != NULL);
	}
	else{
		fp = fopen("lu.dump", "w");
		assert(fp != NULL);
	}

	fprintf(fp, "%u\n", N);
	for(i=0; i<N; i++){
		for(j=0; j<N; j++){
			fprintf(fp, "%10.4lf ", A[i][j]);
		}
		fprintf(fp, "\n");
	}

	fclose(fp);
}

void LU_Base ( void ) {
	uint32_t i, j, k;

	for (i=0; i<N; i++) {
	    for (j=i+1; j<N; j++) {
	        A[j][i] /= A[i][i];
	        for (k=i+1; k<N; k++) {
	            A[j][k] -= A[j][i]*A[i][k];
	        }
	    }
	}
}

void printPerformance ( uint64_t st, uint64_t end ) {
	uint64_t ticks = (end - st) + init_t;
	double time_non_init = (double)(end - st)/(1000000.0);
	double time = (double)ticks/(1000000.0);
	double init_time = (double)init_t/(1000000.0);
	double GFLOPs = (double)(((long double)((1.0/3.0)*N*N*N)/(long double)time_non_init)/(long double)1000000000.0);

	printf("Total Time (Seconds)              :: %lf\n", time);
	printf("Init Time (Seconds)               :: %lf\n", init_time);
	printf("GFLOPs                            :: %lf\n\n", GFLOPs);
}

int is_log2 ( int number ) {
	int cumulative = 1;
	int out = 0;
	int done = 0;

	while ((cumulative < number) && (!done) && (out < 50)) {
		if (cumulative == number) {
			done = 1;
		} else {
			cumulative = cumulative * 2;
			out ++;
		}
	}

	if (cumulative == number) {
		return(out);
	} else {
		return(-1);
	}
}
