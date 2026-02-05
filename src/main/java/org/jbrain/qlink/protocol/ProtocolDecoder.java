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
 * Utilities for decoding and analyzing Q-Link protocol data.
 * Provides hex dumps, ASCII/PETSCII interpretation, and pattern analysis.
 */
public class ProtocolDecoder {

  // PETSCII to Unicode mapping for printable characters
  // This covers the C64 character set conversion
  private static final char[] PETSCII_TO_UNICODE = new char[256];

  static {
    // Initialize with dots for unprintable characters
    for (int i = 0; i < 256; i++) {
      PETSCII_TO_UNICODE[i] = '.';
    }
    // Standard ASCII range (same in PETSCII)
    for (int i = 32; i < 127; i++) {
      PETSCII_TO_UNICODE[i] = (char) i;
    }
    // PETSCII lowercase letters (codes 65-90 in PETSCII are lowercase)
    for (int i = 65; i <= 90; i++) {
      PETSCII_TO_UNICODE[i] = (char) (i + 32); // a-z
    }
    // PETSCII uppercase letters (codes 193-218)
    for (int i = 193; i <= 218; i++) {
      PETSCII_TO_UNICODE[i] = (char) (i - 128); // A-Z
    }
  }

  /**
   * Convert bytes to hex string with spaces.
   */
  public static String hexDump(byte[] data) {
    if (data == null) return "";
    return hexDump(data, 0, data.length);
  }

  /**
   * Convert bytes to hex string without spaces.
   */
  public static String bytesToHex(byte[] data) {
    if (data == null) return "";
    return bytesToHex(data, 0, data.length);
  }

  /**
   * Convert bytes to hex string without spaces.
   */
  public static String bytesToHex(byte[] data, int offset, int length) {
    if (data == null || length <= 0) return "";
    StringBuilder sb = new StringBuilder(length * 2);
    for (int i = 0; i < length && offset + i < data.length; i++) {
      sb.append(String.format("%02X", data[offset + i] & 0xFF));
    }
    return sb.toString();
  }

  /**
   * Convert bytes to hex string with spaces.
   */
  public static String hexDump(byte[] data, int offset, int length) {
    if (data == null || length <= 0) return "";
    StringBuilder sb = new StringBuilder(length * 3);
    for (int i = 0; i < length && offset + i < data.length; i++) {
      if (i > 0) sb.append(' ');
      sb.append(String.format("%02X", data[offset + i] & 0xFF));
    }
    return sb.toString();
  }

  /**
   * Convert hex string back to bytes.
   */
  public static byte[] hexToBytes(String hex) {
    if (hex == null || hex.isEmpty()) return new byte[0];
    // Remove spaces and other separators
    hex = hex.replaceAll("[^0-9A-Fa-f]", "");
    if (hex.length() % 2 != 0) {
      hex = "0" + hex;
    }
    byte[] result = new byte[hex.length() / 2];
    for (int i = 0; i < result.length; i++) {
      result[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
    }
    return result;
  }

  /**
   * Convert bytes to ASCII string (ISO-8859-1), with unprintable chars as dots.
   */
  public static String asciiDump(byte[] data) {
    if (data == null) return "";
    return asciiDump(data, 0, data.length);
  }

  /**
   * Convert bytes to ASCII string (ISO-8859-1), with unprintable chars as dots.
   */
  public static String asciiDump(byte[] data, int offset, int length) {
    if (data == null || length <= 0) return "";
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length && offset + i < data.length; i++) {
      int b = data[offset + i] & 0xFF;
      if (b >= 32 && b < 127) {
        sb.append((char) b);
      } else {
        sb.append('.');
      }
    }
    return sb.toString();
  }

  /**
   * Convert bytes to PETSCII string (C64 character set).
   */
  public static String petsciiDump(byte[] data) {
    if (data == null) return "";
    return petsciiDump(data, 0, data.length);
  }

