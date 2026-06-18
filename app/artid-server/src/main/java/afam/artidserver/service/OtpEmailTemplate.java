package afam.artidserver.service;

import org.springframework.web.util.HtmlUtils;

/**
 * Corpo HTML (email-safe: layout a tabelle e CSS inline) delle email con codice OTP.
 * Condiviso tra login e registrazione: variano solo il titolo e la frase introduttiva.
 * Colori e font allineati alla palette brand ArtID (vedi style.scss del client).
 * Il nome del destinatario è user input, quindi viene sempre HTML-escapato.
 */
final class OtpEmailTemplate {

    private OtpEmailTemplate() {
    }

    // Stack font del brand: Titillium Web dove supportato (Apple Mail, client desktop con il
    // font installato), con fallback di sistema per Gmail/Outlook che ignorano i web font.
    private static final String FONT_STACK =
            "'Titillium Web', system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif";

    private static final String TEMPLATE = ("""
            <!DOCTYPE html>
            <html lang="it">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <meta name="color-scheme" content="light only">
            <meta name="supported-color-schemes" content="light only">
            <title>ArtID</title>
            <style>@import url('https://fonts.googleapis.com/css2?family=Titillium+Web:wght@400;600;700&display=swap');</style>
            </head>
            <body style="margin:0; padding:0; background-color:#f5f5f5; -webkit-text-size-adjust:100%;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f5f5f5;">
                <tr>
                  <td align="center" style="padding:32px 16px;">
                    <table role="presentation" width="460" cellpadding="0" cellspacing="0" border="0" style="width:460px; max-width:460px; background-color:#ffffff; border:1px solid #e2e9f2; border-radius:16px; overflow:hidden; font-family:{font};">
                      <tr>
                        <td style="padding:20px 28px; border-bottom:1px solid #e7eef8;">
                          <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                            <tr>
                              <td width="32" height="32" align="center" valign="middle" style="width:32px; height:32px; background-color:#0066cc; border-radius:9px; color:#ffffff; font-size:17px; font-weight:bold; font-family:{font};">A</td>
                              <td style="padding-left:10px; vertical-align:middle; font-size:17px; font-weight:bold; color:#294766; letter-spacing:0.3px; font-family:{font};">ArtID</td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:28px; font-family:{font};">
                          <h1 style="margin:0 0 8px; font-size:20px; font-weight:bold; color:#17324d;">{heading}</h1>
                          <p style="margin:0; font-size:15px; line-height:1.6; color:#51677e;">Ciao {name}, {intro}</p>
                          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:22px 0;">
                            <tr>
                              <td align="center" style="background-color:#dce9f5; border-radius:12px; padding:24px 16px;">
                                <div style="font-family:'SFMono-Regular',Consolas,'Liberation Mono',Menlo,Courier,monospace; font-size:33px; font-weight:bold; letter-spacing:10px; color:#17324d; padding-left:10px;">{code}</div>
                                <div style="margin-top:12px; font-size:13px; color:#294766;">Valido fino alle {expiry}</div>
                              </td>
                            </tr>
                          </table>
                          <p style="margin:0; font-size:13px; line-height:1.6; color:#51677e;">Non condividere questo codice con nessuno. Se non hai richiesto l'accesso, puoi ignorare questa email in tutta sicurezza.</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:20px 28px; background-color:#f5f8fc; border-top:1px solid #e7eef8; font-family:{font};">
                          <p style="margin:0 0 6px; font-size:12px; line-height:1.55; color:#51677e;">Questo è un messaggio automatico: la casella noreply@artid.space non è monitorata. Per assistenza scrivi a <a href="mailto:supporto@artid.space" style="color:#0066cc; text-decoration:none;">supporto@artid.space</a>.</p>
                          <p style="margin:0; font-size:12px; line-height:1.55; color:#51677e;"><strong style="color:#294766;">ArtID</strong> &mdash; la tua identità artistica, in un unico posto.<br>artid.space &middot; trattamento dati conforme al GDPR.</p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """).replace("{font}", FONT_STACK);

    /**
     * Compone il corpo HTML dell'email OTP.
     *
     * @param name    nome del destinatario (user input, viene escapato); se vuoto si usa "Membro"
     * @param code    codice OTP da mostrare in evidenza
     * @param expiry  orario di scadenza già formattato (es. "14:35")
     * @param heading titolo del messaggio (es. "Il tuo codice di accesso")
     * @param intro   frase introduttiva dopo "Ciao {nome}, " (es. "usa questo codice per...")
     */
    static String render(String name, String code, String expiry, String heading, String intro) {
        String safeName = HtmlUtils.htmlEscape((name == null || name.isBlank()) ? "Membro" : name);
        return TEMPLATE
                .replace("{heading}", heading)
                .replace("{intro}", intro)
                .replace("{name}", safeName)
                .replace("{code}", code)
                .replace("{expiry}", expiry);
    }
}
