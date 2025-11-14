// src/services/twilioClient.js
// Thin Twilio SMS client. Enabled when TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN are set.
// Uses Messaging Service SID if provided; otherwise uses FROM number.

let client = null;

function getClient() {
  const sid = process.env.TWILIO_ACCOUNT_SID;
  const token = process.env.TWILIO_AUTH_TOKEN;
  if (!sid || !token) return null;
  if (!client) {
    // Lazy require to avoid bundling in tests
    const twilio = require('twilio');
    client = twilio(sid, token);
  }
  return client;
}

async function sendSms({ to, body }) {
  const c = getClient();
  if (!c) return false;
  const svc = process.env.TWILIO_MESSAGING_SERVICE_SID || '';
  const from = process.env.TWILIO_FROM_NUMBER || '';
  const payload = svc ? { messagingServiceSid: svc } : { from };
  try {
    await c.messages.create({ to, body, ...payload });
    return true;
  } catch (e) {
    console.warn('[Twilio][sendSms] failed:', e?.message || e);
    return false;
  }
}

module.exports = { sendSms };
