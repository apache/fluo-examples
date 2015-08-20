package io.fluo.commoncrawl.core;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

public class DataUtil {

  public static InetAddressValidator validator = InetAddressValidator.getInstance();

  public static String reverseDomain(String domain) {
    String[] domainArgs = domain.split("\\.");
    ArrayUtils.reverse(domainArgs);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < domainArgs.length - 1; i++) {
      sb.append(domainArgs[i]);
      sb.append(".");
    }
    sb.append(domainArgs[domainArgs.length - 1]);
    return sb.toString();
  }

  public static String toUrl(String uri) {
    String[] args = uri.split("[\\/\\?]", 2);
    String[] hostArgs = args[0].split(":", 2);
    String domain = getReverseHost(hostArgs[0]);
    StringBuilder url = new StringBuilder();
    if ((hostArgs.length == 2) && !hostArgs[1].isEmpty()) {
      if (hostArgs[1].equals("443")) {
        url.append("https://" + domain);
      } else {
        url.append(String.format("http://%s:%s", domain, hostArgs[1]));
      }
    } else {
      url.append("http://" + domain);
    }
    if (args.length == 2) {
      int sepIndex = args[0].length();
      url.append(uri.substring(sepIndex, sepIndex + 1));
      url.append(args[1]);
    }
    return url.toString();
  }

  public static boolean isValidIP(String host) {
    return validator.isValid(host);
  }

  public static String getReverseHost(String host) {
    if (isValidIP(host)) {
      return host;
    }
    return reverseDomain(host);
  }

  public static String toUri(String url) throws MalformedURLException {
    return toUri(new URL(url));
  }

  public static String toUri(URL url) {
    StringBuilder uri = new StringBuilder();

    uri.append(getReverseHost(url.getHost()));
    if ((url.getPort() != -1)) {
      uri.append(":" + Integer.toString(url.getPort()));
    } else if (url.getProtocol().equalsIgnoreCase("https")) {
      uri.append(":443");
    }

    StringBuilder urlStart = new StringBuilder();
    urlStart.append(url.getProtocol() + "://" + url.getHost());
    if (url.getPort() != -1) {
      urlStart.append(":" + Integer.toString(url.getPort()));
    }
    uri.append(url.toString().substring(urlStart.length()));

    return uri.toString();
  }
}
