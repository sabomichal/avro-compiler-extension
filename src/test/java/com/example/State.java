package com.example;

import java.util.Optional;

public sealed interface State permits StateA, StateB {

    default <T extends State> Optional<T> getAs(Class<T> clazz) {
        return clazz.isInstance(this) ? Optional.of(clazz.cast(this)) : Optional.empty();
    }

    default void accept(Visitor visitor) {
        if (this instanceof StateA s) {
            visitor.visit(s);
        } else if (this instanceof StateB s) {
            visitor.visit(s);
        }
    }
}
