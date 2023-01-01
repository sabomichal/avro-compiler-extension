package com.example;

public interface Visitor {
    void visit(StateA state);

    void visit(StateB state);
}
