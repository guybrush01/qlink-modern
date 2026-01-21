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

import java.util.HashSet;
import java.util.Set;

/**
 * Filter criteria for protocol capture.
 */
public class ProtocolFilter {

  private Set<String> mnemonics = new HashSet<>();
  private Set<String> sessionIds = new HashSet<>();
  private Set<String> userHandles = new HashSet<>();
  private Set<String> stateNames = new HashSet<>();
  private boolean unknownOnly = false;
  private boolean inboundOnly = false;
  private boolean outboundOnly = false;

  public ProtocolFilter() {
  }

  /**
   * Creates a filter that only captures unknown actions.
   */
  public static ProtocolFilter unknownOnly() {
    ProtocolFilter filter = new ProtocolFilter();
    filter.setUnknownOnly(true);
    return filter;
  }

  /**
   * Creates a filter for a specific session.
   */
  public static ProtocolFilter forSession(String sessionId) {
    ProtocolFilter filter = new ProtocolFilter();
    filter.addSessionId(sessionId);
    return filter;
  }

  /**
   * Creates a filter for a specific user.
   */
  public static ProtocolFilter forUser(String handle) {
    ProtocolFilter filter = new ProtocolFilter();
    filter.addUserHandle(handle);
    return filter;
  }

  public void addMnemonic(String mnemonic) {
    mnemonics.add(mnemonic);
  }

  public void addSessionId(String sessionId) {
    sessionIds.add(sessionId);
  }

  public void addUserHandle(String handle) {
    userHandles.add(handle.toUpperCase());
  }

  public void addStateName(String stateName) {
    stateNames.add(stateName);
  }

  public boolean isUnknownOnly() {
    return unknownOnly;
  }

  public void setUnknownOnly(boolean unknownOnly) {
    this.unknownOnly = unknownOnly;
  }

  public boolean isInboundOnly() {
    return inboundOnly;
  }

  public void setInboundOnly(boolean inboundOnly) {
    this.inboundOnly = inboundOnly;
    if (inboundOnly) {
      this.outboundOnly = false;
    }
  }

  public boolean isOutboundOnly() {
    return outboundOnly;
  }

  public void setOutboundOnly(boolean outboundOnly) {
    this.outboundOnly = outboundOnly;
    if (outboundOnly) {
      this.inboundOnly = false;
    }
  }

  /**
   * Check if a capture record matches this filter.
   */
  public boolean matches(CaptureRecord record) {
    // Check direction filters
    if (inboundOnly && record.getDirection() != CaptureRecord.Direction.INBOUND) {
      return false;
    }
    if (outboundOnly && record.getDirection() != CaptureRecord.Direction.OUTBOUND) {
      return false;
    }

    // Check unknown filter
    if (unknownOnly && !record.isUnknown()) {
      return false;
    }

    // Check mnemonic filter
    if (!mnemonics.isEmpty() && !mnemonics.contains(record.getMnemonic())) {
      return false;
    }

    // Check session filter
    if (!sessionIds.isEmpty() && !sessionIds.contains(record.getSessionId())) {
      return false;
    }

    // Check user filter
    if (!userHandles.isEmpty()) {
      String handle = record.getUserHandle();
      if (handle == null || !userHandles.contains(handle.toUpperCase())) {
        return false;
      }
    }

    // Check state filter
    if (!stateNames.isEmpty() && !stateNames.contains(record.getStateName())) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ProtocolFilter[");
    if (unknownOnly) sb.append("unknownOnly ");
    if (inboundOnly) sb.append("inboundOnly ");
    if (outboundOnly) sb.append("outboundOnly ");
    if (!mnemonics.isEmpty()) sb.append("mnemonics=").append(mnemonics).append(" ");
    if (!sessionIds.isEmpty()) sb.append("sessions=").append(sessionIds).append(" ");
    if (!userHandles.isEmpty()) sb.append("users=").append(userHandles).append(" ");
    if (!stateNames.isEmpty()) sb.append("states=").append(stateNames);
    return sb.toString().trim() + "]";
  }
}
