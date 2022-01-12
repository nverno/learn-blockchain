SHELL  = /bin/bash
SRCDIR = src/java
SRC    = $(wildcard ${SRCDIR}/*.java)
CLASS  = ${SRC:.java=.class}
DRIVER = Simulation
TEST   = .1 .15 .1 10

all: run

compile: ${SRC}
	@javac ${SRC}

run: compile
	@java -cp ${SRCDIR} ${DRIVER} ${TEST}

clean:
	${RM} ${CLASS}
