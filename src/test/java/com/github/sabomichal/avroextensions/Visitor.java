package com.github.sabomichal.avroextensions;

public interface Visitor {
    void visit(StateA state);
    void visit(StateB state);
}
