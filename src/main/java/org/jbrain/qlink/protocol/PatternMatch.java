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

/**
 * Represents a pattern found in protocol analysis.
 */
public class PatternMatch {
  public enum Type {
    PAYLOAD_LENGTH,
    FIXED_BYTE,
    TEXT_PREFIX,
    NUMERIC,
    CONTAINS_NUMBERS,
    REPEATING_SEQUENCE
  }

  private Type type;
  private String mnemonic;
  private String pattern;
  private int frequency;
  private double confidence;

  // Getters and Setters
  public Type getType() { return type; }
  public void setType(Type type) { this.type = type; }

  public String getMnemonic() { return mnemonic; }
  public void setMnemonic(String mnemonic) { this.mnemonic = mnemonic; }

  public String getPattern() { return pattern; }
  public void setPattern(String pattern) { this.pattern = pattern; }

  public int getFrequency() { return frequency; }
  public void setFrequency(int frequency) { this.frequency = frequency; }

  public double getConfidence() { return confidence; }
  public void setConfidence(double confidence) { this.confidence = confidence; }

  @Override
  public String toString() {
    return String.format("PatternMatch[type=%s, mnemonic=%s, pattern='%s', freq=%d, conf=%.2f]",
        type, mnemonic, pattern, frequency, confidence);
  }
}