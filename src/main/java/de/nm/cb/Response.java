package de.nm.cb;

public class Response<T> {
    private T result;
    private boolean success;

    public static <T> Response<T> success(T result) {
        return new Response(result, true);
    }
    public static <R> Response<R> error(R result) {
        return new Response(result, false);
    }

    public Response(T result, boolean success) {
        this.result = result;
        this.success = success;
    }

    public T getResult() {
        return result;
    }
}
