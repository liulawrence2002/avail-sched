# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in Goblin Scheduler, please report it responsibly.

**Email:** security@goblinscheduler.com

Please include:
- A description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

We will acknowledge receipt within 48 hours and aim to provide a fix within 7 days for critical issues.

## Scope

The following are in scope:
- The Goblin Scheduler web application (frontend and backend)
- API endpoints under `/api/**`
- Authentication and authorization via host/participant tokens

## Known Limitations

- **No account system** — access is controlled via secure random URL tokens. Anyone with a link can access the associated resource.
- **No encryption at rest** — the database stores event data in plaintext. Deploy behind a managed database with encryption enabled.
- **No rate-limit persistence** — rate limiting is in-memory and resets on server restart.
- **No CSRF protection** — the API is stateless and does not use cookies for authentication.

## Supported Versions

| Version | Supported |
|---------|-----------|
| 0.x     | Yes       |
