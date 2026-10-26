readme made by deepseek to not waste tryserves time

# Why I did this

I'm **luvtryserve**. I reverse-engineered and rebuilt the Clarity loader and payload because I was tired of the owner's ego and because the authentication and server-side logic are held together with tape and confidence.

This is not a customer-data dump. It is the loader/payload source, the real delivery protocol, and a write-up of what is actually enforcing security here. The short answer is: not much.

Everything except the text of this README was done by me. The download, protocol analysis, decompilation, source fixes, rebuilds, and testing are all mine. DeepSeek only helped write this README so I did not waste time on it.

## The short version

The loader looks impressive from the outside. Ed25519, X25519, HKDF, AES-GCM, HMAC. It reads like someone copied a modern secure-channel design into a Minecraft cheat loader.

Then you look at what those primitives actually protect.

The signed manifest is verified and then ignored for the thing that matters. The stage body is accepted based on a hash from `stage_begin`, not the signed manifest. In the download I captured, the signed manifest said one size and SHA-256 and the delivered stage had a different size and SHA-256. The loader accepted it anyway. That is not a theoretical weakness. That is the normal path working as written.

The licence flow is a licence key, a MAC-address hash, and a UUID/build/PID supplied by the client. The key is stored in plaintext Java Preferences. The HWID is a SHA-256 of sorted MAC strings. The client tells the server who it is, what build it is running, and what process it is. The server mostly believes it.

The "protection" is client-side. The payload is downloaded, decrypted in memory, defined in the game process, and injected with Instrumentation. Once the code is on the client, the client owns it. You can call that auth, but it is really an honesty system with extra cryptography.

## The owner problem

The technical issues are fixable. The overconfidence is not.

The owner's big ego is specifically about the client being uncrackable. He told someone who wanted to crack it, "give him the loader.jar" — as if handing over the loader proves something. It does the opposite. The loader is the entry point: endpoint, handshake, pinned keys, wire format, stage download logic, and injection path are all in there. Giving someone the loader and calling it uncrackable is not a flex. It is handing them the map and daring them to read it.

The crypto is real. The trust model around it is not. That is the difference between "nobody can break this" and "I never checked what happens after the handshake."

If the auth and server-side logic were actually solid, there would be nothing interesting to publish. They are not. The overconfidence just made the decision easy.

## What is actually wrong

### 1. The signed manifest is security theatre for the stage body

The loader verifies the release Ed25519 signature on the manifest. Good.

Then `downloadStage` verifies the stage against the `sha256` in `stage_begin`. `stage_begin` is encrypted under the session, but it is not signed by the release key. `sendAck` uses the manifest hash, not the actual stage hash.

So the signature authenticates a manifest that is not enforced against the bytes that get loaded. A compromised or malicious server can serve whatever stage it wants while still presenting a valid signed manifest. The pinned key proves the server is the server. It does not prove the payload matches the manifest.

### 2. The auth model trusts the client

The request contains:

- licence key
- HWID
- UUID
- build string
- PID

The server has no strong way to know that the UUID, build, or PID are real. The HWID is a MAC hash, which changes with adapters, VPNs, docking stations, and virtual machines. The licence key is stored locally without encryption. The whole thing is a speed bump, not a trust boundary.

### 3. The loader stores the licence key in plaintext

The GUI writes the key into Java Preferences under the user registry hive. No DPAPI, no credential store, no encryption. Any process running as the same user can read it.

For a paid product, that is embarrassing. For a product whose owner acts like the auth is untouchable, it is funny.

### 4. The payload stores Microsoft refresh tokens in plaintext

The alt manager writes `refreshToken`, `clientId`, `mcToken`, and expiry into `clarity/alts.json` as plain JSON. On Windows the POSIX permission call does nothing. Any local process, backup, or sync client can read those tokens and take the account.

That is a real user-security issue. It is not a cheat-module problem. It is a credential-storage problem.

### 5. The social service is on by default

The presence client is enabled by default. It authenticates with a Mojang session join challenge and sends the account UUID, name, status, current server IP, friend UUIDs, and social/chat data to the Clarity server.

The Minecraft access token stays local, which is good. But the server still gets the social graph and server history. The traffic is TLS, but the application layer is not pinned like the loader is.

### 6. The local web GUI has a CORS and config-write problem

The web GUI binds to `127.0.0.1:7420` and sends `Access-Control-Allow-Origin: *`. The `/friends` POST checks `Origin`. The `/config` POST does not. If the user enables the web GUI, a malicious page in a local browser can read config/server data and submit config changes.

It is local-only, but "local-only" is not a security model when the browser is the attacker.

### 7. The watermark is not a security boundary

`WatermarkSlot` is a 32-character string in the payload. The loader ships a placeholder, the server replaces it for delivery, and people treat it like it means something.

It is a per-delivery tag. It is not an auth check. The client never reads it. It does not stop anything. It just makes it easy to see whose build leaked. I replaced mine with `sourced by luvtryserve` because that is the only watermark that matters in this repo.

### 8. The stage zip is malformed for normal tools

The delivered stage has local-header offsets that are shifted relative to the central directory. `JarFile` and `ZipFile` fail. `ZipInputStream` works because the loader only reads sequentially.

That is not clever. It is fragile. It also means normal jar signing and verification tooling does not work on the thing that is supposed to be signed.

## Why public

Because "it's uncrackable", its not...

If the owner wants to prove me wrong, the code is here. Fix the manifest enforcement. Stop storing tokens in plaintext. Stop pretending a MAC hash and a client-supplied build string are authentication. Stop treating the watermark like a security feature. Start reading the code as carefully as the people who reverse-engineered it.

Until then, this is the source.

- **luvtryserve** (everything except this readme)
