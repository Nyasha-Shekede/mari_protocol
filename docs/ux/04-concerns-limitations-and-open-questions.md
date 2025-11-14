# Concerns, Limitations & Open Questions (UX)

## Scope

- **Audience**
  - UX designers, product, user research.
- **Goal**
  - Capture key user-facing concerns and known limitations.
  - Provide a starting point for UX research and iteration.

## Key User Concerns

- **Privacy & Permissions**
  - Why does Mari ask for:
    - Location access (grid)?
    - Motion/accelerometer access (seal)?
  - UX responses:
    - Explain that data is used to protect against bots and fraud.
    - Clarify that only coarse grids and derived seals are stored, not raw GPS streams.

- **Trust in Settlement**
  - Users want to know:
    - "Did my payment really arrive?"
    - "Who holds my money?"
  - UX responses:
    - Provide clear settled/failed states.
    - Offer access to transaction history.
    - Explain in simple language that a bank-like service updates balances and issues proofs.

- **Offline / SMS Behavior**
  - Concerns:
    - SMS delays or failures.
    - Confusion when confirmation is not immediate.
  - UX responses:
    - Show that payment was sent via SMS and is awaiting confirmation.
    - Provide a clear way to check later whether it succeeded.

- **Error Messages**
  - Internal errors can be technical (risk rejection, physics mismatch, sentinel unavailable).
  - UX risks:
    - Confusing or alarming error messages.
  - UX responses:
    - Map internal error codes to concise, human messages and suggested actions.

## Product Limitations (Current State)

- **Platform Coverage**
  - Focused on Android + specific backend stack.
  - Limited or no support yet for:
    - iOS.
    - Web-only clients.

- **Integration with Existing POS Systems**
  - No out-of-the-box POS integrations.
  - Merchants may need separate UX flows or hardware.

- **AI/Autonomous Payments**
  - Current product assumes human-driven app interaction.
  - No built-in UX for agents or automated spend today.

- **Internationalization & Localization**
  - Baseline language and locale support only.
  - Further work needed for:
    - Full translations.
    - Right-to-left layouts.
    - Localization of amounts, dates, and messages.

## UX Open Questions

- **How to Surface Risk Information**
  - Do we show users when a payment was considered high-risk but allowed?
  - How to avoid creating fear while being transparent?

- **How Much to Explain Physics**
  - Do users need to understand motion/location at all?
  - Where is the right place for deeper explanations (help center vs main screens)?

- **What Happens on Long Delays**
  - If SMS or backend is slow:
    - How long before app shows "we're not sure" and suggests next steps?

- **User Control Over Data**
  - How can users:
    - See what data has been collected about them?
    - Delete or export their data (subject to regulatory constraints)?

## Guidance for UX Research

- **Topics to Explore with Users**
  - Comfort level with motion/location being used for security.
  - Reactions to offline/SMS behavior and delays.
  - Preferences for explanations of risk and settlement.

- **Metrics to Watch**
  - Payment completion rate.
  - Drop-offs at permission prompts.
  - Frequency of support contacts about "where is my money?".

- **Iteration Opportunities**
  - Simplify flows where users struggle.
  - Improve help and onboarding content.
  - Adjust language as understanding of user mental models improves.
