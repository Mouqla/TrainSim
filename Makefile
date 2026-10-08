JAVA ?= java
JAVAC ?= javac
SOURCES := $(shell find src -type f -name '*.java')

.PHONY: all run test clean

all:
	mkdir -p bin
	$(JAVAC) -d bin $(SOURCES)
	mkdir -p bin/tsim/ui
	cp -R src/TSim/ui/bitmaps bin/tsim/ui/

run: all
	$(JAVA) -cp bin JavaMain

test: all
	$(JAVAC) -cp bin -d bin tests/HeadlessLab1Runner.java
	$(JAVA) -cp bin HeadlessLab1Runner 5 10 8000 1

clean:
	rm -rf bin/*
