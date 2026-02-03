/*
Copyright Jim Brain and Brain Innovations, 2005.
Copyright 2024, Modernization Phase 1 Contributors.

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
@contributor Modernization Phase 1 Contributors
Created on 2024

*/
package org.jbrain.qlink.util;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;

/**
 * Security utility class providing input validation and sanitization
 * for the Q-Link Reloaded server to prevent common security vulnerabilities.
 */
public class SecurityUtils {

    private static final Logger _log = LogManager.getLogger(SecurityUtils.class);

    // Maximum allowed lengths for various inputs
    private static final int MAX_HANDLE_LENGTH = 20;
    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_ROOM_NAME_LENGTH = 50;
    private static final int MAX_EMAIL_LENGTH = 100;

    // Pattern for valid Q-Link handles (alphanumeric with underscores)
    private static final Pattern HANDLE_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,20}$");

    // Pattern for valid room names (alphanumeric with spaces and safe punctuation)
    private static final Pattern ROOM_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9 .,!&?()-]{1,50}$");

    // Pattern for SQL injection detection
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i).*((\\bselect\\b|\\binsert\\b|\\bupdate\\b|\\bdelete\\b|\\bdrop\\b|\\bcreate\\b|\\balter\\b|\\bexec\\b|\\bexecute\\b)|" +
        "(\\bunion\\b.*\\bselect\\b)|(\\bdeclare\\b.*\\b@\\w+\\b)|(\\bset\\b.*@\\w+\\b)).*"
    );

    // Pattern for XSS detection
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i).*((<script[^>]*>)|(</script>)|(javascript:)|(onload=)|(onerror=)|(onmouseover=)).*"
    );

    /**
     * Validates a Q-Link handle
     * @param handle The handle to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidHandle(String handle) {
        if (handle == null || handle.isEmpty()) {
            _log.warn("Invalid handle: null or empty");
            return false;
        }

        if (handle.length() > MAX_HANDLE_LENGTH) {
            _log.warn("Invalid handle: too long (" + handle.length() + " > " + MAX_HANDLE_LENGTH + ")");
            return false;
        }

        if (!HANDLE_PATTERN.matcher(handle).matches()) {
            _log.warn("Invalid handle: contains invalid characters: " + handle);
            return false;
        }

        return true;
    }

    /**
     * Validates and sanitizes a chat message
     * @param message The message to validate
     * @return sanitized message or null if invalid
     */
    public static String sanitizeMessage(String message) {
        if (message == null) {
            _log.warn("Invalid message: null");
            return null;
        }

        // Truncate if too long
        if (message.length() > MAX_MESSAGE_LENGTH) {
            _log.warn("Message truncated: too long (" + message.length() + " > " + MAX_MESSAGE_LENGTH + ")");
            message = message.substring(0, MAX_MESSAGE_LENGTH);
        }

        // Check for SQL injection attempts
        if (SQL_INJECTION_PATTERN.matcher(message).matches()) {
            _log.warn("Potential SQL injection detected in message: " + message);
            return null;
        }

        // Check for XSS attempts
        if (XSS_PATTERN.matcher(message).matches()) {
            _log.warn("Potential XSS detected in message: " + message);
            return null;
        }

        // Escape HTML characters
        return StringEscapeUtils.escapeHtml4(message);
    }

    /**
     * Validates a room name
     * @param roomName The room name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidRoomName(String roomName) {
        if (roomName == null || roomName.isEmpty()) {
            _log.warn("Invalid room name: null or empty");
            return false;
        }

        if (roomName.length() > MAX_ROOM_NAME_LENGTH) {
            _log.warn("Invalid room name: too long (" + roomName.length() + " > " + MAX_ROOM_NAME_LENGTH + ")");
            return false;
        }

        if (!ROOM_NAME_PATTERN.matcher(roomName).matches()) {
            _log.warn("Invalid room name: contains invalid characters: " + roomName);
            return false;
        }

        return true;
    }

    /**
     * Validates an email address
     * @param email The email to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            _log.warn("Invalid email: null or empty");
            return false;
        }

        if (email.length() > MAX_EMAIL_LENGTH) {
            _log.warn("Invalid email: too long (" + email.length() + " > " + MAX_EMAIL_LENGTH + ")");
            return false;
        }

        EmailValidator validator = EmailValidator.getInstance();
        return validator.isValid(email);
    }

    /**
     * Validates a numeric ID parameter
     * @param id The ID to validate
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @return true if valid, false otherwise
     */
    public static boolean isValidNumericId(String id, int min, int max) {
        if (id == null || id.isEmpty()) {
            _log.warn("Invalid numeric ID: null or empty");
            return false;
        }

        try {
            int numericId = Integer.parseInt(id);
            if (numericId < min || numericId > max) {
                _log.warn("Invalid numeric ID: out of range (" + numericId + " not in [" + min + ", " + max + "])");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            _log.warn("Invalid numeric ID: not a number - " + id);
            return false;
        }
    }

    /**
     * Generates a secure random string for session tokens
     * @param length Length of the generated string
     * @return Random string
     */
    public static String generateSecureToken(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }

        return sb.toString();
    }

    /**
     * Sanitizes file paths to prevent directory traversal attacks
     * @param filename The filename to sanitize
     * @return sanitized filename or null if unsafe
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            _log.warn("Invalid filename: null or empty");
            return null;
        }

        // Check for directory traversal patterns
        if (filename.contains("../") || filename.contains("./") ||
            filename.contains("..\\") || filename.contains(".\\")) {
            _log.warn("Directory traversal attempt detected in filename: " + filename);
            return null;
        }

        // Check for absolute path patterns
        if (filename.startsWith("/") || filename.startsWith("\\") ||
            filename.contains(":/") || filename.contains(":\\") ||
            filename.startsWith("~")) {
            _log.warn("Absolute path detected in filename: " + filename);
            return null;
        }

        // Remove path separators and normalize
        String sanitized = filename.replace("\\", "/")
            .replaceAll("/+", "/");

        // Check for any remaining dangerous patterns
        if (sanitized.contains("..") || sanitized.contains("~/") ||
            sanitized.startsWith("/") || sanitized.contains("//")) {
            _log.warn("Unsafe filename detected: " + filename);
            return null;
        }

        return sanitized;
    }
}