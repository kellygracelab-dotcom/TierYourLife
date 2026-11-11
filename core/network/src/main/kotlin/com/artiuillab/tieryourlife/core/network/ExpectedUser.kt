package com.artiuillab.tieryourlife.core.network

import java.io.IOException

/** Local request actor, never serialized into a header or body. The server still verifies the bearer. */
data class ExpectedUser(val uid: String)

/** The account selected for an explicit action changed before its request could be sent. */
class ExpectedUserMismatch : IOException("Account changed before request")
