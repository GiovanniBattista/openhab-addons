package org.openhab.binding.proxmox.internal.api;

import com.google.gson.GsonBuilder;

public class GsonBuilderFactory {

    public static GsonBuilder defaultBuilder() {
        return new GsonBuilder();
    }
}
