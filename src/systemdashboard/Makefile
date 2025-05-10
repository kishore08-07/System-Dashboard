CC=gcc
JAVA_HOME=$(shell readlink -f /usr/bin/javac | sed "s:/bin/javac::")
CFLAGS=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -fPIC -Wall
LDFLAGS=-shared

SOURCES=systeminfo.c
LIBRARY=libsysteminfo.so

all: $(LIBRARY)

$(LIBRARY): $(SOURCES)
	$(CC) $(CFLAGS) $(LDFLAGS) -o $@ $^

clean:
	rm -f $(LIBRARY) 