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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jbrain.qlink.QConfig;
import org.jbrain.qlink.QSession;
import org.jbrain.qlink.cmd.action.Action;
import org.jbrain.qlink.cmd.action.UnknownAction;
import org.jbrain.qlink.cmd.action.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Experimental response framework for testing unknown protocol actions.
 * Allows crafting and sending custom responses to understand expected behavior.
 */
public class ResponseExperimenter {
  private static Logger _log = LogManager.getLogger(ResponseExperimenter.class);
  private static ResponseExperimenter _instance;

  private boolean _enabled;
  private Map<String, ResponseTemplate> _templates = new ConcurrentHashMap<>();
  private java.util.Random _random = new java.util.Random();

  private ResponseExperimenter() {
    _enabled = QConfig.getInstance().getBoolean("qlink.protocol.experimenter.enabled", true);
    initializeTemplates();
  }

  public static synchronized ResponseExperimenter getInstance() {
    if (_instance == null) {
      _instance = new ResponseExperimenter();
    }
    return _instance;
  }

  /**
   * Send a raw frame to a session.
   */
  public void sendRawFrame(QSession session, byte[] data) {
    if (!_enabled || session == null || data == null || data.length == 0) return;

    try {
      _log.info("Sending raw frame to " + session.getHandle() + ": " + ProtocolDecoder.hexDump(data));
      // TODO: Implement raw frame sending when session API supports it
      // session.getServer().sendToUser(session.getHandle(), data);
    } catch (Exception e) {
      _log.error("Failed to send raw frame", e);
    }
  }

  /**
   * Send an action to a session.
   */
  public void sendAction(QSession session, Action action) {
    if (!_enabled || session == null || action == null) return;

    try {
      _log.info("Sending action " + action.getName() + " to " + session.getHandle());
      session.send(action);
    } catch (Exception e) {
      _log.error("Failed to send action: " + action.getName(), e);
    }
  }

  /**
   * Register a response template.
   */
  public void registerTemplate(String name, ResponseTemplate template) {
    _templates.put(name, template);
    _log.info("Registered response template: " + name);
  }

  /**
   * Send a template-based response.
   */
  public void sendTemplate(QSession session, String templateName, Map<String, Object> params) {
    if (!_enabled || session == null || templateName == null) return;

    ResponseTemplate template = _templates.get(templateName);
    if (template == null) {
      _log.warn("Template not found: " + templateName);
      return;
    }

    try {
      Action action = template.createAction(params);
      sendAction(session, action);
      _log.info("Sent template response: " + templateName + " to " + session.getHandle());
    } catch (Exception e) {
      _log.error("Failed to send template response: " + templateName, e);
    }
  }

  /**
   * Send a generic error response to an unknown action.
   */
  public void sendGenericErrorResponse(QSession session, String mnemonic) {
    if (!_enabled || session == null || mnemonic == null) return;

    // Create a generic error action (we'll use an existing error action type)
    try {
      // For now, we can't easily create new action types, so we'll log the attempt
      _log.info("Would send generic error response for unknown mnemonic: " + mnemonic +
          " to user: " + session.getHandle());
    } catch (Exception e) {
      _log.error("Failed to send generic error response", e);
    }
  }

  /**
   * Send a test response that echoes back the original data.
   */
  public void sendEchoResponse(QSession session, UnknownAction originalAction) {
    if (!_enabled || session == null || originalAction == null) return;

    try {
      // Create a response that includes some of the original data
      // This is experimental - we don't know what the client expects
      _log.info("Sending echo response for " + originalAction.getName() +
          " to user: " + session.getHandle());

      // For testing, we could send a simple text message back
      String responseText = "Unknown command: " + originalAction.getName();
      sendSystemMessage(session, responseText);

      _log.info("Sending echo response for " + originalAction.getName() +
          " to user: " + session.getHandle());

    } catch (Exception e) {
      _log.error("Failed to send echo response", e);
    }
  }

