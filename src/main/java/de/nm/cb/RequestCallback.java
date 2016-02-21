package de.nm.cb;

public interface RequestCallback {
    void onSuccess();

    void onError(Throwable throwable);
}
