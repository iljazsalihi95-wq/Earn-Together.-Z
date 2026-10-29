
/**
 * QUIZ ARENA PREMIUM
 * NATURAL AI VOICE SYSTEM
 *
 * Google Apps Script
 *
 * Modeli: gpt-4o-mini-tts
 * Gjuhët: SQ, DE, EN, TR
 *
 * API Key:
 * Project Settings > Script Properties
 * OPENAI_API_KEY = çelësi yt
 *
 * Mos e vendos API Key direkt në kod.
 */

// ========================================
// KONFIGURIMI
// ========================================

const ARENA_VOICE_CONFIG = {
  model: "gpt-4o-mini-tts",
  voice: "coral",
  format: "mp3",
  speed: 0.96,
  maxCharacters: 1200,
  folderName: "QuizArenaPremium_Audio"
};

// ========================================
// STILET E PREZANTUESIT
// ========================================

const ARENA_VOICE_STYLES = {

  sq: {
    language: "Albanian",
    question:
      "Speak in natural standard Albanian. " +
      "You are a professional television quiz host. " +
      "Use a warm, clear, expressive voice. " +
      "Read the question with curiosity and natural rhythm. " +
      "Use subtle pauses. Avoid robotic delivery.",

    options:
      "Speak natural standard Albanian. " +
      "Read each answer option clearly and distinctly. " +
      "Use a short natural pause between options. " +
      "Maintain an engaging television quiz atmosphere.",

    explanation:
      "Speak natural standard Albanian. " +
      "Explain the correct answer calmly and confidently. " +
      "Sound like a knowledgeable, friendly presenter.",

    intro:
      "Speak natural standard Albanian. " +
      "Sound like an energetic professional game-show host. " +
      "Be welcoming, exciting and expressive without shouting."
  },

  de: {
    language: "German",
    question:
      "Speak natural standard German. " +
      "Act as a professional television quiz host. " +
      "Use warm, expressive intonation and subtle pauses.",

    options:
      "Speak natural German. " +
      "Read answer options clearly with brief pauses.",

    explanation:
      "Speak natural German. " +
      "Explain the answer confidently and calmly.",

    intro:
      "Speak natural German. " +
      "Be an energetic, welcoming game-show presenter."
  },

  en: {
    language: "English",
    question:
      "Speak natural English as a professional quiz host. " +
      "Use engaging intonation, clear pronunciation " +
      "and realistic pauses.",

    options:
      "Read each option clearly and naturally, " +
      "with a brief pause between choices.",

    explanation:
      "Explain the correct answer naturally, " +
      "with a warm and confident tone.",

    intro:
      "Sound like an enthusiastic professional " +
      "television game-show presenter."
  },

  tr: {
    language: "Turkish",
    question:
      "Speak natural standard Turkish. " +
      "Use warm, expressive television-host intonation " +
      "and realistic pauses.",

    options:
      "Read each option clearly in natural Turkish. " +
      "Pause briefly between choices.",

    explanation:
      "Explain the correct answer calmly and confidently " +
      "in natural Turkish.",

    intro:
      "Speak natural Turkish as an energetic " +
      "and welcoming game-show host."
  }
};

// ========================================
// MERR API KEY
// ========================================

function getArenaApiKey_() {

  const key = PropertiesService
    .getScriptProperties()
    .getProperty("OPENAI_API_KEY");

  if (!key) {
    throw new Error(
      "OPENAI_API_KEY nuk është vendosur " +
      "te Script Properties."
    );
  }

  return key;
}

// ========================================
// FOLDER AUDIO NË GOOGLE DRIVE
// ========================================

function getArenaAudioFolder_() {

  const folders = DriveApp.getFoldersByName(
    ARENA_VOICE_CONFIG.folderName
  );

  if (folders.hasNext()) {
    return folders.next();
  }

  return DriveApp.createFolder(
    ARENA_VOICE_CONFIG.folderName
  );
}

// ========================================
// IDENTIFIKIMI I AUDIOS
// ========================================

