package com.feedvids.controller;

/**
 * @author luismoramedina
 */
public interface Notificable {
    public void notifyActionFinish(String action, Object data);
}