  /**
   * Send a system message to a session.
   */
  private void sendSystemMessage(QSession session, String message) {
    try {
      session.sendSYSOLM(message);
      _log.debug("Sent system message to " + session.getHandle() + ": " + message);
    } catch (Exception e) {
      _log.error("Failed to send system message", e);
    }
  }

  /**
   * Send a random test response to see what happens.
   */
  public void sendRandomTestResponse(QSession session, String mnemonic) {
    if (!_enabled || session == null || mnemonic == null) return;

    try {
      // Generate some random test responses
      String[] testResponses = {
        "ACK",
        "ERROR",
        "UNKNOWN",
        "TEST",
        "RETRY",
        "IGNORE"
      };

      String response = testResponses[_random.nextInt(testResponses.length)];
      String message = "Testing response for " + mnemonic + ": " + response;

      sendSystemMessage(session, message);
      _log.info("Sent random test response: " + response + " for " + mnemonic);

    } catch (Exception e) {
      _log.error("Failed to send random test response", e);
    }
  }

  /**
   * Analyze an unknown action and suggest possible response strategies.
   */
  public String analyzeUnknownAction(UnknownAction action, QSession session) {
    if (action == null || session == null) return "Invalid input";

    StringBuilder analysis = new StringBuilder();
    analysis.append("Analysis for unknown action: ").append(action.getName()).append("\n");

    // Analyze the payload
    // TODO: Implement payload analysis when UnknownAction provides payload access
    // byte[] payload = action.getPayload();
    // if (payload != null && payload.length > 0) {
    //   analysis.append("Payload length: ").append(payload.length).append(" bytes\n");
    //   analysis.append("Payload hex: ").append(ProtocolDecoder.hexDump(payload)).append("\n");
    //   analysis.append("Payload ASCII: ").append(ProtocolDecoder.asciiDump(payload)).append("\n");
    //
    //   // Look for patterns
    //   if (payload.length == 2) {
    //     analysis.append("Likely 2-byte value - could be a numeric parameter\n");
    //   } else if (payload.length >= 4) {
    //     analysis.append("Multi-byte payload - could contain structured data\n");
    //   }
    //
    //   // Check for text
    //   String ascii = ProtocolDecoder.asciiDump(payload);
    //   if (ascii.matches(".*[A-Za-z].*")) {
    //     analysis.append("Contains text - possibly a command parameter or identifier\n");
    //   }
    // }

    // Suggest response strategies
    analysis.append("\nSuggested response strategies:\n");
    analysis.append("1. Send system message back to user\n");
    analysis.append("2. Log the action for manual analysis\n");
    analysis.append("3. Try sending a known response type (ACK, ERROR)\n");
    analysis.append("4. Capture more examples to identify patterns\n");

    return analysis.toString();
  }

  /**
   * Enable or disable the experimenter.
   */
  public void setEnabled(boolean enabled) {
    this._enabled = enabled;
    _log.info("Response experimenter " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Check if experimenter is enabled.
   */
  public boolean isEnabled() {
    return _enabled;
  }

  /**
   * Get list of available templates.
   */
  public String[] getAvailableTemplates() {
    return _templates.keySet().toArray(new String[0]);
  }

  /**
   * Initialize built-in response templates.
   */
  private void initializeTemplates() {
    // Template for generic error responses
    registerTemplate("error_response", new ResponseTemplate() {
      @Override
      public Action createAction(Map<String, Object> params) {
        // This would create an appropriate error response
        // For now, we'll just log it since we can't easily create new action types
        String message = (String) params.getOrDefault("message", "Unknown error");
        return null; // Placeholder
      }
    });

    // Template for acknowledgment responses
    registerTemplate("ack_response", new ResponseTemplate() {
      @Override
      public Action createAction(Map<String, Object> params) {
        // This would create an acknowledgment response
        return null; // Placeholder
      }
    });
  }

  /**
   * Interface for response templates.
   */
  public interface ResponseTemplate {
    Action createAction(Map<String, Object> params);
  }
}