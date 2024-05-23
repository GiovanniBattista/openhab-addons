/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.nuki.internal.webapi.dto;

/**
 * @author danie
 *
 */
public class WebApiSmartLockDevice {
    private int smartLockId;
    private int accountId;
    private int type;
    private int lmType;
    private int authId;
    private String name;
    private boolean favorite;
    private int firmwareVersion;
    private int hardwareVersion;
    private boolean opener;
    private boolean box;
    private boolean smartDoor;
    private boolean keyturner;
    private boolean smartlock3;

    /**
     * @return the smartLockId
     */
    public int getSmartLockId() {
        return smartLockId;
    }

    /**
     * @param smartLockId the smartLockId to set
     */
    public void setSmartLockId(int smartLockId) {
        this.smartLockId = smartLockId;
    }

    /**
     * @return the accountId
     */
    public int getAccountId() {
        return accountId;
    }

    /**
     * @param accountId the accountId to set
     */
    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    /**
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * @return the lmType
     */
    public int getLmType() {
        return lmType;
    }

    /**
     * @param lmType the lmType to set
     */
    public void setLmType(int lmType) {
        this.lmType = lmType;
    }

    /**
     * @return the authId
     */
    public int getAuthId() {
        return authId;
    }

    /**
     * @param authId the authId to set
     */
    public void setAuthId(int authId) {
        this.authId = authId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the favorite
     */
    public boolean isFavorite() {
        return favorite;
    }

    /**
     * @param favorite the favorite to set
     */
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    /**
     * @return the firmwareVersion
     */
    public int getFirmwareVersion() {
        return firmwareVersion;
    }

    /**
     * @param firmwareVersion the firmwareVersion to set
     */
    public void setFirmwareVersion(int firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    /**
     * @return the hardwareVersion
     */
    public int getHardwareVersion() {
        return hardwareVersion;
    }

    /**
     * @param hardwareVersion the hardwareVersion to set
     */
    public void setHardwareVersion(int hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }

    /**
     * @return the opener
     */
    public boolean isOpener() {
        return opener;
    }

    /**
     * @param opener the opener to set
     */
    public void setOpener(boolean opener) {
        this.opener = opener;
    }

    /**
     * @return the box
     */
    public boolean isBox() {
        return box;
    }

    /**
     * @param box the box to set
     */
    public void setBox(boolean box) {
        this.box = box;
    }

    /**
     * @return the smartDoor
     */
    public boolean isSmartDoor() {
        return smartDoor;
    }

    /**
     * @param smartDoor the smartDoor to set
     */
    public void setSmartDoor(boolean smartDoor) {
        this.smartDoor = smartDoor;
    }

    /**
     * @return the keyturner
     */
    public boolean isKeyturner() {
        return keyturner;
    }

    /**
     * @param keyturner the keyturner to set
     */
    public void setKeyturner(boolean keyturner) {
        this.keyturner = keyturner;
    }

    /**
     * @return the smartlock3
     */
    public boolean isSmartlock3() {
        return smartlock3;
    }

    /**
     * @param smartlock3 the smartlock3 to set
     */
    public void setSmartlock3(boolean smartlock3) {
        this.smartlock3 = smartlock3;
    }
}
