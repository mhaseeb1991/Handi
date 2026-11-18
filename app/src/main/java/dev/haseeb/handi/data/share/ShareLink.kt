package dev.haseeb.handi.data.share

/**
 * Deep link format for a shared recipe: `handi://recipe/{shareId}`. A custom scheme, not a real
 * `https://` App Link -- this repo has no web domain to host a Digital Asset Links file on, so
 * the link only opens anything when Handi is already installed on the device that taps it. See
 * `docs/DATA_LAYER.md`.
 */
private const val SHARE_URI_SCHEME = "handi"
private const val SHARE_URI_HOST = "recipe"

internal fun shareLinkUri(shareId: String): String = "$SHARE_URI_SCHEME://$SHARE_URI_HOST/$shareId"
