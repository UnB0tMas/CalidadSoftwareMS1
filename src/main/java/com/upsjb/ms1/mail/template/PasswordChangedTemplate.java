package com.upsjb.ms1.mail.template;

import com.upsjb.ms1.mail.model.EmailTemplateData;
import org.springframework.stereotype.Component;

@Component
public class PasswordChangedTemplate {

    public static final String TEMPLATE_NAME = "PASSWORD_CHANGED";

    public String subject(EmailTemplateData data) {
        String appName = data.getStringOrDefault("appName", "MS Seguridad Usuarios");
        return "Tu contraseña fue cambiada - " + appName;
    }

    public String renderHtml(EmailTemplateData data) {
        String appName = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("appName", "MS Seguridad Usuarios"));
        String nombreUsuario = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("nombreUsuario", "usuario"));
        String changedAt = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("changedAt", ""));
        String ipAddress = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("ipAddress", "No disponible"));
        String userAgent = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("userAgent", "No disponible"));
        String frontendUrl = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("frontendUrl", ""));

        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <title>Contraseña cambiada</title>
                </head>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table width="620" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e5e7eb;">
                          <tr>
                            <td style="padding:28px 32px;background:#065f46;color:#ffffff;">
                              <h1 style="margin:0;font-size:22px;">%s</h1>
                              <p style="margin:8px 0 0;font-size:14px;color:#d1fae5;">Confirmación de seguridad</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="font-size:16px;margin:0 0 16px;">Hola, <strong>%s</strong>.</p>
                              <p style="font-size:15px;line-height:1.6;margin:0 0 20px;">
                                Te confirmamos que la contraseña de tu cuenta fue cambiada correctamente.
                              </p>
                              <p style="font-size:14px;line-height:1.6;margin:0;">
                                Fecha: <strong>%s</strong><br>
                                IP: <strong>%s</strong><br>
                                Dispositivo: <strong>%s</strong>
                              </p>
                              <p style="font-size:13px;line-height:1.6;margin:24px 0 0;color:#6b7280;">
                                Si no realizaste esta acción, ingresa al sistema y solicita recuperación de contraseña:
                                <br>%s
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(appName, nombreUsuario, changedAt, ipAddress, userAgent, frontendUrl);
    }

    public String renderText(EmailTemplateData data) {
        return """
                Tu contraseña fue cambiada

                Hola, %s.

                La contraseña de tu cuenta fue cambiada correctamente.

                Fecha: %s
                IP: %s
                Dispositivo: %s

                Si no realizaste esta acción, solicita recuperación de contraseña desde:
                %s
                """.formatted(
                data.getStringOrDefault("nombreUsuario", "usuario"),
                data.getStringOrDefault("changedAt", ""),
                data.getStringOrDefault("ipAddress", "No disponible"),
                data.getStringOrDefault("userAgent", "No disponible"),
                data.getStringOrDefault("frontendUrl", "")
        );
    }
}