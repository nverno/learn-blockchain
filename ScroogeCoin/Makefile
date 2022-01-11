SHELL  = /bin/bash
SRCDIR = src/java
SRC    = $(wildcard ${SRCDIR}/*.java)
CLASS  = ${SRC:.java=.class}
DRIVER =
TEST   = 

all: compile

compile: ${SRC}
	@javac ${SRC}

run: compile
	@java -cp ${SRCDIR} ${DRIVER} ${TEST}

clean:
	${RM} ${CLASS}
