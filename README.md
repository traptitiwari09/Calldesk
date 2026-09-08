# CallDesk

A web-based click-to-call dialer that places **real phone calls** through Twilio.

Click a name in the browser — your own phone rings, you answer, and you're connected
to the other person. Neither party sees the other's number; both see the Twilio
number instead. Every call is logged automatically with its status and duration.

The conversation happens on **real phones over the carrier network**, not in the
browser. Your laptop can be closed mid-call.

---

## Why this exists

It's the mechanism behind "call your driver" in Uber, or the callback button in a
support tool. Three problems it solves:

| Problem | How |
|---|---|
| **Privacy** — give a stranger your number and they have it forever | Both parties see a rented Twilio number, never each other's |
| **Manual dialing** — typing 10 digits hundreds of times, misdials | Click a name |
| **No records** — "did anyone call that customer back?" | Every call logged automatically with duration and outcome |

The third one is why businesses pay for this.

---

## How it works

The part worth understanding: **Twilio calls your server back mid-call.**

```
1. Browser    ──POST /api/calls──▶   App
2. App        ──REST API──────────▶  Twilio
3. Twilio     ──rings────────────▶   YOUR phone
4. You answer
5. Twilio     ──POST /twilio/voice/{id}──▶  App     ◀── Twilio asking "now what?"
6. App        ──TwiML XML──────────────▶    Twilio
                <Dial><Number>+91…</Number></Dial>
7. Twilio     ──rings────────────▶   THEIR phone, bridges the two
8. Call ends
9. Twilio     ──POST /twilio/status/{id}──▶ App     ◀── duration + outcome
```

Steps 5 and 9 are Twilio making HTTP requests **to** this app — which is why it needs
a public URL (ngrok / VS Code dev tunnel) during development, and why every webhook
is verified against Twilio's request signature.

A single call row fills itself in across four asynchronous callbacks:

```
queued → initiated → ringing → in-progress → completed (47s)
```

### Inbound and callback

Calls **to** the Twilio number hit `/twilio/incoming`, which logs the caller and
answers with "we'll call you back". They land in a **shared queue** any signed-in
agent can claim with one click — modelled on call-centre ACD.

Callers are matched against saved contacts automatically, but a contact is **not**
required: a call is an event involving a phone *number*, and forcing every unknown
caller to be saved first doesn't scale.

---

## Stack

- **Java 17**, **Spring Boot 4**
- **Spring Data JPA** + **H2** (file-based; swap the datasource URL for MySQL)
- **Twilio Java SDK 12** — Voice API and TwiML
- **BCrypt** password hashing, `HttpSession` auth
- Plain **HTML / CSS / JavaScript** — no frontend framework

---

## Running it

### 1. Twilio setup

- A Twilio account and a **voice-capable** phone number
- **Geo Permissions** enabled for the countries you're calling
  (Console → Voice → Settings → Geo Permissions — calls fail with error `21215` otherwise)
- On a trial account, destination numbers must be **verified caller IDs**

### 2. A public URL

Twilio must be able to reach your machine. Any tunnel works:

```bash
ngrok http 8080
# or use VS Code's built-in port forwarding (set visibility to Public)
```

### 3. Configuration

Copy the example file and fill it in:

```bash
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
```

That file is **gitignored** — credentials never enter the repository.

### 4. Run

```bash
./mvnw spring-boot:run
```

Then open <http://localhost:8080/signup.html>. Sign up with **your own phone number** —
that's the phone that rings first when you place a call.

### 5. Optional — receive inbound calls

Twilio Console → your number → **"A call comes in"**:

```
https://<your-tunnel>/twilio/incoming     (HTTP POST)
```

> ⚠️ From that moment, every call to that number reaches your machine. Only do this
> on a number that is yours to configure.

---

## API

```
POST   /api/auth/signup     {email, password, name, phoneNumber}
POST   /api/auth/login      {email, password}
POST   /api/auth/logout
GET    /api/auth/me                       401 when signed out

GET    /api/contacts
POST   /api/contacts        {name, phoneNumber}
DELETE /api/contacts/{id}

POST   /api/calls           {contactId} | {phoneNumber} | {callbackForCallId}
GET    /api/calls                         history, newest first
GET    /api/calls/inbound                 the shared callback queue

POST   /twilio/incoming                   webhook: someone called us
POST   /twilio/voice/{id}                 webhook: returns TwiML to bridge the legs
POST   /twilio/status/{id}                webhook: status + duration
```

Errors always return `{"error": "..."}`, so the frontend needs one handler.

---

## Security

- **Webhook signature validation** — every `/twilio/**` request is verified against
  Twilio's HMAC-SHA1 signature. The endpoints are public, so without this anyone who
  discovered the URL could make the app dial arbitrary numbers.
- **BCrypt** password hashing; login errors are identical for unknown-email and
  wrong-password so they can't be used to enumerate accounts.
- Session is regenerated on login (session fixation).
- Contacts are scoped per user; every lookup is by `id AND userId`.
- Credentials live only in the gitignored local properties file.

---

## Not built

Honest scope, for anyone reading this as a portfolio piece:

- **IVR** ("press 1 for sales") and skills-based routing
- **Call recording** and transcription
- **Disposition** — the outcome an agent picks after hanging up
  (interested / wrong number / callback). This is the highest-value missing field:
  it's what every call-centre report is built on, and Twilio can never supply it
- SLA timers, concurrency limits
- Browser-based calling (Twilio Voice JS SDK) — deliberately excluded; the whole
  point here is that the conversation happens on a real phone
