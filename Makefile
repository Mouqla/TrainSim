JAVA ?= java
JAVAC ?= javac

.PHONY: all run test clean

all:
	mkdir -p bin
	$(JAVAC) -sourcepath src -d bin src/Main.java

run: all
	$(JAVA) -cp bin JavaMain

test: all
	$(JAVAC) -cp bin -sourcepath src:tests -d bin tests/HeadlessLab1Runner.java
	$(JAVA) -cp bin HeadlessLab1Runner 5 10 8000 1

clean:
	rm -rf bin/*
