# Customer Support Playbook

## Scope

- **Audience**
  - Customer support, frontline agents, CS operations.
- **Goal**
  - Explain how to reason about Mari payments when users call in.
  - Provide standard patterns for answering common questions.

## Concepts in Simple Terms

- **Who holds the money?**
  - A bank-like service holds user balances.
  - Mari is the system that sends instructions and checks for fraud.

- **What is a payment in Mari?**
  - A secure digital message ("coupon") sent from the user’s phone to the bank.
  - It includes who to pay, how much, and some security information.

- **What is settlement?**
  - The moment when the bank updates balances and confirms the payment.
  - Users see this as a successful payment with a receipt.

## Typical User Questions & How to Approach Them

### 1. "Where is my money?"

- **Possible causes**
  - Payment failed before settlement (risk or validation issues).
  - Network/SMS delay.
  - Bank/HSM temporarily unavailable.

- **Support steps**
  - Ask for:
    - Time of payment.
    - Sender and receiver identifiers as shown in app.
  - Check:
    - App’s transaction history screen (if accessible).
    - Core transaction status via internal tools.
    - Whether settlement succeeded (bank side) for that transaction.

- **User-friendly explanations**
  - If failed:
    - "This payment did not complete. Your account was not debited. You can try again."  
  - If pending due to delay:
    - "Your payment is still being processed because the network is slow. Please check again shortly."  

### 2. "Why was my payment declined?"

- **Common technical reasons**
  - Security checks failed (risk engine).
  - The payment expired.
  - The bank could not approve it (e.g. not enough balance).

- **Support guidance**
  - Avoid technical jargon.
  - Example explanations:
    - Security / risk decline:
      - "Our security checks couldn’t safely approve this payment. This is to protect you from fraud. If you believe this is a mistake, we can review it."  
    - Expired:
      - "The payment took too long to complete and expired. Please try again."  
    - Insufficient funds:
      - "It looks like there wasn’t enough balance to complete this payment."  

### 3. "Why does the app need my location/motion?"

- **Key points**
  - Explain simply:
    - "We use approximate location and motion to help detect bots and fraud."
    - "We do not store your exact GPS coordinates, only a coarse area."

- **Reassurance**
  - "This information is used to protect your account, not to track you for advertising."

### 4. "Payment sent via SMS but no confirmation"

- **Possible causes**
  - SMS delayed or never delivered.
  - Backend slow or temporarily down.

- **Support steps**
  - Confirm whether a corresponding transaction exists in backend.
  - If not:
    - Treat as "not sent" from system perspective.
  - If yes but not settled:
    - Explain that processing is still ongoing or failed.

- **User-facing messaging**
  - "We haven’t received this payment yet. If you still see it as pending after some time, please try again or contact support with the time and recipient details."  

## When to Escalate

- **Escalate to Risk/Ops when:**
  - Many users in the same area report similar declines or delays.
  - There are signs of coordinated fraud or unusual behavior.

- **Escalate to Engineering/Ops when:**
  - Multiple users report outages or severe slowdowns.
  - Health pages or internal dashboards show service issues.

## Tone & Communication Guidelines

- **Be clear and calm**
  - Avoid blaming the user.
  - Avoid over-promising (e.g. do not guarantee recovery of funds without confirmation).

- **Protect security details**
  - Do not share internal risk thresholds or model details.
  - Do not speculate about fraud beyond what data shows.

- **Empathize**
  - Recognize that payment issues are stressful.
  - Offer clear next steps and timelines where possible.

## Data You Might Use (via Internal Tools)

- Transaction status and timestamps.
- Whether settlement has completed.
- Basic reason for failure (mapped to support-friendly wording).

## Coordination with Other Teams

- Work with Risk Ops to:
  - Keep an up-to-date list of common decline reasons and suggested responses.

- Work with Product/UX to:
  - Improve in-app messages where users frequently contact support.

- Work with Legal/Compliance to:
  - Ensure explanations are accurate and within policy.
