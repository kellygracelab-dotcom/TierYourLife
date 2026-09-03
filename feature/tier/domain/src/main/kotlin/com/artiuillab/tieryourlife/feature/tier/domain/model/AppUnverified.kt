package com.artiuillab.tieryourlife.feature.tier.domain.model

/**
 * Google Play would not vouch for this installation, so the server turned it
 * away. Distinct from being signed out, and worth its own type: nothing the
 * person does inside the app fixes it, so no screen should offer them a
 * retry, a sign-in, or a suggestion about their connection.
 */
class AppUnverified : Exception("Play could not confirm this installation")
