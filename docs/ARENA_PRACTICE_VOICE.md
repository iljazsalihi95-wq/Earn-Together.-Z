# Albanian practice narration

Existing bundled recordings remain first choice and are matched by question ID, exact prompt and option order. Recordings rejected by the owner are not activated.

Signed-in practice can now call the existing speech Worker for question, introduction and post-answer explanation. Device-only scores, progression, wallet and reward ledgers are unchanged. Guests and genuine network outages retain the existing installed-device-voice fallback.

The Worker authenticates the existing Apps Script session via arenaBootstrap, selects narration exclusively from the 232-question canonical practice bank and rejects modified prompts/options, unsupported phases, or an explanation without a selected/timeout choice. It never trusts a client-supplied narration string or answer key. Existing durable user/global cost limits and private audio caches remain in use. Before an explanation is requested the Android client must already have local feedback. Practice endpoints have no competitive or monetary effect.

Both Android and Worker now retain A:, B:, C:, D: labels rather than inserting the word Alternativa. Numeric and religious abbreviation normalization remain intact.

Deployment: publish this repository version to the EXISTING quiz-arena-backend Worker, retaining its current bindings, key, account backend and quota settings. The new health response includes practice_speech:true and speech_revision:10. Android checks this capability before sending a practice request; an older Worker cannot be silently presented as upgraded. Do not create another API key or Worker.

Local checks: Worker tests include authentication, canonical question/order mismatch, absence of arbitrary text synthesis, and separate post-answer feedback. No claim of production speech quality follows from mocked tests or a successful APK build; live audio still needs to be produced and listened to after deployment.
