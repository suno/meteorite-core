package bi.meteorite.core.api.security.tokenprovider;

import java.util.Map;

/**
 * An authentication token.
 */
public interface IToken {
  String getToken();

  void setToken(String token);

  String getTokenSecret();

  void setTokenSecret(String tokenSecret);

  long getTimestamp();

  void setTimestamp(long timestamp);

  Map<String, String> getProperties();

  String getProperty(String key);

  void setProperties(Map<String, String> properties);

  void setProperty(String key, String value);

  boolean isExpired(long validityDuration);
}
