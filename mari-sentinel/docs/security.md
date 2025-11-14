# Security

This document outlines the core security controls available in Mari Sentinel and recommendations for hardening.

## API Authentication (Inference)
- The inference service supports a simple token check. If `SENTINEL_AUTH_TOKEN` is set in the environment, clients must send the header `X-Mari-Auth` with the same value.
- Without the header (or with a wrong token), inference responds `401 unauthorized`.

### Enabling auth
1. Set an environment variable for the inference container (compose env or deployment):
   ```env
   SENTINEL_AUTH_TOKEN=replace-with-strong-secret
   ```
2. Ensure clients include the header:
   ```http
   X-Mari-Auth: replace-with-strong-secret
   ```

## Network boundaries
- Use the `mari-prod` Docker network for cross-service resolution.
- Expose only necessary ports; inference defaults to `3002`.
- Consider Docker Compose profiles or separate networks for dev/test/prod.

## Secrets management
- Do not hardcode secrets in the repo.
- Use environment variables or a secrets manager (e.g., Docker secrets, Vault).
- For label-oracle credentials (if any), prefer per‑environment injection.

## TLS / mTLS (future)
- Front inference with a reverse proxy/ingress that terminates TLS.
- Add mTLS for service‑to‑service authentication inside the cluster for higher security.

## RBAC & least privilege
- Grant only required permissions to CI/CD runners and deployment agents.
- Restrict Redis and RabbitMQ access to internal networks.

## Supply‑chain
- Pin image versions where possible.
- Use a registry with vulnerability scanning.

## Logging & monitoring
- Prometheus and Grafana are included. Add alerting for error rates and model ID changes.
- Consider central log collection (e.g., Loki) for audit and incident response.
