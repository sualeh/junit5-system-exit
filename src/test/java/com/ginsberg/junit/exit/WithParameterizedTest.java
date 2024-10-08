package com.ginsberg.junit.exit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import us.fatehi.SystemExit;
import static com.ginsberg.junit.exit.TestUtils.assertParameterizedTestFails;

public class WithParameterizedTest {

  @Test
  @DisplayName("@ParameterizedTest on method should reset state between tests")
  void failOnSystemExitOnClass() {
    assertParameterizedTestFails(
        WithParameterizedTest.SucceedsAndThenFails.class, true, false, true);
  }

  @EnabledIfSystemProperty(named = "running_within_test", matches = "true")
  static class SucceedsAndThenFails {
    @ParameterizedTest(name = "{index}")
    @ValueSource(booleans = {true, false, true})
    @ExpectSystemExit
    public void testBasicAssumptions(boolean shouldSucceed) {
      if (shouldSucceed) {
        SystemExit.exit(1); // This test expects a system exit to succeed
      }
    }
  }
}
