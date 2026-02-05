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
package org.jbrain.qlink.cmd.action;

import org.jbrain.qlink.cmd.CRCException;

/**
 * Admin command for protocol analysis operations.
 * Mnemonic: PA (Protocol Analysis)
 */
public class ProtocolCommand extends AbstractAction {

  public static final String MNEMONIC = "PA";
  private String _command;
  private String _parameter;

  public ProtocolCommand(byte[] data, int start, int len) throws CRCException {
    super(data, start, len);
    if (len > 10) {
      _command = getString(data, start + 10, 10).trim();
      if (len > 20) {
        _parameter = getString(data, start + 20, 30).trim();
      }
    }
  }

  public ProtocolCommand(String command, String parameter) {
    super(MNEMONIC);
    _command = command;
    _parameter = parameter;
  }

  public String getData() {
    return _command;
  }

  public String getCommandName() {
    return _command;
  }

  public String getParameter() {
    return _parameter;
  }
}