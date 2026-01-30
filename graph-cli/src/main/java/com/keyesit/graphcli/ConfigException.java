
package com.keyesit.graphcli;

/** Thrown when config.ini / auth.ini is missing or invalid. */
public final class ConfigException extends RuntimeException {
  public ConfigException(String message) {
    super(message);
  }
}
