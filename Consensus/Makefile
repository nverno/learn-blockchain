SHELL  = /bin/bash
SRCDIR = src/java
SRC    = $(wildcard ${SRCDIR}/*.java)
CLASS  = ${SRC:.java=.class}
DRIVER = Simulation

# Input parameters for simulation
PGRAPH ?= .1 .2 #.3
PMAL   ?= .15 #.30 .45
PTX    ?= .01 .05# .10
NROUND ?= 10 #20

all: run

compile: ${SRC}
	@javac ${SRC}

# 3x3x3x2 parameter combinations
run: compile
	@for nr in ${NROUND}; do                                   \
	  for pg in ${PGRAPH}; do                                  \
	    for pm in ${PMAL}; do                                  \
	      for ptx in ${PTX}; do                                \
	        java -cp ${SRCDIR} ${DRIVER} $$pg $$pm $$ptx $$nr; \
	      done                                                 \
	    done                                                   \
	  done                                                     \
	done

clean:
	${RM} ${CLASS}
