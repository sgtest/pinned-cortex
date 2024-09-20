package com.cortex.backend.common.email;

import java.util.Map;

public interface EmailService {
  void sendEmail(String to, EmailTemplateName emailTemplate, Map<String, Object> templateVariables, String subject);
}