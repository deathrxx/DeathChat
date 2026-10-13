/**
 * Expiration calculation and query helper utilities.
 * Ensures consistent server-enforced expiration across all REST endpoints and Socket handlers.
 */

const DEFAULT_ROOM_LIFETIME_HOURS = 24;

function calculateRoomExpiration(hours = DEFAULT_ROOM_LIFETIME_HOURS) {
  const expiresAt = new Date();
  expiresAt.setHours(expiresAt.getHours() + hours);
  return expiresAt;
}

function calculateMessageExpiration(retentionSeconds = 3600, roomExpiresAt = null) {
  const expiresAt = new Date(Date.now() + retentionSeconds * 1000);
  // A message cannot live longer than the room itself
  if (roomExpiresAt && expiresAt > new Date(roomExpiresAt)) {
    return new Date(roomExpiresAt);
  }
  return expiresAt;
}

function isExpired(date) {
  if (!date) return true;
  return new Date() >= new Date(date);
}

/**
 * Filter object to be combined with Mongoose queries to exclude expired items
 */
function getUnexpiredQueryFilter(field = 'expiresAt') {
  return { [field]: { $gt: new Date() } };
}

module.exports = {
  DEFAULT_ROOM_LIFETIME_HOURS,
  calculateRoomExpiration,
  calculateMessageExpiration,
  isExpired,
  getUnexpiredQueryFilter
};
