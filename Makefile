SHELL = /bin/bash
PROGS := $(shell find . -maxdepth 1 -mindepth 1 -type d -iname "[^.]*")

all:
	@echo ${PROGS}

.PHONY: ${PROGS}
${PROGS}:
	${MAKE} -C $@

clean-all:
	@for p in ${PROGS}; do        \
		${MAKE} -C $$p clean; \
	done
