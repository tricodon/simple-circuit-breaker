package de.nm.cb;

public interface CircuitBreakerTask<T> {
	T processAndReturn();
	T getFallback();
}
