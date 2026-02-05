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

import java.sql.Timestamp;

/**
 * Data transfer object representing a captured unknown action record.
 */
public class UnknownActionRecord {
  private int id;
  private Timestamp captureTimestamp;
  private String sessionId;
  private String userHandle;
  private String stateName;
  private String direction;
  private String mnemonic;
  private String rawHex;
  private String payloadHex;
  private boolean isUnknown;
  private String actionClass;

  // Getters and Setters
  public int getId() { return id; }
  public void setId(int id) { this.id = id; }

  public Timestamp getCaptureTimestamp() { return captureTimestamp; }
  public void setCaptureTimestamp(Timestamp captureTimestamp) { this.captureTimestamp = captureTimestamp; }

  public String getSessionId() { return sessionId; }
  public void setSessionId(String sessionId) { this.sessionId = sessionId; }

  public String getUserHandle() { return userHandle; }
  public void setUserHandle(String userHandle) { this.userHandle = userHandle; }

  public String getStateName() { return stateName; }
  public void setStateName(String stateName) { this.stateName = stateName; }

  public String getDirection() { return direction; }
  public void setDirection(String direction) { this.direction = direction; }

  public String getMnemonic() { return mnemonic; }
  public void setMnemonic(String mnemonic) { this.mnemonic = mnemonic; }

  public String getRawHex() { return rawHex; }
  public void setRawHex(String rawHex) { this.rawHex = rawHex; }

  public String getPayloadHex() { return payloadHex; }
  public void setPayloadHex(String payloadHex) { this.payloadHex = payloadHex; }

  public boolean isUnknown() { return isUnknown; }
  public void setUnknown(boolean unknown) { isUnknown = unknown; }

  public String getActionClass() { return actionClass; }
  public void setActionClass(String actionClass) { this.actionClass = actionClass; }

  @Override
  public String toString() {
    return "UnknownActionRecord{" +
        "id=" + id +
        ", timestamp=" + captureTimestamp +
        ", user='" + userHandle + '\'' +
        ", state='" + stateName + '\'' +
        ", direction='" + direction + '\'' +
        ", mnemonic='" + mnemonic + '\'' +
        ", isUnknown=" + isUnknown +
        ", actionClass='" + actionClass + '\'' +
        '}';
  }

  /**
   * Get decoded payload as text using ISO-8859-1.
   */
  public String getPayloadText() {
    if (payloadHex == null || payloadHex.isEmpty()) {
      return "";
    }
    return ProtocolDecoder.asciiDump(ProtocolDecoder.hexToBytes(payloadHex));
  }

  /**
   * Get decoded raw data as text using ISO-8859-1.
   */
  public String getRawText() {
    if (rawHex == null || rawHex.isEmpty()) {
      return "";
    }
    return ProtocolDecoder.asciiDump(ProtocolDecoder.hexToBytes(rawHex));
  }

  /**
   * Get payload as byte array.
   */
  public byte[] getPayloadBytes() {
    return ProtocolDecoder.hexToBytes(payloadHex);
  }

  /**
   * Get raw data as byte array.
   */
  public byte[] getRawBytes() {
    return ProtocolDecoder.hexToBytes(rawHex);
  }
}