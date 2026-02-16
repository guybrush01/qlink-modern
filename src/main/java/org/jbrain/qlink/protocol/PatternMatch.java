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

*/

package org.jbrain.qlink.protocol;

/**
 * Represents a pattern found in protocol analysis.
 * Modernized with Java 17 records.
 */
public record PatternMatch(Type type, String mnemonic, String pattern, int frequency, double confidence) {

    public PatternMatch {
        if (mnemonic == null || mnemonic.trim().isEmpty()) {
            throw new IllegalArgumentException("Mnemonic cannot be null or empty");
        }
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException("Pattern cannot be null or empty");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
    }

    @Override
    public String toString() {
        return String.format("PatternMatch[type=%s, mnemonic=%s, pattern='%s', freq=%d, conf=%.2f]",
            type, mnemonic, pattern, frequency, confidence);
    }
}

/**
 * Pattern types for protocol analysis.
 */
enum Type {
    PAYLOAD_LENGTH,
    FIXED_BYTE,
    TEXT_PREFIX,
    NUMERIC,
    CONTAINS_NUMBERS,
    REPEATING_SEQUENCE
}