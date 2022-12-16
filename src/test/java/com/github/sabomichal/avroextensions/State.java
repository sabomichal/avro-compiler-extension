package com.github.sabomichal.avroextensions;

import java.util.Optional;

public interface State {

    default <T extends State> Optional<T> getAs(Class<T> clazz) {
        return clazz.isInstance(this) ? Optional.of(clazz.cast(this)) : Optional.empty();
    }

    default void accept(Visitor visitor) {
        if (this instanceof StateA) {
            visitor.visit((StateA) this);
        } else if (this instanceof StateB) {
            visitor.visit((StateB) this);
        }
    }
}
