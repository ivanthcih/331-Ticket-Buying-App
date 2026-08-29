ifeq ($(OS),Windows_NT)
    SEP := ;
else
    SEP := :
endif

SOURCES := $(wildcard genericqueue/*.java tickets/*.java)

# Files you should upload to Gradescope (everything except the spec).
SUBMISSION_FILES := \
    genericqueue/GenericQueueImpl.java \
    genericqueue/GenericQueueSpecTest.java \
    genericqueue/GenericQueueImplTest.java \
    tickets/TicketApp.java \
    $(wildcard static/*) \
    REFLECTION.txt

.PHONY: compile run test clean submission

compile:
	javac -d . -cp "lib/*$(SEP)." $(SOURCES)

run: compile
	java -cp "lib/*$(SEP)." tickets.TicketApp

test: compile
	java -cp "lib/*$(SEP)." org.junit.platform.console.ConsoleLauncher execute --scan-classpath --classpath .

clean:
	rm -rf genericqueue/*.class tickets/*.class submission.zip

submission:
	rm -f submission.zip
	zip submission.zip $(SUBMISSION_FILES)
