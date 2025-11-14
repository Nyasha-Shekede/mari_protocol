# Comparisons & Positioning (UX View)

## Scope

- **Audience**
  - UX designers, product and go-to-market teams.
- **Goal**
  - Explain how Mari feels to users compared to other ways of paying.
  - Provide a basis for UX decisions and messaging.

## Comparison: Cash

- **User experience with cash**
  - Pros:
    - Immediate, tangible.
    - Universally understood.
    - No device or battery required.
  - Cons:
    - Easy to lose or steal.
    - No built-in record of transactions.

- **Mari vs Cash (UX)**
  - Pros for Mari:
    - Digital history of payments for disputes and budgeting.
    - Easier long-distance payments.
    - Lost device does not immediately equal lost balance (if bank-side account is protected).
  - Cons / Trade-offs:
    - Requires a phone, app, and some connectivity.
    - Requires user trust in digital systems and bank.

## Comparison: Card Payments

- **User experience with cards**
  - Flow:
    - Insert/tap card → PIN/signature → wait for approval.
  - Pain points:
    - POS hardware dependency.
    - Potentially confusing decline reasons.

- **Mari vs Cards (UX)**
  - Pros for Mari:
    - Can work where only phones and SMS exist (no POS terminal).
    - More visible link between user device and payment (phone as central object).
  - Cons / Trade-offs:
    - Today, less integrated with existing POS/hardware ecosystem.
    - Users may not recognize Mari brand like they recognize card networks.

## Comparison: Mobile Money / Wallet Apps

- **Typical mobile money UX**
  - USSD or app-based menus, often nested.
  - Simple send/receive flows but limited transparency on risk.

- **Mari vs Mobile Money (UX)**
  - Pros for Mari:
    - Explicit risk checks and richer context behind decisions.
    - Designed for deeper visibility into fraud patterns and settlement.
  - Cons / Trade-offs:
    - Some concepts (physics, risk scoring) need simplified messaging.
    - Users may need education on why motion/location permissions are requested.

## Positioning Themes for UX & Messaging

- **"Digital cash with a receipt"**
  - Emphasize:
    - Speed and simplicity like cash.
    - Verifiable trail like card/electronic payments.

- **"Works even when your data is flaky"**
  - Highlight:
    - SMS fallback.
    - Robustness in low-connectivity environments.

- **"Built to protect you from bots and scams"**
  - Without over-promising:
    - Explain that motion/location help detect suspicious behavior.
    - Reinforce that there are checks before money moves.

## UX Risks & How They Compare

- **Complexity vs Familiarity**
  - Risk:
    - Introducing new concepts (physics, risk scores) can confuse users.
  - Mitigation:
    - Keep core flows and language close to existing mental models ("Send money", "Payment received").

- **Error Visibility**
  - Compared to simple "failed" messages, Mari can expose more structure.
  - UX should:
    - Map internal errors to clear user messages.
    - Avoid leaking low-level technical detail.

- **Trust & Transparency**
  - Users may ask:
    - "Why do you need my location or motion?"
    - "Who holds my money?"
  - UX should provide:
    - Short, clear explanations in settings/help.
    - Easy access to transaction history and support.
