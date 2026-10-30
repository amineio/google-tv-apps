# AI Chat sign-in diagnostic candidate

The user authorized the AI Chat name and continued Mac-assisted subscription sign-in.
This candidate keeps the current interface and direct loopback authorization flow.
It changes the displayed name, the banner title, and the name hint for new registrations.
The package remains `com.amine.asktv`. Version is `0.2`, code `2`.
Storage, issued registration identities, endpoints, session bounds, the vector icon, and the text-free hero remain unchanged.

Diagnostics identified a `SocketTimeoutException` while reading a browser connection.
The handler previously sent an unsolicited HTTP 400 response after an idle or incomplete connection timed out.
The repair closes only that socket, sends no response, and preserves pending authorization.
Deterministic tests cover timeouts before the request and during incomplete headers, followed by one accepted valid callback.

Diagnostics record only a fixed parser stage and exception class.
Allowed stages are `READ_REQUEST`, `READ_HEADERS`, `PARSE_URI`, `DECODE_QUERY`, `VALIDATE_CALLBACK`, and `DISPATCH_GRANT`.
Never log exception messages, stack traces, requests, URLs, authorization values, identifiers, or credentials.
The generic browser error remains `This request could not be read.` for other unhandled request errors.
The five-second socket timeout and existing request bounds remain unchanged.
Any further behavior fix needs new evidence and a focused regression.

No QR implementation is included.
A phone QR code alone cannot complete the current localhost callback flow.
The coordinator owns builds, device input, browser authorization, microphone tests, audible review, and Git delivery.
