package com.example;

public interface RecursiveTypeBound<T extends RecursiveTypeBound<T>> {

    @SuppressWarnings("unchecked")
    default T self() {
        return (T) this;
    }
}
