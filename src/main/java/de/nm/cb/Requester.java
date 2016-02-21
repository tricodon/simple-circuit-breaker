package de.nm.cb;

public class Requester<T> {

    private CircuitBreakerTask<T> task;

    public Requester(CircuitBreakerTask<T> task) {
        this.task = task;
    }

    public Response<T> processWithFallbackOnError(RequestCallback requestCallback) {
        try {
            T result = task.processAndReturn();
            requestCallback.onSuccess();
            return Response.success(result);
        } catch (Throwable onProcessException) {
            requestCallback.onError(onProcessException);
            return Response.error(task.getFallback());
        }
    }

    public Response<T> fallback() {
        T fallback = task.getFallback();
        return Response.success(fallback);
    }
}
