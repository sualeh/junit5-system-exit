/*
 * MIT License
 *
 * Copyright (c) 2021 Todd Ginsberg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.ginsberg.junit.exit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import java.lang.annotation.Annotation;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import us.fatehi.SystemExitException;

/**
 * Does the work of installing the DisallowExitSecurityManager, interpreting the test results, and
 * returning the original SecurityManager to service.
 */
public class SystemExitExtension
    implements BeforeEachCallback, AfterEachCallback, TestExecutionExceptionHandler {

  private boolean failOnSystemExit;
  private boolean expectSystemExit;
  private Integer expectedStatusCode;
  private boolean systemExitCalled;
  private Integer systemExitCode;

  @Override
  public void afterEach(final ExtensionContext context) {
    try {
      if (failOnSystemExit && systemExitCalled) {
        fail("Unexpected System.exit(" + systemExitCode + ") caught");
      }

      if (expectSystemExit && expectedStatusCode == null && !systemExitCalled) {
        fail("Expected System.exit() to be called, but it was not");
      }

      if (expectSystemExit && expectedStatusCode != null && !systemExitCalled) {
        fail("Expected System.exit(" + expectedStatusCode + ") to be called, but it was not.");
      }

      if (expectSystemExit
          && expectedStatusCode != null
          && systemExitCalled
          && systemExitCode != null) {
        assertEquals(
            expectedStatusCode,
            systemExitCode,
            "Expected System.exit(" + expectedStatusCode + ") to be called, but it was not.");
      }
    } finally {
      // Clear state so if this is run as part of a @ParameterizedTest, the next time through we'll
      // have the correct state
      expectSystemExit = false;
      expectedStatusCode = null;
      failOnSystemExit = false;
      systemExitCalled = false;
      systemExitCode = null;
    }
  }

  @Override
  public void beforeEach(final ExtensionContext context) {
    // Should we fail on a System.exit() rather than letting it bubble out?
    failOnSystemExit = getAnnotation(context, FailOnSystemExit.class).isPresent();

    getAnnotation(context, ExpectSystemExit.class).ifPresent(code -> expectSystemExit = true);

    getAnnotation(context, ExpectSystemExitWithStatus.class)
        .ifPresent(
            code -> {
              expectSystemExit = true;
              expectedStatusCode = code.value();
            });
  }

  /**
   * This is here so we can catch exceptions thrown by our own security manager and prevent them
   * from stopping the annotated test. If anything other than our own exception comes through, throw
   * it because the system SecurityManager to which we delegate prevented some other action from
   * happening.
   *
   * @param context the current extension context; never {@code null}
   * @param throwable the {@code Throwable} to handle; never {@code null}
   * @throws Throwable if the throwable argument is not a SystemExitPreventedException
   */
  @Override
  public void handleTestExecutionException(
      final ExtensionContext context, final Throwable throwable) throws Throwable {
    if (!(throwable instanceof SystemExitException)) {
      throw throwable;
    }
    systemExitCalled = true;
    systemExitCode = ((SystemExitException) throwable).getExitCode();
  }

  // Find the annotation on a method, or failing that, a class.
  private <T extends Annotation> Optional<T> getAnnotation(
      final ExtensionContext context, final Class<T> annotationClass) {
    final Optional<T> method = findAnnotation(context.getTestMethod(), annotationClass);
    if (method.isPresent()) {
      return method;
    }
    return findAnnotation(context.getTestClass(), annotationClass);
  }
}
