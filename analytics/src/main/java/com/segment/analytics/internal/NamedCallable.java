package com.segment.analytics.internal;

import java.util.concurrent.Callable;

/** Callable implementation which always sets its thread name. */
public final class NamedCallable<T> implements Callable<T> {
  private final String name;
  private final Callable<T> delegate;

  public static <T> Callable<T> with(String name, Callable<T> delegate) {
    return new NamedCallable<>(name, delegate);
  }

  private NamedCallable(String name, Callable<T> delegate) {
    this.name = name;
    this.delegate = delegate;
  }

  @Override
  public T call() throws Exception {
    String oldName = Thread.currentThread().getName();
    Thread.currentThread().setName(name);
    try {
      return delegate.call();
    } finally {
      Thread.currentThread().setName(oldName);
    }
  }
}
