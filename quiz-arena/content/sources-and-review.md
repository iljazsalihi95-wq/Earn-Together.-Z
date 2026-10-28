# Quiz Arena — editorial verification policy

All Islamic questions require review before public release. Never generate a claim or attribution without checking its primary source. A correct answer alone is insufficient.

Required fields per question:
- stable ID, locale, domain, category, difficulty, question type
- exact question and four distinct options
- correct option and educational explanation
- source title, author, passage reference (surah/ayah, hadith collection/number, book/chapter/page as appropriate)
- source URL when a reliable edition exists, edition details when pagination differs
- review status: draft / needs_source / verified / rejected
- reviewer, review date, and source version

Islamic categories:
1. Quran — check Arabic text and surah/ayah, then translation and recitation.
2. Hadith — check original text, collection, hadith identifier and authenticity classification. Different numbering systems must be identified.
3. Fiqh — specify Hanafi ruling and reference; mark genuine madhhab disagreements instead of hiding them.
4. Tafsir — attribute interpretations to a specific commentator and passage, distinguishing commentary from Quran text.
5. Islamic history — distinguish reliable historical evidence from disputed narratives.
6. General Islamic knowledge — check reference works and primary sources where possible.

Do not label the 12 existing seed questions verified: their sources are placeholders and need editorial review. Do not ship them as authenticated Islamic educational content without review.

Audio: Quranic recitation must come from authenticated recordings; Android TTS is not a substitute. Other spoken questions need localized, checked pronunciation.

The current native game uses a small hardcoded question bank and is not yet connected to the JSON seed. Its UI language picker is not a translation engine. This file does not claim gameplay integration or successful build.
