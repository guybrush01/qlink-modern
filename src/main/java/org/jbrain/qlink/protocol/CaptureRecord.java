/*
Copyright Jim Brain and Brain Innovations, 2005.

This file is part of QLinkServer.

QLinkServer is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

QLinkServer is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with QLinkServer; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

@author Jim Brain
*/
package org.jbrain.qlink.protocol;

import java.util.Date;

/**
 * Represents a captured protocol message with full context.
 */
public class CaptureRecord {

  public enum Direction {
    INBOUND,
    OUTBOUND
  }

  private int id;
  private Date timestamp;
  private String sessionId;
  private String userHandle;
  private String stateName;
  private Direction direction;
  private String mnemonic;
  private byte[] rawData;
  private byte[] payload;
  private boolean isUnknown;
  private String actionClassName;

  public CaptureRecord() {
    this.timestamp = new Date();
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getUserHandle() {
    return userHandle;
  }

  public void setUserHandle(String userHandle) {
    this.userHandle = userHandle;
  }

  public String getStateName() {
    return stateName;
  }

  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  public Direction getDirection() {
    return direction;
  }

  public void setDirection(Direction direction) {
    this.direction = direction;
  }

  public String getMnemonic() {
    return mnemonic;
  }

  public void setMnemonic(String mnemonic) {
    this.mnemonic = mnemonic;
  }

  public byte[] getRawData() {
    return rawData;
  }

  public void setRawData(byte[] rawData) {
    this.rawData = rawData;
  }

  public byte[] getPayload() {
    return payload;
  }

  public void setPayload(byte[] payload) {
    this.payload = payload;
  }

  public boolean isUnknown() {
    return isUnknown;
  }

  public void setUnknown(boolean unknown) {
    isUnknown = unknown;
  }

  public String getActionClassName() {
    return actionClassName;
  }

  public void setActionClassName(String actionClassName) {
    this.actionClassName = actionClassName;
  }

  public String getRawHex() {
    return bytesToHex(rawData);
  }

  public String getPayloadHex() {
    return payload != null ? bytesToHex(payload) : "";
  }

  private static String bytesToHex(byte[] data) {
    if (data == null) return "";
    StringBuilder sb = new StringBuilder(data.length * 3);
    for (byte b : data) {
      sb.append(String.format("%02X ", b & 0xFF));
    }
    return sb.toString().trim();
  }

  @Override
  public String toString() {
    return String.format("CaptureRecord[%s %s %s user=%s state=%s mnemonic=%s unknown=%b]",
        timestamp, direction, sessionId, userHandle, stateName, mnemonic, isUnknown);
  }
}