function arenaAudioHash_(text) {

  const bytes = Utilities.computeDigest(
    Utilities.DigestAlgorithm.SHA_256,
    text,
    Utilities.Charset.UTF_8
  );

  return bytes.map(function(b) {
    return ("0" + (b & 255).toString(16)).slice(-2);
  }).join("");
}

// ========================================
// GJENERIMI I ZËRIT NATYRAL
// ========================================

function generateArenaVoice(
  text,
  locale,
  segment
) {

  locale = String(locale || "sq")
    .toLowerCase();

  segment = String(segment || "question")
    .toLowerCase();

  text = String(text || "").trim();

  if (!text) {
    throw new Error("Teksti është bosh.");
  }

  if (
    text.length >
    ARENA_VOICE_CONFIG.maxCharacters
  ) {
    throw new Error(
      "Teksti është shumë i gjatë."
    );
  }

  const language =
    ARENA_VOICE_STYLES[locale];

  if (!language) {
    throw new Error(
      "Gjuhë e papërkrahur: " + locale
    );
  }

  const instructions =
    language[segment] ||
    language.question;

  const folder = getArenaAudioFolder_();

  // Identifikues unik për audio-cache
  const signature = [
    ARENA_VOICE_CONFIG.model,
    ARENA_VOICE_CONFIG.voice,
    ARENA_VOICE_CONFIG.speed,
    locale,
    segment,
    instructions,
    text
  ].join("|");

  const hash = arenaAudioHash_(signature);

  const filename =
    "arena_" + locale + "_" +
    segment + "_" +
    hash + ".mp3";

  // Kontrollo nëse audio ekziston
  const existing =
    folder.getFilesByName(filename);

  if (existing.hasNext()) {

    const file = existing.next();

    return {
      success: true,
      cached: true,
      fileId: file.getId(),
      fileName: filename
    };
  }

  // Kërkesa për OpenAI Audio API
  const response = UrlFetchApp.fetch(
    "https://api.openai.com/v1/audio/speech",
    {
      method: "post",
      contentType: "application/json",

      headers: {
        Authorization:
          "Bearer " + getArenaApiKey_()
      },

      payload: JSON.stringify({
        model: ARENA_VOICE_CONFIG.model,
        voice: ARENA_VOICE_CONFIG.voice,
        input: text,
        instructions: instructions,
        response_format:
          ARENA_VOICE_CONFIG.format,
        speed: ARENA_VOICE_CONFIG.speed
      }),

      muteHttpExceptions: true
    }
  );

  const status =
    response.getResponseCode();

  if (status !== 200) {

    // Mos regjistro API Key ose përgjigje
    // të plota që mund të kenë të dhëna.
    throw new Error(
      "OpenAI Audio API: HTTP " + status
    );
  }

  const audioBlob = response
    .getBlob()
    .setName(filename);

  const file =
    folder.createFile(audioBlob);

  return {
    success: true,
    cached: false,
    fileId: file.getId(),
    fileName: filename
  };
}

// ========================================
// TEST I ZËRIT SHQIP
// ========================================

function testQuizNaturalVoice() {

  const text =
    "Mirë se erdhët në Quiz Arena Premium! " +
    "Përgatituni për një sfidë të re. " +
    "Dëgjoni me kujdes pyetjen " +
    "dhe zgjidhni përgjigjen e saktë.";

  const result = generateArenaVoice(
    text,
    "sq",
    "intro"
  );

  Logger.log(
    JSON.stringify(result)
  );

  Logger.log(
    "Audio u ruajt në Google Drive."
  );

  return result;
}

// ========================================
// TEST I PYETJES
// ========================================

function testArenaQuestionVoice() {

  const text =
    "Cila është surja e parë " +
    "e Kuranit Famëlartë?";

  const result = generateArenaVoice(
    text,
    "sq",
    "question"
  );

  Logger.log(
    JSON.stringify(result)
  );

  return result;
}

// ========================================
// TEST I GJERMANISHTES
// ========================================

function testArenaGermanVoice() {

  const text =
    "Willkommen bei Quiz Arena Premium! " +
    "Hören Sie die Frage aufmerksam an " +
    "und wählen Sie die richtige Antwort.";

  return generateArenaVoice(
    text,
    "de",
    "intro"
  );
}
