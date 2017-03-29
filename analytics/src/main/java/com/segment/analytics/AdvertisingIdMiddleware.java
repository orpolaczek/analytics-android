package com.segment.analytics;

import static com.segment.analytics.internal.Utils.isNullOrEmpty;

import android.content.Context;
import android.support.annotation.Nullable;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.ads.identifier.AdvertisingIdClient.Info;
import com.segment.analytics.integrations.BasePayload;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.internal.NamedCallable;
import com.segment.analytics.internal.Utils.AnalyticsThreadFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AdvertisingIdMiddleware implements Middleware {

  private final Logger logger;
  private final Context context;
  private final ExecutorService executor;
  @Nullable Info advertisingIdInfo;

  public AdvertisingIdMiddleware(Logger logger, Context context) {
    this.logger = logger;
    this.context = context;
    this.executor = Executors.newSingleThreadExecutor(new AnalyticsThreadFactory());
  }

  @Override
  public void intercept(Chain chain) {
    if (advertisingIdInfo == null) {
      synchronized (this) {
        if (advertisingIdInfo == null) {
          try {
            Callable<Info> info = GetAdvertisingIdInfoCallable.with(context);
            Future<Info> future = executor.submit(info);
            advertisingIdInfo = future.get(15, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            logger.error(e, "Thread interrupted while waiting for advertising ID.");
            return;
          } catch (ExecutionException e) {
            logger.error(e, "Error retrieving advertising information.");
          } catch (TimeoutException e) {
            logger.error(e, "Thread interrupted while retrieving advertising information.");
          }
        }
      }
    }

    if (advertisingIdInfo == null) {
      chain.proceed(chain.payload());
      return;
    }

    BasePayload payload = chain.payload();

    AnalyticsContext context = payload.context();
    Map<String, Object> deviceContext = context.device();
    if (isNullOrEmpty(deviceContext)) {
      deviceContext = new LinkedHashMap<>();
    } else {
      deviceContext = new LinkedHashMap<>(deviceContext);
    }

    // Note: isLimitAdTrackingEnabled = !adTrackingEnabled.
    boolean adTrackingEnabled = !advertisingIdInfo.isLimitAdTrackingEnabled();
    String advertisingId = advertisingIdInfo.getId();

    deviceContext.put("adTrackingEnabled", adTrackingEnabled);
    if (adTrackingEnabled && !isNullOrEmpty(advertisingId)) {
      deviceContext.put("advertisingId", advertisingId);
    }

    Map<String, Object> newContext = new LinkedHashMap<>(payload.context());
    newContext.put("device", deviceContext);

    //noinspection unchecked
    BasePayload newPayload = payload.toBuilder().context(new AnalyticsContext(newContext)).build();

    chain.proceed(newPayload);
  }

  static class GetAdvertisingIdInfoCallable implements Callable<Info> {

    private static final String NAME = "Get Advertising ID Info";

    final Context context;

    static Callable<Info> with(Context context) {
      return NamedCallable.with(NAME, new GetAdvertisingIdInfoCallable(context));
    }

    private GetAdvertisingIdInfoCallable(Context context) {
      this.context = context;
    }

    @Override
    public Info call() throws Exception {
      return AdvertisingIdClient.getAdvertisingIdInfo(context);
    }
  }
}
