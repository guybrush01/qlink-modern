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

import java.nio.charset.StandardCharsets;

/**
 * Represents a decoded Q-Link protocol frame with all component parts.
 * Frame structure: START(0x5A) + CRC(4) + seq(2) + command + payload + FRAME_END(0x0D)
 */
public class FrameInfo {
  private byte[] rawFrame;
  private int crc;
  private int sequence;
  private String mnemonic;
  private byte[] payload;
  private boolean valid;
  private String parseError;

  public FrameInfo(byte[] data, int start, int len) {
    this.rawFrame = new byte[len];
    System.arraycopy(data, start, rawFrame, 0, len);
    decode();
  }

  private void decode() {
    try {
      if (rawFrame.length < 10) {
        valid = false;
        parseError = "Frame too short: " + rawFrame.length + " bytes";
        return;
      }

      // CRC is 4 bytes starting at offset 0
      crc = ((rawFrame[0] & 0xFF) << 24)
          | ((rawFrame[1] & 0xFF) << 16)
          | ((rawFrame[2] & 0xFF) << 8)
          | (rawFrame[3] & 0xFF);

      // Sequence bytes at offset 4-5
      sequence = ((rawFrame[4] & 0xFF) << 8) | (rawFrame[5] & 0xFF);

      // Command type at offset 6-7 (we skip for now)

      // Mnemonic at offset 8-9
      if (rawFrame.length >= 10) {
        mnemonic = new String(rawFrame, 8, 2, StandardCharsets.ISO_8859_1);
      }

      // Payload is everything after the mnemonic
      if (rawFrame.length > 10) {
        payload = new byte[rawFrame.length - 10];
        System.arraycopy(rawFrame, 10, payload, 0, payload.length);
      } else {
        payload = new byte[0];
      }

      valid = true;
    } catch (Exception e) {
      valid = false;
      parseError = e.getMessage();
    }
  }

  public byte[] getRawFrame() {
    return rawFrame;
  }

  public int getCrc() {
    return crc;
  }

  public int getSequence() {
    return sequence;
  }

  public String getMnemonic() {
    return mnemonic;
  }

  public byte[] getPayload() {
    return payload;
  }

  public boolean isValid() {
    return valid;
  }

  public String getParseError() {
    return parseError;
  }

  public String getRawHex() {
    return bytesToHex(rawFrame);
  }

  public String getPayloadHex() {
    return payload != null ? bytesToHex(payload) : "";
  }

  public String getPayloadAscii() {
    if (payload == null || payload.length == 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (byte b : payload) {
      char c = (char) (b & 0xFF);
      if (c >= 32 && c < 127) {
        sb.append(c);
      } else {
        sb.append('.');
      }
    }
    return sb.toString();
  }

  private static String bytesToHex(byte[] data) {
    StringBuilder sb = new StringBuilder(data.length * 3);
    for (byte b : data) {
      sb.append(String.format("%02X ", b & 0xFF));
    }
    return sb.toString().trim();
  }

  @Override
  public String toString() {
    if (!valid) {
      return "FrameInfo[invalid: " + parseError + "]";
    }
    return String.format("FrameInfo[mnemonic=%s, seq=%d, payload=%d bytes]",
        mnemonic, sequence, payload != null ? payload.length : 0);
  }
}
