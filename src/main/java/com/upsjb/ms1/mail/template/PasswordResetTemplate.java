package com.upsjb.ms1.mail.template;

import com.upsjb.ms1.mail.model.EmailTemplateData;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTemplate {

    public static final String TEMPLATE_NAME = "PASSWORD_RESET";

    public String subject(EmailTemplateData data) {
        String appName = data.getStringOrDefault("appName", "MS Seguridad Usuarios");
        return "Recuperación de contraseña - " + appName;
    }

    public String renderHtml(EmailTemplateData data) {
        String appName = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("appName", "MS Seguridad Usuarios"));
        String nombreUsuario = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("nombreUsuario", "usuario"));
        String tokenOrCode = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("tokenOrCode", ""));
        String resetUrl = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("resetUrl", ""));
        String expiresAt = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("expiresAt", ""));
        String ipAddress = EmailTemplateRenderer.escapeHtml(data.getStringOrDefault("ipAddress", "No disponible"));

        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <title>Recuperación de contraseña</title>
                </head>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8;padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table width="620" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e5e7eb;">
                          <tr>
                            <td style="padding:28px 32px;background:#1e3a8a;color:#ffffff;">
                              <h1 style="margin:0;font-size:22px;">%s</h1>
                              <p style="margin:8px 0 0;font-size:14px;color:#bfdbfe;">Recuperación de contraseña</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="font-size:16px;margin:0 0 16px;">Hola, <strong>%s</strong>.</p>
                              <p style="font-size:15px;line-height:1.6;margin:0 0 20px;">
                                Recibimos una solicitud para recuperar tu contraseña.
                              </p>
                              <div style="font-size:22px;font-weight:bold;text-align:center;padding:18px;border-radius:12px;background:#f9fafb;border:1px solid #e5e7eb;color:#111827;">
                                %s
                              </div>
                              <p style="font-size:14px;line-height:1.6;margin:20px 0 0;">
                                También puedes continuar desde: <br><strong>%s</strong>
                              </p>
                              <p style="font-size:14px;line-height:1.6;margin:20px 0 0;">
                                Vence en: <strong>%s</strong><br>
                                IP origen: <strong>%s</strong>
                              </p>
                              <p style="font-size:13px;line-height:1.6;margin:24px 0 0;color:#6b7280;">
                                Si no solicitaste esta recuperación, ignora este correo.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(appName, nombreUsuario, tokenOrCode, resetUrl, expiresAt, ipAddress);
    }

    public String renderText(EmailTemplateData data) {
        return """
                Recuperación de contraseña

                Hola, %s.

                Usa este token o código para recuperar tu contraseña:
                %s

                URL:
                %s

                Vence en: %s
                IP origen: %s

                Si no solicitaste esta recuperación, ignora este correo.
                """.formatted(
                data.getStringOrDefault("nombreUsuario", "usuario"),
                data.getStringOrDefault("tokenOrCode", ""),
                data.getStringOrDefault("resetUrl", ""),
                data.getStringOrDefault("expiresAt", ""),
                data.getStringOrDefault("ipAddress", "No disponible")
        );
    }
}