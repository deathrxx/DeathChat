const crypto = require('crypto');

const ADJECTIVES = [
  'Phantom', 'Shadow', 'Neon', 'Void', 'Cyber', 'Ghost', 'Silent', 'Dark',
  'Venom', 'Spectral', 'Rogue', 'Echo', 'Cryptic', 'Astral', 'Iron', 'Null'
];

const NOUNS = [
  'Reaper', 'Spectre', 'Stalker', 'Wraith', 'Hunter', 'Ghost', 'Viper',
  'Walker', 'Shade', 'Raven', 'Strider', 'Oracle', 'Demon', 'Titan', 'Drifter'
];

const AVATARS = [
  'avatar_reaper',
  'avatar_phantom',
  'avatar_spectre',
  'avatar_demon',
  'avatar_stalker',
  'avatar_wraith',
  'avatar_skull',
  'avatar_glitch'
];

function generateRandomDisplayName() {
  const adj = ADJECTIVES[Math.floor(Math.random() * ADJECTIVES.length)];
  const noun = NOUNS[Math.floor(Math.random() * NOUNS.length)];
  const num = Math.floor(100 + Math.random() * 900);
  return `${adj}${noun}_${num}`;
}

function getRandomAvatar() {
  return AVATARS[Math.floor(Math.random() * AVATARS.length)];
}

function generateRoomCode() {
  // Generate a distinct 6-character uppercase alphanumeric code, e.g. "DC7X9K"
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // Avoid confusing chars like 0, O, 1, I
  let code = 'DC';
  for (let i = 0; i < 4; i++) {
    code += chars.charAt(crypto.randomInt(0, chars.length));
  }
  return code;
}

function validateDisplayName(name) {
  if (!name || typeof name !== 'string') return false;
  const trimmed = name.trim();
  if (trimmed.length < 2 || trimmed.length > 32) return false;
  // Allow letters, numbers, underscores, and dashes
  const regex = /^[a-zA-Z0-9_\-\s]+$/;
  return regex.test(trimmed);
}

module.exports = {
  ADJECTIVES,
  NOUNS,
  AVATARS,
  generateRandomDisplayName,
  getRandomAvatar,
  generateRoomCode,
  validateDisplayName
};
