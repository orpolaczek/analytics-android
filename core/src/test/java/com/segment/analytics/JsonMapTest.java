package com.segment.analytics;

import java.util.Map;
import org.fest.assertions.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.TestUtils.PROJECT_SETTINGS_JSON_SAMPLE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18)
public class JsonMapTest {
  JsonMap jsonMap;

  @Before public void setUp() {
    jsonMap = new JsonMap();
  }

  @Test public void disallowsNullMap() throws Exception {
    try {
      new JsonMap((Map) null);
      fail("Null Map should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test public void emptyMap() throws Exception {
    assertThat(jsonMap).hasSize(0).isEmpty();

    assertThat(jsonMap.get("foo")).isNull();
    assertThat(jsonMap.getDouble("foo")).isNull();
    assertThat(jsonMap.getString("foo")).isNull();
    assertThat(jsonMap.getBoolean("foo")).isNull();
    assertThat(jsonMap.getInteger("foo")).isNull();
    assertThat(jsonMap.getLong("foo")).isNull();
    assertThat(jsonMap.getChar("foo")).isNull();
    assertThat(jsonMap.getEnum(MyEnum.class, "foo")).isNull();
  }

  @Test public void conversionsAreCached() throws Exception {
    String stringPi = String.valueOf(Math.PI);

    jsonMap.put("double_pi", Math.PI);
    assertThat(jsonMap).contains(MapEntry.entry("double_pi", Math.PI));
    assertThat(jsonMap.getString("double_pi")).isEqualTo(stringPi);
    assertThat(jsonMap).contains(MapEntry.entry("double_pi", stringPi));

    jsonMap.put("string_pi", stringPi);
    assertThat(jsonMap).contains(MapEntry.entry("string_pi", stringPi));
    assertThat(jsonMap.getDouble("string_pi")).isEqualTo(Math.PI);
    assertThat(jsonMap).contains(MapEntry.entry("string_pi", Math.PI));
  }

  @Test public void enumDeserialization() throws Exception {
    jsonMap.put("value1", MyEnum.VALUE1);
    jsonMap.put("value2", MyEnum.VALUE2);
    String json = jsonMap.toString();
    assertThat(json).isEqualTo("{\"value1\":\"VALUE1\",\"value2\":\"VALUE2\"}");

    jsonMap = new JsonMap("{\"value1\":\"VALUE1\",\"value2\":\"VALUE2\"}");
    assertThat(jsonMap) //
        .contains(MapEntry.entry("value1", "VALUE1")) //
        .contains(MapEntry.entry("value2", "VALUE2"));
    assertThat(jsonMap.getEnum(MyEnum.class, "value1")).isEqualTo(MyEnum.VALUE1);
    assertThat(jsonMap.getEnum(MyEnum.class, "value2")).isEqualTo(MyEnum.VALUE2);
    assertThat(jsonMap) //
        .contains(MapEntry.entry("value1", MyEnum.VALUE1))
        .contains(MapEntry.entry("value2", MyEnum.VALUE2));
  }

  @Test public void nestedMaps() throws Exception {
    JsonMap nested = new JsonMap();
    nested.put("value", "box");
    jsonMap.put("nested", nested);

    assertThat(jsonMap).hasSize(1).contains(MapEntry.entry("nested", nested));
    assertThat(jsonMap.toString()).isEqualTo("{\"nested\":{\"value\":\"box\"}}");

    jsonMap = new JsonMap("{\"nested\":{\"value\":\"box\"}}");
    assertThat(jsonMap).hasSize(1).contains(MapEntry.entry("nested", nested));
  }

  @Test public void customJsonMapDeserialization() throws Exception {
    Settings settings = new Settings(PROJECT_SETTINGS_JSON_SAMPLE);
    assertThat(settings).hasSize(4)
        .containsKey("Amplitude")
        .containsKey("Segment.io")
        .containsKey("Flurry")
        .containsKey("Mixpanel");

    // Map Constructor
    MixpanelSettings mixpanelSettings = settings.getMixpanelSettings();
    assertThat(mixpanelSettings) //
        .contains(MapEntry.entry("token", "f7afe0cb436685f61a2b203254779e02"))
        .contains(MapEntry.entry("people", true))
        .contains(MapEntry.entry("trackNamedPages", true))
        .contains(MapEntry.entry("trackCategorizedPages", true))
        .contains(MapEntry.entry("trackAllPages", false));

    try {
      settings.getAmplitudeSettings();
    } catch (AssertionError error) {
      assertThat(error).hasMessageContaining("Could not find map constructor for");
    }
  }

  @Test public void projectSettings() throws Exception {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") JsonMap jsonMap =
        new JsonMap(PROJECT_SETTINGS_JSON_SAMPLE);

    assertThat(jsonMap.getJsonMap("Amplitude")).isNotNull()
        .hasSize(4)
        .contains(MapEntry.entry("apiKey", "ad3c426eb736d7442a65da8174bc1b1b"))
        .contains(MapEntry.entry("trackNamedPages", true))
        .contains(MapEntry.entry("trackCategorizedPages", true))
        .contains(MapEntry.entry("trackAllPages", false));
    assertThat(jsonMap.getJsonMap("Flurry")).isNotNull()
        .hasSize(4)
        .contains(MapEntry.entry("apiKey", "8DY3D6S7CCWH54RBJ9ZM"))
        .contains(MapEntry.entry("captureUncaughtExceptions", false))
        .contains(MapEntry.entry("useHttps", true))
        .contains(MapEntry.entry("sessionContinueSeconds", 10.0));
  }

  private static enum MyEnum {
    VALUE1, VALUE2
  }

  static class Settings extends JsonMap {
    Settings(String json) {
      super(json);
    }

    AmplitudeSettings getAmplitudeSettings() {
      return getJsonMap("Amplitude", AmplitudeSettings.class);
    }

    MixpanelSettings getMixpanelSettings() {
      return getJsonMap("Mixpanel", MixpanelSettings.class);
    }
  }

  static class MixpanelSettings extends JsonMap {
    MixpanelSettings(Map<String, Object> delegate) {
      super(delegate);
    }
  }

  static class AmplitudeSettings extends JsonMap {
    AmplitudeSettings(String json) {
      super(json);
    }
  }
}