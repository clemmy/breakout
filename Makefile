JAVAC=javac
sources = $(wildcard *.java)
classes = $(sources:.java=.class)

all: $(classes)

clean:
	rm -f *.class

%.class : %.java
	$(JAVAC) $<

run:
	javac *.java
	java Breakout 240 30
