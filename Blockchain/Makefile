SHELL  = /bin/bash
SRCDIR = src/java
SRC    = $(wildcard ${SRCDIR}/*.java)
CLASS  = ${SRC:.java=.class}
DRIVER = 
TEST   = 

all: run

compile: ${SRC}
	@javac ${SRC}

run: compile

clean:
	${RM} ${CLASS}