  /**
   * Convert bytes to PETSCII string (C64 character set).
   */
  public static String petsciiDump(byte[] data, int offset, int length) {
    if (data == null || length <= 0) return "";
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length && offset + i < data.length; i++) {
      sb.append(PETSCII_TO_UNICODE[data[offset + i] & 0xFF]);
    }
    return sb.toString();
  }

  /**
   * Format a hex dump with both hex and ASCII columns (like hexdump -C).
   */
  public static String formatHexDump(byte[] data) {
    if (data == null) return "";
    return formatHexDump(data, 0, data.length);
  }

  /**
   * Format a hex dump with both hex and ASCII columns (like hexdump -C).
   */
  public static String formatHexDump(byte[] data, int offset, int length) {
    if (data == null || length <= 0) return "";
    StringBuilder sb = new StringBuilder();
    int bytesPerLine = 16;

    for (int i = 0; i < length; i += bytesPerLine) {
      // Offset
      sb.append(String.format("%04X  ", i));

      // Hex bytes
      for (int j = 0; j < bytesPerLine; j++) {
        if (i + j < length && offset + i + j < data.length) {
          sb.append(String.format("%02X ", data[offset + i + j] & 0xFF));
        } else {
          sb.append("   ");
        }
        if (j == 7) sb.append(" ");
      }

      sb.append(" |");

      // ASCII
      for (int j = 0; j < bytesPerLine; j++) {
        if (i + j < length && offset + i + j < data.length) {
          int b = data[offset + i + j] & 0xFF;
          if (b >= 32 && b < 127) {
            sb.append((char) b);
          } else {
            sb.append('.');
          }
        }
      }

      sb.append("|\n");
    }
    return sb.toString();
  }

  /**
   * Decode a raw frame into its components.
   */
  public static FrameInfo decodeFrame(byte[] data) {
    return new FrameInfo(data, 0, data.length);
  }

  /**
   * Decode a raw frame from hex string.
   */
  public static FrameInfo decodeFrameFromHex(String hex) {
    return decodeFrame(hexToBytes(hex));
  }

  /**
   * Extract a null-terminated string from data at the given offset.
   */
  public static String extractNullTerminatedString(byte[] data, int offset) {
    if (data == null || offset >= data.length) return "";
    int end = offset;
    while (end < data.length && data[end] != 0) {
      end++;
    }
    return new String(data, offset, end - offset, StandardCharsets.ISO_8859_1);
  }

  /**
   * Extract a length-prefixed string from data at the given offset.
   * The first byte is the length.
   */
  public static String extractLengthPrefixedString(byte[] data, int offset) {
    if (data == null || offset >= data.length) return "";
    int len = data[offset] & 0xFF;
    if (offset + 1 + len > data.length) {
      len = data.length - offset - 1;
    }
    return new String(data, offset + 1, len, StandardCharsets.ISO_8859_1);
  }

  /**
   * Read an unsigned 8-bit value.
   */
  public static int readUint8(byte[] data, int offset) {
    if (data == null || offset >= data.length) return 0;
    return data[offset] & 0xFF;
  }

  /**
   * Read an unsigned 16-bit value (big-endian).
   */
  public static int readUint16BE(byte[] data, int offset) {
    if (data == null || offset + 1 >= data.length) return 0;
    return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
  }

  /**
   * Read an unsigned 16-bit value (little-endian).
   */
  public static int readUint16LE(byte[] data, int offset) {
    if (data == null || offset + 1 >= data.length) return 0;
    return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
  }

  /**
   * Find all occurrences of a byte pattern in data.
   */
  public static int[] findPattern(byte[] data, byte[] pattern) {
    if (data == null || pattern == null || pattern.length > data.length) {
      return new int[0];
    }
    java.util.List<Integer> matches = new java.util.ArrayList<>();
    for (int i = 0; i <= data.length - pattern.length; i++) {
      boolean match = true;
      for (int j = 0; j < pattern.length; j++) {
        if (data[i + j] != pattern[j]) {
          match = false;
          break;
        }
      }
      if (match) {
        matches.add(i);
      }
    }
    return matches.stream().mapToInt(Integer::intValue).toArray();
  }

  /**
   * Compare two byte arrays and show differences.
   */
  public static String compareDumps(byte[] a, byte[] b) {
    if (a == null) a = new byte[0];
    if (b == null) b = new byte[0];

    StringBuilder sb = new StringBuilder();
    int maxLen = Math.max(a.length, b.length);

    sb.append("Length: A=").append(a.length).append(" B=").append(b.length).append("\n");
    sb.append("Differences:\n");

    boolean hasDiff = false;
    for (int i = 0; i < maxLen; i++) {
      int byteA = i < a.length ? (a[i] & 0xFF) : -1;
      int byteB = i < b.length ? (b[i] & 0xFF) : -1;
      if (byteA != byteB) {
        hasDiff = true;
        sb.append(String.format("  [%04X] A=%s B=%s\n",
            i,
            byteA >= 0 ? String.format("%02X", byteA) : "--",
            byteB >= 0 ? String.format("%02X", byteB) : "--"));
      }
    }

    if (!hasDiff) {
      sb.append("  (identical)\n");
    }

    return sb.toString();
  }
}
