package com.upsjb.ms1.mail.template;

import com.upsjb.ms1.mail.model.EmailTemplateData;
import org.springframework.stereotype.Component;

@Component
public class SuspiciousLoginTemplate {

    public static final String TEMPLATE_NAME = "SUSPICIOUS_LOGIN";

    public String subject(EmailTemplateData data) {
        String appName = data.getStringOrDefault("appName", "MS Seguridad Usuarios");
        return "Alerta de inicio de sesión sospechoso - " + appName;
    }

    public String renderHtml(EmailTemplateData data) {
        String appName = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("appName", "MS Seguridad Usuarios"));
        String nombreUsuario = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("nombreUsuario", "usuario"));
        String loginAt = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("loginAt", ""));
        String ipAddress = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("ipAddress", "No disponible"));
        String userAgent = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("userAgent", "No disponible"));
        String frontendUrl = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("frontendUrl", ""));

        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <title>Alerta de seguridad</title>
                </head>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table width="620" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e5e7eb;">
                          <tr>
                            <td style="padding:28px 32px;background:#92400e;color:#ffffff;">
                              <h1 style="margin:0;font-size:22px;">%s</h1>
                              <p style="margin:8px 0 0;font-size:14px;color:#fde68a;">Alerta de inicio de sesión</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="font-size:16px;margin:0 0 16px;">Hola, <strong>%s</strong>.</p>
                              <p style="font-size:15px;line-height:1.6;margin:0 0 20px;">
                                Detectamos un inicio de sesión que podría requerir tu revisión.
                              </p>
                              <p style="font-size:14px;line-height:1.6;margin:0;">
                                Fecha: <strong>%s</strong><br>
                                IP: <strong>%s</strong><br>
                                Dispositivo: <strong>%s</strong>
                              </p>
                              <p style="font-size:13px;line-height:1.6;margin:24px 0 0;color:#6b7280;">
                                Si reconoces esta actividad, no necesitas hacer nada. Si no la reconoces,
                                cambia tu contraseña desde: <br>%s
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(appName, nombreUsuario, loginAt, ipAddress, userAgent, frontendUrl);
    }

    public String renderText(EmailTemplateData data) {
        return """
                Alerta de inicio de sesión sospechoso

                Hola, %s.

                Detectamos un inicio de sesión que podría requerir revisión.

                Fecha: %s
                IP: %s
                Dispositivo: %s

                Si no reconoces esta actividad, cambia tu contraseña desde:
                %s
                """.formatted(
                data.getStringOrDefault("nombreUsuario", "usuario"),
                data.getStringOrDefault("loginAt", ""),
                data.getStringOrDefault("ipAddress", "No disponible"),
                data.getStringOrDefault("userAgent", "No disponible"),
                data.getStringOrDefault("frontendUrl", "")
        );
    }
}