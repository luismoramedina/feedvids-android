package com.feedvids.controller.service;

/**
 * @author mora on 24/06/14.
 */
public interface PocketService {
    java.lang.String KEY_AUTHORIZATION_URL = "";
    java.lang.String REQUEST_TOKEN = "REQUEST_TOKEN";

    public String retrieve();

    public String modify(String action, String item);

    public String archive(String item);

    public String delete(String item);

    public String getRequestToken(String redirectUrl);

    public String authorize(String code);
}
